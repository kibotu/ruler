package com.spotify.ruler.common.ownership

import org.yaml.snakeyaml.Yaml
import java.io.File

/** Responsible for parsing and extracting ownership entries from the ownership file. */
class OwnershipFileParser {

    fun parse(ownershipFile: File): List<OwnershipEntry> = try {
        val yaml = Yaml()
        val entries: List<Map<String, String>> = ownershipFile.inputStream().use(yaml::load)
        entries.map { entry ->
            OwnershipEntry(entry.getValue("identifier"), entry.getValue("owner"))
        }
    } catch (@Suppress("TooGenericExceptionCaught") exception: Exception) {
        throw IllegalStateException("Could not parse ownership file", exception)
    }
}
