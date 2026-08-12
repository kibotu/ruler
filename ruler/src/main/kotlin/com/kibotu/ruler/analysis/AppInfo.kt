package com.kibotu.ruler.analysis

import java.io.Serializable

/** General info about the app under analysis. Serializable, because it is a Gradle task input. */
data class AppInfo(
    val variantName: String,
    val applicationId: String,
    val versionName: String,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
