package com.spotify.ruler.common.models

import java.io.Serializable
import kotlinx.serialization.Serializable as KSerializable

/** Specification of a device for which APKs can be generated. */
@KSerializable
data class DeviceSpec(
    val abi: String,
    val locale: String,
    val screenDensity: Int,
    val sdkVersion: Int,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
