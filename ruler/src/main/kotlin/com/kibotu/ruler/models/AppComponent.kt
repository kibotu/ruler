package com.kibotu.ruler.models

import kotlinx.serialization.SerialName
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
    @SerialName("internal")
    val internal: Boolean? = null,
) : FileContainer
