package com.kibotu.ruler.model

import kotlinx.serialization.Serializable

/**
 * One bundled ProGuard/R8 rules file of a dependency component. The rules never end up in the
 * APK — they only steer R8 — so they are read from the dependency's archive, not from the app.
 */
@Serializable
data class ProguardRuleFile(
    /** Why the file is applied: "Consumer rules", or "Targeted R8 rules (r8-from-8.0.0)". */
    val origin: String,
    /** Path of the file inside the dependency archive. */
    val name: String,
    /** The rules themselves. */
    val content: String,
    /** Number of rule directives, i.e. the non-comment lines starting with `-`. */
    val ruleCount: Int,
    /** The package the component's own classes live in, which its rules are measured against. */
    val namespace: String? = null,
    /** The rules that reach beyond the component. Null when none of them do. */
    val flags: List<RuleFlag>? = null,
)

/** A rule that does not stay within the component that ships it. */
@Serializable
data class RuleFlag(
    /** 1-based line in [ProguardRuleFile.content] where the directive starts. */
    val line: Int,
    /** The directive up to its member block, on one line. */
    val directive: String,
    val scope: RuleScope,
    /** The components whose classes the directive reached, the costly one first. Only set on a
     *  [RuleScope.CROSS_DEPENDENCY], where it is known because those classes are in the app. */
    val targets: List<String>? = null,
)

@Serializable
enum class RuleScope {
    /** Reaches classes of another component of the app, so it acts on a dependency it does not own. */
    CROSS_DEPENDENCY,

    /** Names a package other than the one the component lives in. */
    OUTSIDE_NAMESPACE,

    /** Matches every class of the app (`*` or `**`), with no hierarchy or annotation to narrow it. */
    ALL_CLASSES,

    /** Starts with a wildcard but is narrowed by `extends`, `implements` or an annotation. */
    CROSS_CUTTING,

    /** Keeps classes by name, and the app contains none of them. */
    NO_MATCH,
}

/** What other components' rules do to a component, measured on its classes in the app. */
@Serializable
data class ProguardReach(
    /** Number and install size of the component's classes in the app. */
    val classes: Int,
    val bytes: Long,
    /** Classes kept by a keep rule of the component itself. */
    val ownKept: Pinned,
    /** Classes kept by a keep rule of another component, and not by its own. */
    val othersKept: Pinned,
    /** The other components whose rules match its classes, the most bytes only they keep first. */
    val affectedBy: List<RuleImpact>,
)

@Serializable
data class Pinned(val classes: Int, val bytes: Long)

/** What the rules of one other component do to a component. */
@Serializable
data class RuleImpact(
    val source: String,
    /** Number of classes its rules match, kept or only rewritten. */
    val classes: Int,
    /** The classes its keep rules keep. */
    val kept: Pinned,
    /** The classes no other rule keeps: the most that dropping its rules can save. */
    val onlyTheirs: Pinned,
    /** Its directives that matched, most bytes kept first. */
    val rules: List<RuleMatch>,
)

@Serializable
data class RuleMatch(
    val directive: String,
    val classes: Int,
    /** Install size of the classes it keeps. Zero for a directive that keeps none. */
    val bytes: Long,
)
