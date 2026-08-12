package com.kibotu.ruler.analysis.verification

import java.io.Serializable

/** Size limits for the app. Serializable, because it is a Gradle task input. */
data class VerificationConfig(
    val downloadSizeThreshold: Long = Long.MAX_VALUE,
    val installSizeThreshold: Long = Long.MAX_VALUE,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
