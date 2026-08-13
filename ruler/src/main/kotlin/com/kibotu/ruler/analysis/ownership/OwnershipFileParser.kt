package com.kibotu.ruler.analysis.ownership

import org.yaml.snakeyaml.Yaml
import java.io.File

/** Reads ownership entries from a YAML file. */
class OwnershipFileParser {

    fun parse(ownershipFile: File): List<OwnershipEntry> = try {
        val entries: List<Map<String, Any?>> = ownershipFile.inputStream().use(Yaml()::load)
        entries.map { entry ->
            OwnershipEntry(
                identifier = requireNotNull(entry["identifier"]) { "Missing 'identifier'" }.toString(),
                owners = parseOwners(entry),
                internal = entry["internal"]?.toString()?.lowercase()?.toBooleanStrictOrNull(),
            )
        }
    } catch (exception: Exception) {
        throw IllegalStateException("Could not parse ownership file ${ownershipFile.name}", exception)
    }

    private fun parseOwners(entry: Map<String, Any?>): List<String> {
        val owners = entry["owners"] ?: entry["owner"] ?: throw IllegalArgumentException("Missing 'owner' or 'owners'")
        return when (owners) {
            is List<*> -> owners.map { requireNotNull(it) { "Null owner in list" }.toString() }
            else -> listOf(owners.toString())
        }
    }
}
