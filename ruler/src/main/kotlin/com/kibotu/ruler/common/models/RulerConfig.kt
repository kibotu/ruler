package com.kibotu.ruler.common.models

import com.kibotu.ruler.common.verification.VerificationConfig
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
    val verificationConfig: VerificationConfig,
)
