package com.kibotu.ruler.analysis.dependency

import com.kibotu.ruler.analysis.sanitizer.ClassNameSanitizer
import com.kibotu.ruler.model.ComponentType

/**
 * Cleans up dependency entries, so that their names line up with the entries of the APK.
 *
 * @param classNameSanitizer De-obfuscates class names.
 */
class DependencySanitizer(private val classNameSanitizer: ClassNameSanitizer) {

    /** @return File names mapped to every component that contains that file. */
    fun sanitize(entries: List<DependencyEntry>): Map<String, List<DependencyComponent>> {
        val components = mutableMapOf<String, MutableList<DependencyComponent>>()
        entries.forEach { entry ->
            components.getOrPut(nameOf(entry), ::mutableListOf) += componentOf(entry.component)
        }
        return components
    }

    private fun nameOf(entry: DependencyEntry): String = when (entry) {
        is DependencyEntry.Class -> classNameSanitizer.sanitize(entry.name)
        // A Windows path never matches an APK entry, whose separator is always a slash.
        is DependencyEntry.Default -> entry.name.replace('\\', '/')
    }

    /**
     * Gradle reports a project dependency as `project ':sample:lib'`, and a module dependency by
     * its Maven coordinate. Only the former starts with a colon once unwrapped, which is what
     * tells the two apart.
     */
    private fun componentOf(rawComponent: String): DependencyComponent {
        val name = rawComponent.removePrefix("project ").trim().removeSurrounding("'")
        val type = if (name.startsWith(":")) ComponentType.INTERNAL else ComponentType.EXTERNAL
        return DependencyComponent(name, type)
    }
}
