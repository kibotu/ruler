package com.spotify.ruler.common.sanitizer

import com.android.tools.proguard.ProguardMap
import java.io.File
import java.io.StringReader

/** Responsible for sanitizing class names. */
class ClassNameSanitizer {
    private val proguardMap = ProguardMap()

    constructor(mappingFile: File?) { mappingFile?.let(proguardMap::readFromFile) }
    constructor(mapping: String) { proguardMap.readFromReader(StringReader(mapping)) }

    /** Sanitizes a given [className], which includes deobfuscation (if applicable). */
    fun sanitize(className: String): String {
        val sanitized = className
            .removeSurrounding("L", ";") // La/b/C; -> a/b/C
            .removeSuffix(".class") // a/b/C.class -> a/b/C
            .replace('/', '.') // a/b/c -> a.b.C
        return proguardMap.getClassName(sanitized) // a.b.C -> foo/bar/Baz
    }
}
