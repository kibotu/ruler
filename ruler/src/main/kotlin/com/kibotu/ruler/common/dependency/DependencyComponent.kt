package com.kibotu.ruler.common.dependency

import com.kibotu.ruler.models.ComponentType

/** Component representing a single dependency. */
data class DependencyComponent(
    val name: String,
    val type: ComponentType,
)
