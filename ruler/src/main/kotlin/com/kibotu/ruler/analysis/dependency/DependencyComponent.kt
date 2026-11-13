package com.kibotu.ruler.analysis.dependency

import com.kibotu.ruler.model.ComponentType

/** Component representing a single dependency. */
data class DependencyComponent(
    val name: String,
    val type: ComponentType,
) {
    companion object {

        /** Gradle reports a project dependency as `project ':sample:lib'`, and a module dependency
         *  by its Maven coordinate. Only the former starts with a colon once unwrapped. */
        fun of(raw: String): DependencyComponent {
            val name = raw.removePrefix("project ").trim().removeSurrounding("'")
            val type = if (name.startsWith(":")) ComponentType.INTERNAL else ComponentType.EXTERNAL
            return DependencyComponent(name, type)
        }
    }
}
