package com.kibotu.ruler.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Dynamic feature contained in an app. */
@Serializable
data class DynamicFeature(
    override val name: String,
    override val downloadSize: Long,
    override val installSize: Long,
    override val files: List<AppFile>?,
    override val owner: String? = null,
    @SerialName("internal")
    val internal: Boolean? = null,
) : FileContainer
