package com.spotify.ruler.common.dependency

import com.spotify.ruler.models.ComponentType

/** Component representing a single dependency. */
data class DependencyComponent(
    val name: String,
    val type: ComponentType,
)
