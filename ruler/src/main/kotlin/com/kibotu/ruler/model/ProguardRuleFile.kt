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
)
