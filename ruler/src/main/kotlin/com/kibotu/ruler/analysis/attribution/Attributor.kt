package com.kibotu.ruler.analysis.attribution

import com.kibotu.ruler.analysis.dependency.DependencyComponent
import com.kibotu.ruler.model.AppFile
import com.kibotu.ruler.model.FileType

private typealias Dependencies = Map<String, List<DependencyComponent>>

/**
 * A manual attribution rule. A file belongs to [component] when its name contains [path].
 *
 * @param path A literal path fragment, escaped into a regex by the caller.
 */
data class StaticAttribution(
    val path: Regex,
    val component: DependencyComponent,
)

/**
 * Attributes files to the component that they come from.
 *
 * @param defaultComponent Component for files that match nothing else.
 * @param staticAttributions Manual rules. The longest matching path wins, because a longer path
 * is the more specific one.
 */
class Attributor(
    private val defaultComponent: DependencyComponent,
    staticAttributions: List<StaticAttribution> = emptyList(),
) {

    private val staticAttributions = staticAttributions.sortedByDescending { it.path.pattern.length }

    private val resourceVersionRegex = "(/res/[a-z][^/])*-(.*?)(?=/)".toRegex()
    private val resourceMultipleVectorRegex = "\\\$(\\D+)__\\d+\\.xml\$".toRegex()

    /**
     * @param files Files contained in the APKs.
     * @param dependencies File names mapped to every component that contains that file.
     * @return Components mapped to the files attributed to them.
     */
    fun attribute(files: List<AppFile>, dependencies: Dependencies): Map<DependencyComponent, List<AppFile>> {
        val components = mutableMapOf<DependencyComponent, MutableList<AppFile>>()
        files.forEach { file ->
            val component = when (file.type) {
                FileType.CLASS -> componentForClass(file.name, dependencies)
                FileType.RESOURCE -> componentForResource(file.name, dependencies)
                FileType.ASSET -> dependencies[file.name.removePrefix("/assets")]?.singleOrNull()
                FileType.NATIVE_LIB -> componentForNativeLib(file.name, dependencies)
                FileType.NATIVE_FILE -> staticComponentFor(file.name)
                FileType.OTHER -> dependencies[file.name]?.singleOrNull()
            } ?: staticComponentFor(file.name) ?: defaultComponent

            components.getOrPut(component) { mutableListOf() }.add(file)
        }
        return components
    }

    private fun componentForClass(name: String, dependencies: Dependencies): DependencyComponent? {
        dependencies[name]?.singleOrNull()?.let { return it }

        // Attribute Dagger factories like the type they produce.
        dependencies[name.removeSuffix("_Factory")]?.singleOrNull()?.let { return it }

        // Attribute Dagger modules like their abstract class or interface.
        dependencies[name.substringBefore("_Provide")]?.singleOrNull()?.let { return it }

        // Attribute lambdas by their package.
        if (name.contains(".-\$\$Lambda\$")) {
            componentForPackage(name.substringBefore(".-\$\$Lambda\$"), dependencies)?.let { return it }
        }

        // Attribute external synthetic classes by their simple class name.
        if (name.contains("\$\$ExternalSynthetic")) {
            val simpleName = name.substringBefore("\$\$ExternalSynthetic").substringAfterLast('.')
            dependencies.entries
                .filter { it.key.substringAfterLast('.') == simpleName }
                .flatMap { it.value }
                .distinct()
                .singleOrNull()
                ?.let { return it }
        }

        return componentForPackage(name.substringBeforeLast('.'), dependencies)
    }

    /**
     * Resource names carry qualifiers that the dependency graph does not have.
     *
     * `/res/layout-v21/name.xml` is stripped to `/layout/name.xml`. Some vector drawables are split
     * into `/res/drawable-anydpi-v24/${'$'}name__1.xml`, which is folded back to `/drawable-anydpi-v24/name.xml`.
     */
    private fun componentForResource(name: String, dependencies: Dependencies): DependencyComponent? {
        dependencies[name.removePrefix("/res")]?.singleOrNull()?.let { return it }

        if (name.contains(resourceVersionRegex)) {
            val stripped = name.replace(resourceVersionRegex, "").removePrefix("/res")
            dependencies[stripped]?.singleOrNull()?.let { return it }
        }

        if (name.contains(resourceMultipleVectorRegex)) {
            val folded = resourceMultipleVectorRegex.replace(name, "\$1.xml").removePrefix("/res")
            dependencies[folded]?.singleOrNull()?.let { return it }
        }

        return null
    }

    private fun componentForNativeLib(name: String, dependencies: Dependencies): DependencyComponent? {
        val libName = name.removePrefix("/lib")
        dependencies[libName]?.singleOrNull()?.let { return it }

        // Attribute LZMA-compressed libraries to their original source.
        return dependencies[libName.replace(".lzma.", ".")]?.singleOrNull()
    }

    private fun componentForPackage(name: String, dependencies: Dependencies): DependencyComponent? {
        return dependencies.entries
            .filter { it.key.substringBeforeLast('.') == name }
            .flatMap { it.value }
            .distinct()
            .singleOrNull()
    }

    private fun staticComponentFor(name: String): DependencyComponent? {
        return staticAttributions.firstOrNull { it.path.containsMatchIn(name) }?.component
    }
}
