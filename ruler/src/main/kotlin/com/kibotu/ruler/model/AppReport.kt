package com.kibotu.ruler.model

import kotlinx.serialization.Serializable

/** Analysis report of an app. */
@Serializable
data class AppReport(
    val name: String,
    val version: String,
    val variant: String,
    override val downloadSize: Long,
    override val installSize: Long,
    val components: List<AppComponent>,
    val dynamicFeatures: List<DynamicFeature>,
) : Measurable
