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
        val owner: String,
        val internal: Boolean?,
    )

    private val compiledEntries = entries.map {
        CompiledEntry(globToRegex(it.identifier), it.owner, it.internal)
    }

    fun getOwner(component: String, componentType: ComponentType): String? =
        match(candidateNames(component, componentType))?.owner ?: defaultOwnerOrNull()

    fun getOwner(feature: String): String? =
        match(listOf(feature))?.owner ?: defaultOwnerOrNull()

    /** A match on the file name overrides the owner of its component. */
    fun getOwner(file: String, component: String, componentType: ComponentType): String? =
        match(listOf(file))?.owner ?: getOwner(component, componentType)

    /** A match on the file name overrides the owner of its dynamic feature. */
    fun getOwner(file: String, feature: String): String? =
        match(listOf(file))?.owner ?: getOwner(feature)

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

    private fun defaultOwnerOrNull(): String? = defaultOwner.takeIf(String::isNotBlank)

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
