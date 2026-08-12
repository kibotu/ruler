package com.kibotu.ruler.common.verification

import com.kibotu.ruler.models.AppFile

class Verificator(private val config: VerificationConfig) {

    fun verify(components: List<AppFile>) {
        val downloadSize = components.sumOf(AppFile::downloadSize)
        val downloadSizeThreshold = config.downloadSizeThreshold
        if (downloadSize > downloadSizeThreshold) {
            throw SizeExceededException("Download", downloadSize, downloadSizeThreshold)
        }

        val installSize = components.sumOf(AppFile::installSize)
        val installSizeThreshold = config.installSizeThreshold
        if (installSize > installSizeThreshold) {
            throw SizeExceededException("Install", installSize, installSizeThreshold)
        }
    }
}
