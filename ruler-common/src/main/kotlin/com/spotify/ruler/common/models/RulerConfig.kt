package com.spotify.ruler.common.models

import com.spotify.ruler.common.apk.ApkEntry
import com.spotify.ruler.common.veritication.VerificationConfig
import java.io.File

data class RulerConfig(
    val projectPath: String,
    val apkFilesMap: Map<String, List<File>>,
    val reportDir: File,
    val ownershipFile: File?,
    val staticDependenciesFile: File?,
    val appInfo: AppInfo,
    val deviceSpec: DeviceSpec?,
    val defaultOwner: String,
    val omitFileBreakdown: Boolean,
    val additionalEntries: List<ApkEntry.Default>?,
    val ignoredFiles: List<String>,
    val verificationConfig: VerificationConfig
)
