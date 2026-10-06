package com.kibotu.ruler.analysis.sanitizer

import com.android.tools.proguard.ProguardMap
import java.io.File

/** De-obfuscates class names with an R8, ProGuard, or DexGuard mapping file. Null leaves them as
 *  they are. */
class ClassNameSanitizer private constructor(private val proguardMap: ProguardMap) {

    constructor(mappingFile: File? = null) : this(
        ProguardMap().apply { mappingFile?.let(::readFromFile) },
    )

    fun sanitize(className: String): String {
        val sanitized = className
            .removeSurrounding("L", ";") // La/b/C; -> a/b/C
            .removeSuffix(".class") // a/b/C.class -> a/b/C
            .replace('/', '.') // a/b/C -> a.b.C
        return proguardMap.getClassName(sanitized) // a.b.C -> foo.bar.Baz
    }
}
