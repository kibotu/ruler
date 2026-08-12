package com.kibotu.ruler.analysis.verification

import com.kibotu.ruler.model.AppFile

/** Fails the build when the app is larger than its configured thresholds. */
class Verifier(private val config: VerificationConfig) {

    fun verify(files: List<AppFile>) {
        check("Download", files.sumOf(AppFile::downloadSize), config.downloadSizeThreshold)
        check("Install", files.sumOf(AppFile::installSize), config.installSizeThreshold)
    }

    private fun check(label: String, size: Long, threshold: Long) {
        if (size > threshold) throw SizeExceededException(label, size, threshold)
    }
}
