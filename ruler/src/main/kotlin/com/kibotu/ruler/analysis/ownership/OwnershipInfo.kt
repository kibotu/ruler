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

    private val matchers = entries.map { Matcher(it.identifier, it.owners, it.internal) }

    fun owners(component: String, componentType: ComponentType): List<String>? =
        match(candidateNames(component, componentType))?.owners ?: defaultOwners()

    fun owners(feature: String): List<String>? = match(listOf(feature))?.owners ?: defaultOwners()

    /** Owners named for the file itself. Null leaves the file to inherit from what contains it. */
    fun fileOwners(file: String): List<String>? = match(listOf(file))?.owners

    /** Declared internal/external override, or null to keep the structural [ComponentType]. */
    fun internalOverride(component: String, componentType: ComponentType): Boolean? =
        match(candidateNames(component, componentType))?.internal

    fun internalOverride(feature: String): Boolean? = match(listOf(feature))?.internal

    /** A Maven coordinate is matched with and without its version. */
    private fun candidateNames(component: String, componentType: ComponentType): List<String> =
        when (componentType) {
            ComponentType.INTERNAL -> listOf(component)
            ComponentType.EXTERNAL -> listOf(component, component.substringBeforeLast(':'))
        }

    private fun match(candidates: List<String>): Matcher? =
        matchers.firstOrNull { matcher -> candidates.any(matcher::matches) }

    private fun defaultOwners(): List<String>? =
        defaultOwner.takeIf(String::isNotBlank)?.let(::listOf)

    private class Matcher(
        private val pattern: String,
        val owners: List<String>,
        val internal: Boolean?,
    ) {
        /** Most patterns are plain names. Those compare directly, which every file pays for. */
        private val regex = if (pattern.any { it == '*' || it == '?' }) globToRegex(pattern) else null

        fun matches(name: String): Boolean =
            regex?.matches(name) ?: name.equals(pattern, ignoreCase = true)
    }

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
