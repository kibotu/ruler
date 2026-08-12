package com.kibotu.ruler.analysis

import com.kibotu.ruler.analysis.verification.VerificationConfig
import java.io.File

/** What to measure, and where to put the result. */
data class RulerConfig(
    /** Gradle path of the application module, used as the fallback component. */
    val projectPath: String,
    /** Each feature of the bundle, mapped to the APKs that make it up. */
    val apkFilesMap: Map<String, List<File>>,
    val reportDir: File,
    val ownershipFile: File?,
    val staticDependenciesFile: File?,
    val appInfo: AppInfo,
    val defaultOwner: String,
    val omitFileBreakdown: Boolean,
    val verificationConfig: VerificationConfig,
)
