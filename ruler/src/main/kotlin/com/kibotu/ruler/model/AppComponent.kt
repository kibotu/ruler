package com.kibotu.ruler.model

import kotlinx.serialization.Serializable

/** Single component of an app. Can either be a Gradle module or a dependency. */
@Serializable
data class AppComponent(
    override val name: String,
    val type: ComponentType,
    override val downloadSize: Long,
    override val installSize: Long,
    override val files: List<AppFile>?,
    override val owner: String? = null,
    val additionalOwners: List<String>? = null,
    /** Overrides [type] in the report, for a dependency that belongs to your own organisation. */
    val internal: Boolean? = null,
    /** The ProGuard/R8 rules this component bundles. Null for components that ship none. */
    val proguardRules: List<ProguardRuleFile>? = null,
    /** How the rules of other components reach this one. Null when none do. */
    val proguardReach: ProguardReach? = null,
) : FileContainer
