package com.kibotu.ruler.analysis.dependency

import com.kibotu.ruler.model.ComponentType

/** Component representing a single dependency. */
data class DependencyComponent(
    val name: String,
    val type: ComponentType,
)
