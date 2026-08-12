package com.kibotu.ruler.common.models

import java.io.Serializable
import kotlinx.serialization.Serializable as KSerializable

/** General info about an app. */
@KSerializable
data class AppInfo(
    val variantName: String,
    val applicationId: String,
    val versionName: String,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
