package com.kibotu.ruler.analysis.dependency

import kotlinx.serialization.Serializable

@Serializable
data class StaticComponent(
    val path: String,
    val id: String,
)
