package com.kibotu.ruler.analysis.proguard

import java.io.File
import java.io.Serializable

/**
 * The archive of a dependency that may bundle ProGuard/R8 rules: an AAR or JAR of the runtime
 * classpath. Serializable, because it is a Gradle task input.
 */
data class RuleArchive(
    val component: String,
    val file: File,
) : Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }
}
