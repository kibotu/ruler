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
    /** The rules that reach beyond [namespace]. Null when all of them stay inside. */
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
)

@Serializable
enum class RuleScope {
    /** Names a package other than the one the component lives in. */
    OUTSIDE_NAMESPACE,

    /** Matches every class of the app (`*` or `**`), with no hierarchy or annotation to narrow it. */
    ALL_CLASSES,

    /** Starts with a wildcard but is narrowed by `extends`, `implements` or an annotation. */
    CROSS_CUTTING,
}

/** What other components' rules do to a component, measured on its classes in the app. */
@Serializable
data class ProguardReach(
    /** Number and install size of the component's classes in the app. */
    val classes: Int,
    val bytes: Long,
    /** Classes pinned by a keep rule of the component itself. */
    val ownKept: Pinned,
    /** Classes pinned by a keep rule of another component, and not by its own. */
    val othersKept: Pinned,
    /** The other components whose rules match its classes, most bytes first. */
    val affectedBy: List<RuleImpact>,
)

@Serializable
data class Pinned(val classes: Int, val bytes: Long)

@Serializable
data class RuleImpact(
    val source: String,
    val classes: Int,
    val bytes: Long,
    /** The directives that matched, most classes first. */
    val rules: List<String>,
)
