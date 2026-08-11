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
        val component = entry.component.removePrefix("project ")
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

    private val versionRegex = Regex("^\\d+\\.\\d+\\.\\d.*")

    /**
     * Determines the correct component type for a given [entry].
     * Assuming that all external dependencies do have a version number in the format XX.XX.XX
     * */
    private fun getComponentType(entry: DependencyEntry): ComponentType = when {
        versionRegex.containsMatchIn(entry.component.substringAfterLast(":","")) -> ComponentType.EXTERNAL
        else -> ComponentType.INTERNAL
    }
}
