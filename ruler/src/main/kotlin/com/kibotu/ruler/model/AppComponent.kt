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
) : FileContainer
