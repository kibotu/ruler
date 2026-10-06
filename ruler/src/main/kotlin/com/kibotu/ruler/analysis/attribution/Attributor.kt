package com.kibotu.ruler.analysis.attribution

import com.kibotu.ruler.analysis.dependency.DependencyComponent
import com.kibotu.ruler.model.AppFile
import com.kibotu.ruler.model.FileType

private typealias Dependencies = Map<String, List<DependencyComponent>>

/** A manual attribution rule. [path] is a literal from the config file, escaped by the caller. */
data class StaticAttribution(
    val path: Regex,
    val component: DependencyComponent,
)

/** Attributes files to the component they come from. The longest matching path wins. */
class Attributor(
    private val defaultComponent: DependencyComponent,
    staticAttributions: List<StaticAttribution> = emptyList(),
) {

    private val staticAttributions = staticAttributions.sortedByDescending { it.path.pattern.length }

    private val resourceVersionRegex = "(/res/[a-z][^/])*-(.*?)(?=/)".toRegex()
    private val resourceMultipleVectorRegex = "\\\$(\\D+)__\\d+\\.xml\$".toRegex()

    fun attribute(
        files: List<AppFile>,
        dependencies: Dependencies,
    ): Map<DependencyComponent, List<AppFile>> {
        val index = DependencyIndex(dependencies)
        val components = mutableMapOf<DependencyComponent, MutableList<AppFile>>()
        files.forEach { file ->
            val component = when (file.type) {
                FileType.CLASS -> componentForClass(file.name, index)
                FileType.RESOURCE -> componentForResource(file.name, index)
                FileType.ASSET -> index.declaring(file.name.removePrefix("/assets"))
                FileType.NATIVE_LIB -> componentForNativeLib(file.name, index)
                FileType.NATIVE_FILE -> staticComponentFor(file.name)
                FileType.OTHER -> index.declaring(file.name)
            } ?: staticComponentFor(file.name) ?: defaultComponent

            components.getOrPut(component) { mutableListOf() }.add(file)
        }
        return components
    }

    private fun componentForClass(name: String, index: DependencyIndex): DependencyComponent? {
        index.declaring(name)?.let { return it }

        // Attribute Dagger factories like the type they produce.
        index.declaring(name.removeSuffix("_Factory"))?.let { return it }

        // Attribute Dagger modules like their abstract class or interface.
        index.declaring(name.substringBefore("_Provide"))?.let { return it }

        // Attribute lambdas by their package.
        if (name.contains(".-\$\$Lambda\$")) {
            index.owningPackage(name.substringBefore(".-\$\$Lambda\$"))?.let { return it }
        }

        // Attribute external synthetic classes by their simple class name.
        if (name.contains("\$\$ExternalSynthetic")) {
            val simpleName = name.substringBefore("\$\$ExternalSynthetic").substringAfterLast('.')
            index.declaringSimpleName(simpleName)?.let { return it }
        }

        return index.owningPackage(name.substringBeforeLast('.'))
    }

    /** Dependency names carry no resource qualifiers, so strip them: `/res/layout-v21/name.xml` to
     *  `/res/layout/name.xml`, `/res/drawable-anydpi-v24/${'$'}foo__1.xml` back to `/foo.xml`. */
    private fun componentForResource(name: String, index: DependencyIndex): DependencyComponent? {
        index.declaring(name.removePrefix("/res"))?.let { return it }

        val stripped = name.replace(resourceVersionRegex, "")
        if (stripped != name) {
            index.declaring(stripped.removePrefix("/res"))?.let { return it }
        }

        val folded = resourceMultipleVectorRegex.replace(name, "\$1.xml")
        if (folded != name) {
            index.declaring(folded.removePrefix("/res"))?.let { return it }
        }

        return null
    }

    private fun componentForNativeLib(name: String, index: DependencyIndex): DependencyComponent? {
        val libName = name.removePrefix("/lib")
        index.declaring(libName)?.let { return it }

        // Attribute LZMA-compressed libraries to their original source.
        return index.declaring(libName.replace(".lzma.", "."))
    }

    private fun staticComponentFor(name: String): DependencyComponent? {
        return staticAttributions.firstOrNull { it.path.containsMatchIn(name) }?.component
    }
}

/**
 * Lookup tables over the dependency graph. A key that more than one component claims is left out,
 * so an ambiguous match attributes nothing.
 */
private class DependencyIndex(private val byName: Dependencies) {

    private val byPackage by lazy { index { it.substringBeforeLast('.') } }
    private val bySimpleName by lazy { index { it.substringAfterLast('.') } }

    /** Each returns null unless exactly one component claims the key. */
    fun declaring(name: String): DependencyComponent? = byName[name]?.singleOrNull()

    fun owningPackage(packageName: String): DependencyComponent? = byPackage[packageName]

    fun declaringSimpleName(simpleName: String): DependencyComponent? = bySimpleName[simpleName]

    private fun index(keyOf: (String) -> String): Map<String, DependencyComponent> {
        val candidates = mutableMapOf<String, MutableSet<DependencyComponent>>()
        byName.forEach { (name, components) ->
            candidates.getOrPut(keyOf(name), ::mutableSetOf) += components
        }
        return buildMap(candidates.size) {
            candidates.forEach { (key, components) -> components.singleOrNull()?.let { put(key, it) } }
        }
    }
}
