package com.kibotu.ruler.analysis.proguard

import com.kibotu.ruler.model.AppFile
import com.kibotu.ruler.model.FileType
import com.kibotu.ruler.model.Pinned
import com.kibotu.ruler.model.ProguardReach
import com.kibotu.ruler.model.ProguardRuleFile
import com.kibotu.ruler.model.RuleFlag
import com.kibotu.ruler.model.RuleImpact
import com.kibotu.ruler.model.RuleMatch
import com.kibotu.ruler.model.RuleScope

/**
 * Works out which classes of a component the rules of other components reach, and names the
 * directives that do. R8 applies every consumer rule to the whole app, so a library can keep or
 * rewrite code it does not own, including the third-party dependencies it brings along.
 *
 * Matching is by class name only. Directives narrowed by `extends`, `implements` or an annotation,
 * and the ones that match every class, are left out: they cannot be attributed to a component by
 * name, and the rule files already flag them. Only keep rules that keep whole classes count bytes.
 * The bytes are what a rule matched in the final app, so they are the most a rule can cost: some
 * of those classes would stay anyway because other code uses them.
 */
class ProguardAnalyzer {

    /** What the rule files of the app do to each other. */
    data class Analysis(
        /** The given rules, with every directive that reaches another component, or nothing, flagged. */
        val rules: Map<String, List<ProguardRuleFile>>,
        /** The reach per component, for those at least one rule of another component reaches. */
        val reach: Map<String, ProguardReach>,
    )

    private class Pattern(val prefix: String, val regex: Regex)

    private class Directive(
        val source: String,
        val file: Int,
        val line: Int,
        val text: String,
        val pins: Boolean,
        val patterns: List<Pattern>,
        /** Whether matching no class proves the directive does nothing in this app. */
        val canBeDead: Boolean,
    ) {
        fun matches(name: String) = patterns.any { name.startsWith(it.prefix) && it.regex.matches(name) }
    }

    /** The classes one directive matched in one component. */
    private class Match(val directive: Directive, val target: String, val classes: List<AppFile>) {
        val kept: List<AppFile> get() = if (directive.pins) classes else emptyList()

        fun from(other: String) = directive.source != other
    }

    /**
     * @param files The classes each component contributes to the app.
     * @param rules The rule files each component bundles.
     * @param app The application module. Its rules count like anyone else's, but its classes are
     *  never a target: its generated resource classes are noise, and its rules are its own call.
     * @param features The classes of dynamic features. No component owns them, but a rule that
     *  matches them is not dead.
     */
    fun analyze(
        files: Map<String, List<AppFile>>,
        rules: Map<String, List<ProguardRuleFile>>,
        app: String? = null,
        features: Collection<AppFile> = emptyList(),
    ): Analysis {
        val directives = rules.flatMap { (source, files) -> directivesOf(source, files) }
        if (directives.isEmpty()) return Analysis(rules, emptyMap())

        val classesOf = files.mapValues { (_, files) -> files.filter { it.type == FileType.CLASS } }
        val matched = matchedOf(directives, classesOf)
        val hit = matched.mapTo(HashSet(), Match::directive)
        val featureClasses = features.filter { it.type == FileType.CLASS }
        val dead = directives.filter { directive ->
            directive.canBeDead && directive !in hit && featureClasses.none { directive.matches(it.name) }
        }
        val attributed = matched.filter { it.target != app }
        return Analysis(
            rules = flagged(rules, attributed.filter { it.directive.source != app }, dead),
            reach = reached(attributed, classesOf),
        )
    }

    private fun directivesOf(source: String, files: List<ProguardRuleFile>): List<Directive> =
        files.flatMapIndexed { index, file ->
            RuleParser.parse(file.content).filter { !it.constrained }.mapNotNull { rule ->
                // A pattern without a literal start spans the whole app, so it says nothing.
                val patterns = rule.patterns.mapNotNull { pattern ->
                    RuleParser.literalPrefix(pattern).takeIf { it.isNotEmpty() }
                        ?.let { Pattern(it, RuleParser.regexOf(pattern)) }
                }
                patterns.takeIf { it.isNotEmpty() }?.let {
                    Directive(
                        source = source,
                        file = index,
                        line = rule.line,
                        text = rule.directive.take(MAX_DIRECTIVE),
                        pins = rule.pins,
                        patterns = it,
                        canBeDead = rule.pins && !rule.conditional && it.size == rule.patterns.size,
                    )
                }
            }
        }

