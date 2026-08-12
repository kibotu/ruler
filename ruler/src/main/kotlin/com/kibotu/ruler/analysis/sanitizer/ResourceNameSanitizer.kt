package com.kibotu.ruler.analysis.sanitizer

import java.io.File

/** De-obfuscates resource file names, which DexGuard obfuscates. */
class ResourceNameSanitizer private constructor(private val nameMapping: Map<String, String>) {

    /** Names stay obfuscated when [mappingFile] is null. */
    constructor(mappingFile: File? = null) : this(
        mappingFile?.readText()?.let(::parse) ?: emptyMap(),
    )

    /** Sanitizes [resourceName] and de-obfuscates it, if a mapping is available. */
    fun sanitize(resourceName: String): String {
        return nameMapping[resourceName] ?: resourceName // /res/raw/dVo.xml -> /res/drawable/foo.xml
    }

    companion object {
        /** Builds a sanitizer from the contents of a mapping file. */
        fun fromMapping(mapping: String) = ResourceNameSanitizer(parse(mapping))

        /** Reads lines of the form `res/anim/foo.xml -> [res/raw/a.xml]` and ignores the rest. */
        private fun parse(mapping: String): Map<String, String> {
            return mapping.lineSequence()
                .filter { it.trim().startsWith("res/") }
                .mapNotNull { line ->
                    val parts = line.split(" -> ")
                    if (parts.size != 2) return@mapNotNull null
                    val obfuscated = parts[1].removeSurrounding("[", "]")
                    "/$obfuscated" to "/${parts[0]}"
                }
                .toMap()
        }
    }
}
