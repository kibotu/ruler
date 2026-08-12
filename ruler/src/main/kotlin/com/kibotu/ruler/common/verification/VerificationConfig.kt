package com.kibotu.ruler.common.verification

import java.io.Serializable
import kotlinx.serialization.Serializable as KSerializable

@KSerializable
data class VerificationConfig(
    val downloadSizeThreshold: Long = Long.MAX_VALUE,
    val installSizeThreshold: Long = Long.MAX_VALUE,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
