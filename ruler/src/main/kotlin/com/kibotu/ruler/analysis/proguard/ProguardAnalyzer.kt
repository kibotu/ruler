package com.kibotu.ruler.analysis.proguard

import com.kibotu.ruler.model.AppFile
import com.kibotu.ruler.model.FileType
import com.kibotu.ruler.model.Pinned
import com.kibotu.ruler.model.ProguardReach
import com.kibotu.ruler.model.ProguardRuleFile
import com.kibotu.ruler.model.RuleFlag
import com.kibotu.ruler.model.RuleImpact
import com.kibotu.ruler.model.RuleScope

/**
 * Works out which classes of a component the rules of other components reach, and names the
 * directives that do. R8 applies every consumer rule to the whole app, so a library can keep or
 * rewrite code it does not own, including the third-party dependencies it brings along.
 *
 * Matching is by class name only. Directives narrowed by `extends`, `implements` or an annotation,
 * and the ones that match every class, are left out: they cannot be attributed to a component by
 * name, and the rule files already flag them. Only keep rules pin code, so a directive that only
 * rewrites it, like `-assumenosideeffects`, reaches a component without adding to its pinned bytes.
 */
class ProguardAnalyzer {

    /** What the rule files of the app do to each other. */
    data class Analysis(
        /** The given rules, with every directive that reaches another component flagged. */
        val rules: Map<String, List<ProguardRuleFile>>,
        /** The reach per component, for those at least one rule of another component reaches. */
        val reach: Map<String, ProguardReach>,
    )

    private class Directive(
        val source: String,
        val file: Int,
        val line: Int,
        val text: String,
        val keep: Boolean,
        val prefix: String,
        val regex: Regex,
    )

    /** The classes one directive matched in one component. */
    private class Match(val directive: Directive, val target: String, val classes: List<AppFile>) {
        val bytes: Long get() = classes.sumOf(AppFile::installSize)

        fun from(other: String) = directive.source != other
    }

    /**
     * @param files The classes each component contributes to the app.
     * @param rules The rule files each component bundles.
     * @param ignored The component that is left out, as source and target. The app's own rules
     *  and its generated resource classes are noise next to what libraries do to each other.
     */
    fun analyze(
        files: Map<String, List<AppFile>>,
        rules: Map<String, List<ProguardRuleFile>>,
        ignored: String? = null,
    ): Analysis {
        val directives = rules.filterKeys { it != ignored }.flatMap { (source, files) -> directivesOf(source, files) }
        if (directives.isEmpty()) return Analysis(rules, emptyMap())

        val classesOf = files.filterKeys { it != ignored }
            .mapValues { (_, files) -> files.filter { it.type == FileType.CLASS } }
        val matched = matchedOf(directives, classesOf)
        return Analysis(crossFlagged(rules, matched), reached(matched, classesOf))
    }

    private fun directivesOf(source: String, files: List<ProguardRuleFile>): List<Directive> =
        files.flatMapIndexed { index, file ->
            RuleParser.parse(file.content).filter { !it.constrained }.flatMap { rule ->
                rule.patterns.mapNotNull { pattern ->
                    val prefix = RuleParser.literalPrefix(pattern)
                    // A pattern without a literal start spans the whole app, so it says nothing.
                    prefix.takeIf { it.isNotEmpty() }
                        ?.let { Directive(source, index, rule.line, rule.directive, rule.keep, it, RuleParser.regexOf(pattern)) }
                }
            }
        }

    /** Every class a directive matched, as the component that owns those classes. */
    private fun matchedOf(directives: List<Directive>, classesOf: Map<String, List<AppFile>>): List<Match> =
        directives.flatMap { directive ->
            classesOf.mapNotNull { (target, classes) ->
                val matched = classes.filter { it.name.startsWith(directive.prefix) && directive.regex.matches(it.name) }
                matched.takeIf { it.isNotEmpty() }?.let { Match(directive, target, it) }
            }
        }

    private fun reached(matched: List<Match>, classesOf: Map<String, List<AppFile>>): Map<String, ProguardReach> =
        matched.groupBy { it.target }.mapNotNull { (target, ofTarget) ->
            val own = ofTarget.filter { it.directive.keep && !it.from(target) }.flatMap(Match::classes).toSet()
            val foreign = ofTarget.filter { it.from(target) }
            if (own.isEmpty() && foreign.isEmpty()) return@mapNotNull null
            val classes = classesOf.getValue(target)
            target to ProguardReach(
                classes = classes.size,
                bytes = classes.sumOf(AppFile::installSize),
                ownKept = own.pinned(),
                othersKept = (foreign.filter { it.directive.keep }.flatMap(Match::classes).toSet() - own).pinned(),
                affectedBy = affectedBy(foreign),
            )
        }.toMap()

    private fun affectedBy(foreign: List<Match>): List<RuleImpact> =
        foreign.groupBy { it.directive.source }.map { (source, bySource) ->
            val touched = bySource.flatMap(Match::classes).toSet()
            RuleImpact(
                source = source,
                classes = touched.size,
                bytes = touched.sumOf(AppFile::installSize),
                rules = bySource.groupBy { it.directive.text }
                    .mapValues { (_, matches) -> matches.flatMap(Match::classes).toSet() }
                    .entries.sortedByDescending { it.value.size }
                    .map { it.key }
                    .take(MAX_RULES),
            )
        }.sortedByDescending(RuleImpact::bytes)

    /** Flags the directives that reach classes of a component other than the one that ships them. */
    private fun crossFlagged(rules: Map<String, List<ProguardRuleFile>>, matched: List<Match>): Map<String, List<ProguardRuleFile>> {
        val bySource = matched.filter { it.from(it.target) }
            .sortedByDescending(Match::bytes)
            .groupBy { it.directive.source }
        return rules.mapValues { (source, files) ->
            val ofSource = bySource[source] ?: return@mapValues files
            files.mapIndexed { index, file ->
                val ofFile = ofSource.filter { it.directive.file == index }
                if (ofFile.isEmpty()) file else file.copy(flags = flagged(file, ofFile))
            }
        }
    }

    /**
     * The file's flags, with each directive that reached another component replaced by a cross
     * dependency naming what it reached. A directive can reach several components, so they are
     * listed by what each one costs, and a directive naming its own package is flagged here even
     * though the file's namespace has nothing to say about it: only the app shows who owns the
     * classes it matched.
     */
    private fun flagged(file: ProguardRuleFile, matches: List<Match>): List<RuleFlag> {
        val byLine = matches.groupBy { it.directive.line }
        val cross = byLine.map { (line, ofLine) ->
            RuleFlag(
                line = line,
                directive = ofLine.first().directive.text.take(MAX_DIRECTIVE),
                scope = RuleScope.CROSS_DEPENDENCY,
                targets = ofLine.map { it.target }.distinct(),
            )
        }
        return (file.flags.orEmpty().filterNot { it.line in byLine } + cross).sortedBy { it.line }
    }

    private fun Set<AppFile>.pinned() = Pinned(size, sumOf(AppFile::installSize))

    private companion object {
        const val MAX_RULES = 5
    }
}
