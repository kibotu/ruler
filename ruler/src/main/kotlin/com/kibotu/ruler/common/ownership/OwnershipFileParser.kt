package com.kibotu.ruler.common.ownership

import org.yaml.snakeyaml.Yaml
import java.io.File

/** Responsible for parsing and extracting ownership entries from the ownership file. */
class OwnershipFileParser {

    fun parse(ownershipFile: File): List<OwnershipEntry> = try {
        val yaml = Yaml()
        val entries: List<Map<String, Any?>> = ownershipFile.inputStream().use(yaml::load)
        entries.map { entry ->
            OwnershipEntry(
                identifier = entry["identifier"]?.toString()
                    ?: throw IllegalArgumentException("Missing 'identifier' in ownership entry"),
                owner = entry["owner"]?.toString()
                    ?: throw IllegalArgumentException("Missing 'owner' in ownership entry"),
                internal = parseBoolean(entry["internal"]),
            )
        }
    } catch (@Suppress("TooGenericExceptionCaught") exception: Exception) {
        throw IllegalStateException("Could not parse ownership file", exception)
    }

    private fun parseBoolean(value: Any?): Boolean? = when (value) {
        null -> null
        is Boolean -> value
        is String -> value.lowercase() == "true"
        else -> null
    }
}