    /** Every class a directive matched, as the component that owns those classes. */
    private fun matchedOf(directives: List<Directive>, classesOf: Map<String, List<AppFile>>): List<Match> =
        directives.flatMap { directive ->
            classesOf.mapNotNull { (target, classes) ->
                val matched = classes.filter { directive.matches(it.name) }
                matched.takeIf { it.isNotEmpty() }?.let { Match(directive, target, it) }
            }
        }

    private fun reached(matched: List<Match>, classesOf: Map<String, List<AppFile>>): Map<String, ProguardReach> =
        matched.groupBy { it.target }.mapNotNull { (target, ofTarget) ->
            val (foreign, ofOwn) = ofTarget.partition { it.from(target) }
            val own = ofOwn.kept()
            if (own.isEmpty() && foreign.isEmpty()) return@mapNotNull null
            val classes = classesOf.getValue(target)
            target to ProguardReach(
                classes = classes.size,
                bytes = classes.sumOf(AppFile::installSize),
                ownKept = own.pinned(),
                othersKept = (foreign.kept() - own).pinned(),
                affectedBy = affectedBy(foreign, own),
            )
        }.toMap()

    private fun affectedBy(foreign: List<Match>, own: Set<AppFile>): List<RuleImpact> {
        val bySource = foreign.groupBy { it.directive.source }
        val keptBy = bySource.mapValues { (_, matches) -> matches.kept() }
        val keepers = keptBy.values.flatten().groupingBy { it }.eachCount()
        return bySource.map { (source, matches) ->
            val kept = keptBy.getValue(source)
            RuleImpact(
                source = source,
                classes = matches.flatMap(Match::classes).toSet().size,
                kept = kept.pinned(),
                onlyTheirs = kept.filter { it !in own && keepers[it] == 1 }.toSet().pinned(),
                rules = matches.groupBy { it.directive.text }
                    .map { (text, ofText) ->
                        RuleMatch(text, ofText.flatMap(Match::classes).toSet().size, ofText.kept().sumOf(AppFile::installSize))
                    }
                    .sortedWith(compareByDescending(RuleMatch::bytes).thenByDescending(RuleMatch::classes)),
            )
        }.sortedWith(compareByDescending<RuleImpact> { it.onlyTheirs.bytes }.thenByDescending { it.kept.bytes })
    }

    /**
     * Flags the directives that reach classes of a component other than the one that ships them,
     * and the keep rules that match nothing at all. Either replaces the flag the rule file
     * guessed at on that line: only the app shows who owns the classes a directive matched, so a
     * directive naming its own package is a cross dependency when another component owns them.
     */
    private fun flagged(
        rules: Map<String, List<ProguardRuleFile>>,
        matched: List<Match>,
        dead: List<Directive>,
    ): Map<String, List<ProguardRuleFile>> {
        val cross = matched.filter { it.from(it.target) }
            .sortedWith(compareByDescending<Match> { it.kept.sumOf(AppFile::installSize) }.thenByDescending { it.classes.size })
            .groupBy { it.directive.source }
        val deadBySource = dead.groupBy { it.source }
        return rules.mapValues { (source, files) ->
            files.mapIndexed { index, file ->
                val proven = cross[source].orEmpty().filter { it.directive.file == index }
                    .groupBy { it.directive }
                    .map { (directive, ofDirective) ->
                        RuleFlag(directive.line, directive.text, RuleScope.CROSS_DEPENDENCY, ofDirective.map(Match::target))
                    } +
                    deadBySource[source].orEmpty().filter { it.file == index }
                        .map { RuleFlag(it.line, it.text, RuleScope.NO_MATCH) }
                if (proven.isEmpty()) return@mapIndexed file
                val lines = proven.map(RuleFlag::line).toSet()
                file.copy(flags = (file.flags.orEmpty().filterNot { it.line in lines } + proven).sortedBy(RuleFlag::line))
            }
        }
    }

    private fun List<Match>.kept(): Set<AppFile> = flatMap(Match::kept).toSet()

    private fun Set<AppFile>.pinned() = Pinned(size, sumOf(AppFile::installSize))
}
