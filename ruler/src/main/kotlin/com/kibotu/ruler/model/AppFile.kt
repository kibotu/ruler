package com.kibotu.ruler.model

import kotlinx.serialization.Serializable

/** Single file contained in an app. */
@Serializable
data class AppFile(
    val name: String,
    val type: FileType,
    override val downloadSize: Long,
    override val installSize: Long,
    val owner: String? = null,
    val resourceType: ResourceType? = null,
) : Measurable
