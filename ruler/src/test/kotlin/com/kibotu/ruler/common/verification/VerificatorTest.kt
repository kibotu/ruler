package com.kibotu.ruler.common.verification

import com.google.common.truth.Truth.assertThat
import com.kibotu.ruler.models.AppFile
import com.kibotu.ruler.models.FileType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class VerificatorTest {
    @Test
    fun `passes when sizes are within thresholds`() {
        val verificator = Verificator(
            VerificationConfig(downloadSizeThreshold = 100, installSizeThreshold = 200),
        )
        val files = listOf(appFile(download = 50, install = 100))

        verificator.verify(files)
    }

    @Test
    fun `fails when download size exceeds threshold`() {
        val verificator = Verificator(
            VerificationConfig(downloadSizeThreshold = 10, installSizeThreshold = 200),
        )
        val files = listOf(appFile(download = 50, install = 100))

        val exception = assertThrows<SizeExceededException> { verificator.verify(files) }

        assertThat(exception.message).contains("Download")
    }

    @Test
    fun `fails when install size exceeds threshold`() {
        val verificator = Verificator(
            VerificationConfig(downloadSizeThreshold = 200, installSizeThreshold = 10),
        )
        val files = listOf(appFile(download = 5, install = 50))

        val exception = assertThrows<SizeExceededException> { verificator.verify(files) }

        assertThat(exception.message).contains("Install")
    }

    private fun appFile(download: Long, install: Long) =
        AppFile("com.example.Foo", FileType.CLASS, download, install)
}
