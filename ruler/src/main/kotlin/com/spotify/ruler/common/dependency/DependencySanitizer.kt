package com.spotify.ruler.common.dependency

import com.spotify.ruler.common.sanitizer.ClassNameSanitizer
import com.spotify.ruler.models.ComponentType

/**
 * Responsible for sanitizing dependency entries, so they can be attributed easier.
 *
 * @param classNameSanitizer Used for sanitizing class names
 */
class DependencySanitizer(private val classNameSanitizer: ClassNameSanitizer) {

    /**
     * Sanitizes a list of dependency entries, to ease further processing. Sanitizing means cleaning up entry names and
     * associating entries with their components.
     *
     * @param entries List of raw entries parsed from dependencies
     * @return Map of file names to a list of all components which include this file
     */
    fun sanitize(entries: List<DependencyEntry>): Map<String, List<DependencyComponent>> {
        val map = mutableMapOf<String, MutableList<DependencyComponent>>()
        entries.map(::sanitizeEntry).forEach { entry ->
            val components = map.getOrPut(entry.name) { ArrayList() }
            val type = getComponentType(entry)
            components += DependencyComponent(entry.component, type)
        }
        return map
    }

    /** Cleans the component name and potentially sanitizes the name for a given [entry]. */
    private fun sanitizeEntry(entry: DependencyEntry): DependencyEntry {
        val component = normalizeComponentName(entry.component)
        return when(entry) {
            is DependencyEntry.Class -> {
                val name = classNameSanitizer.sanitize(entry.name)
                DependencyEntry.Class(name, component)
            }
            is DependencyEntry.Default -> {
                val name = entry.name.replace('\\', '/') // Convert Windows-style paths to UNIX-style paths
                DependencyEntry.Default(name, component)
            }
        }
    }

    /**
     * Normalizes Gradle component identifiers to a stable project path.
     * Gradle reports project dependencies as `project ':sample:lib'`; we want `:sample:lib`.
     */
    private fun normalizeComponentName(raw: String): String {
        return raw
            .removePrefix("project ")
            .trim()
            .removeSurrounding("'")
    }

    /**
     * Determines the correct component type for a given [entry].
     * After normalization, Gradle subprojects look like ":foo" (internal),
     * while Maven dependencies look like "org.bar:bar:1.0.0" (external).
     */
    private fun getComponentType(entry: DependencyEntry): ComponentType = when {
        entry.component.startsWith(":") -> ComponentType.INTERNAL
        else -> ComponentType.EXTERNAL
    }
}
