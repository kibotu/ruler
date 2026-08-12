package com.kibotu.ruler.analysis.sanitizer

import com.android.tools.proguard.ProguardMap
import java.io.File
import java.io.StringReader

/** De-obfuscates class names with an R8, ProGuard, or DexGuard mapping file. */
class ClassNameSanitizer private constructor(private val proguardMap: ProguardMap) {

    /** Names stay obfuscated when [mappingFile] is null. */
    constructor(mappingFile: File? = null) : this(
        ProguardMap().apply { mappingFile?.let(::readFromFile) },
    )

    /** Sanitizes [className] and de-obfuscates it, if a mapping is available. */
    fun sanitize(className: String): String {
        val sanitized = className
            .removeSurrounding("L", ";") // La/b/C; -> a/b/C
            .removeSuffix(".class") // a/b/C.class -> a/b/C
            .replace('/', '.') // a/b/C -> a.b.C
        return proguardMap.getClassName(sanitized) // a.b.C -> foo.bar.Baz
    }

    companion object {
        /** Builds a sanitizer from the contents of a mapping file. */
        fun fromMapping(mapping: String) = ClassNameSanitizer(
            ProguardMap().apply { readFromReader(StringReader(mapping)) },
        )
    }
}
