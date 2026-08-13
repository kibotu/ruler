package com.kibotu.ruler.analysis.ownership

import com.kibotu.ruler.model.ComponentType

/**
 * Resolves the owner of a component, a dynamic feature, or a single file.
 *
 * Entries are checked in the order of the ownership file, and the first match wins.
 *
 * @param entries Ownership entries, in file order.
 * @param defaultOwner Owner for names that match no entry. A blank value leaves them unowned.
 */
class OwnershipInfo(
    entries: List<OwnershipEntry>,
    private val defaultOwner: String,
) {

    private data class CompiledEntry(
        val pattern: Regex,
        val owners: List<String>,
        val internal: Boolean?,
    )

    private val compiledEntries = entries.map {
        CompiledEntry(globToRegex(it.identifier), it.owners, it.internal)
    }

    fun getOwners(component: String, componentType: ComponentType): List<String>? =
        match(candidateNames(component, componentType))?.owners ?: defaultOwnersOrNull()

    fun getOwners(feature: String): List<String>? =
        match(listOf(feature))?.owners ?: defaultOwnersOrNull()

    /** A match on the file name overrides the owners of its component. */
    fun getOwners(file: String, component: String, componentType: ComponentType): List<String>? =
        match(listOf(file))?.owners ?: getOwners(component, componentType)

    /** A match on the file name overrides the owners of its dynamic feature. */
    fun getOwners(file: String, feature: String): List<String>? =
        match(listOf(file))?.owners ?: getOwners(feature)

    fun getOwner(component: String, componentType: ComponentType): String? =
        getOwners(component, componentType)?.firstOrNull()

    fun getOwner(feature: String): String? = getOwners(feature)?.firstOrNull()

    fun getOwner(file: String, component: String, componentType: ComponentType): String? =
        getOwners(file, component, componentType)?.firstOrNull()

    fun getOwner(file: String, feature: String): String? =
        getOwners(file, feature)?.firstOrNull()

    fun getInternal(component: String, componentType: ComponentType): Boolean? =
        match(candidateNames(component, componentType))?.internal

    fun getInternal(feature: String): Boolean? = match(listOf(feature))?.internal

    /** A Maven coordinate is matched with and without its version. */
    private fun candidateNames(component: String, componentType: ComponentType): List<String> =
        when (componentType) {
            ComponentType.INTERNAL -> listOf(component)
            ComponentType.EXTERNAL -> listOf(component, component.substringBeforeLast(':'))
        }

    private fun match(candidates: List<String>): CompiledEntry? =
        compiledEntries.firstOrNull { entry -> candidates.any(entry.pattern::matches) }

    private fun defaultOwnersOrNull(): List<String>? =
        defaultOwner.takeIf(String::isNotBlank)?.let(::listOf)

    companion object {
        /** Translates `*` and `?` into a regex, and escapes everything else. */
        fun globToRegex(pattern: String): Regex {
            val regex = buildString {
                append('^')
                for (char in pattern) {
                    when (char) {
                        '*' -> append(".*")
                        '?' -> append('.')
                        '.', '(', ')', '[', ']', '{', '}', '+', '^', '$', '|', '\\' -> append('\\').append(char)
                        else -> append(char)
                    }
                }
                append('$')
            }
            return Regex(regex, RegexOption.IGNORE_CASE)
        }
    }
}
