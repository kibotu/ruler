package com.kibotu.ruler.analysis

import java.io.Serializable

/** The device to build APKs for. Serializable, because it is a Gradle task input. */
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
