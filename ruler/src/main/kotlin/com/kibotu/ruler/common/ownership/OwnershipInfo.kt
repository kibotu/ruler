package com.kibotu.ruler.common.ownership

import com.kibotu.ruler.models.ComponentType

/**
 * Encapsulates ownership information of different components.
 *
 * Uses Caliper-style matching: entries are checked in YAML order, first match wins.
 * Patterns support glob syntax: `*` matches any characters, `?` matches a single character.
 *
 * @param entries List of ownership entries parsed from the ownership file (order matters).
 * @param defaultOwner Owner to use when no entry matches. If blank/empty, unmatched items have null owner.
 */
class OwnershipInfo(
    private val entries: List<OwnershipEntry>,
    private val defaultOwner: String,
) {
    private data class CompiledEntry(
        val regex: Regex,
        val owner: String,
        val internal: Boolean?,
    )

    private val compiledEntries: List<CompiledEntry> = entries.map { entry ->
        CompiledEntry(
            regex = globToRegex(entry.identifier),
            owner = entry.owner,
            internal = entry.internal,
        )
    }

    fun getOwner(component: String, componentType: ComponentType): String? {
        val names = candidateNames(component, componentType)
        return findMatch(names)?.owner ?: defaultOwnerOrNull()
    }

    fun getOwner(feature: String): String? {
        return findMatch(listOf(feature))?.owner ?: defaultOwnerOrNull()
    }

    fun getOwner(file: String, component: String, componentType: ComponentType): String? {
        val fileMatch = findMatch(listOf(file))
        if (fileMatch != null) return fileMatch.owner
        return getOwner(component, componentType)
    }

    fun getOwner(file: String, feature: String): String? {
        val fileMatch = findMatch(listOf(file))
        if (fileMatch != null) return fileMatch.owner
        return getOwner(feature)
    }

    fun getInternal(component: String, componentType: ComponentType): Boolean? {
        val names = candidateNames(component, componentType)
        return findMatch(names)?.internal
    }

    fun getInternal(feature: String): Boolean? {
        return findMatch(listOf(feature))?.internal
    }

    private fun candidateNames(component: String, componentType: ComponentType): List<String> {
        return when (componentType) {
            ComponentType.INTERNAL -> listOf(component)
            ComponentType.EXTERNAL -> listOf(
                component,
                component.substringBeforeLast(':'),
            )
        }
    }

    private fun findMatch(candidates: List<String>): CompiledEntry? {
        for (entry in compiledEntries) {
            for (candidate in candidates) {
                if (entry.regex.matches(candidate)) {
                    return entry
                }
            }
        }
        return null
    }

    private fun defaultOwnerOrNull(): String? = defaultOwner.takeIf { it.isNotBlank() }

    companion object {
        fun globToRegex(pattern: String): Regex {
            val regexPattern = buildString {
                append("^")
                for (char in pattern) {
                    when (char) {
                        '*' -> append(".*")
                        '?' -> append(".")
                        '.', '(', ')', '[', ']', '{', '}', '+', '^', '$', '|', '\\' -> {
                            append("\\")
                            append(char)
                        }
                        else -> append(char)
                    }
                }
                append("$")
            }
            return Regex(regexPattern, RegexOption.IGNORE_CASE)
        }
    }
}
