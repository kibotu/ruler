package com.kibotu.ruler.analysis.proguard

import com.kibotu.ruler.model.AppFile
import com.kibotu.ruler.model.FileType
import com.kibotu.ruler.model.Pinned
import com.kibotu.ruler.model.ProguardReach
import com.kibotu.ruler.model.ProguardRuleFile
import com.kibotu.ruler.model.RuleImpact

/**
 * Works out which classes of a component the rules of other components reach. R8 applies every
 * consumer rule to the whole app, so a library can pin or rewrite code it does not own.
 *
 * Matching is by class name only. Directives narrowed by `extends`, `implements` or an
 * annotation, and the ones that match every class, are left out: they cannot be attributed to a
 * component by name, and the rule files already flag them.
 */
class ProguardReachAnalyzer {

    private class Directive(val source: String, val text: String, val keep: Boolean, val prefix: String, val regex: Regex)

    /**
     * @param files The classes each component contributes to the app.
     * @param rules The rule files each component bundles.
     * @param ignored The component that is left out, as source and target. The app's own rules
     *  and its generated resource classes are noise next to what libraries do to each other.
     * @return The reach per component, for those that are pinned or affected by at least one rule.
     */
    fun analyze(
        files: Map<String, List<AppFile>>,
        rules: Map<String, List<ProguardRuleFile>>,
        ignored: String? = null,
    ): Map<String, ProguardReach> {
        val directives = rules.filterKeys { it != ignored }.flatMap { (source, files) -> directivesOf(source, files) }
        if (directives.isEmpty()) return emptyMap()

        return files.filterKeys { it != ignored }.mapNotNull { (target, files) ->
            reachOf(target, files.filter { it.type == FileType.CLASS }, directives)?.let { target to it }
        }.toMap()
    }

    private fun directivesOf(source: String, files: List<ProguardRuleFile>): List<Directive> =
        files.flatMap { file ->
            RuleParser.parse(file.content).filter { !it.constrained }.flatMap { rule ->
                rule.patterns.mapNotNull { pattern ->
                    val prefix = RuleParser.literalPrefix(pattern)
                    // A pattern without a literal start spans the whole app, so it says nothing.
                    prefix.takeIf { it.isNotEmpty() }
                        ?.let { Directive(source, rule.directive, rule.keep, it, RuleParser.regexOf(pattern)) }
                }
            }
        }

    private fun reachOf(target: String, classes: List<AppFile>, directives: List<Directive>): ProguardReach? {
        val own = mutableSetOf<AppFile>()
        val others = mutableSetOf<AppFile>()
        val impacts = mutableMapOf<String, MutableMap<String, MutableSet<AppFile>>>()

        for (directive in directives) {
            val matched = classes.filter { it.name.startsWith(directive.prefix) && directive.regex.matches(it.name) }
            if (matched.isEmpty()) continue
            if (directive.source == target) {
                if (directive.keep) own += matched
            } else {
                if (directive.keep) others += matched
                impacts.getOrPut(directive.source, ::mutableMapOf).getOrPut(directive.text, ::mutableSetOf) += matched
            }
        }
        if (own.isEmpty() && impacts.isEmpty()) return null

        return ProguardReach(
            classes = classes.size,
            bytes = classes.sumOf(AppFile::installSize),
            ownKept = own.pinned(),
            othersKept = (others - own).pinned(),
            affectedBy = impacts.map { (source, byDirective) ->
                val touched = byDirective.values.flatten().toSet()
                RuleImpact(
                    source = source,
                    classes = touched.size,
                    bytes = touched.sumOf(AppFile::installSize),
                    rules = byDirective.entries.sortedByDescending { it.value.size }.map { it.key }.take(MAX_RULES),
                )
            }.sortedByDescending(RuleImpact::bytes),
        )
    }

    private fun Set<AppFile>.pinned() = Pinned(size, sumOf(AppFile::installSize))

    private companion object {
        const val MAX_RULES = 5
    }
}
