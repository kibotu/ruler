package com.kibotu.ruler.analysis.verification

import com.google.common.truth.Truth.assertThat
import com.kibotu.ruler.model.AppFile
import com.kibotu.ruler.model.FileType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class VerifierTest {
    @Test
    fun `passes when sizes are within thresholds`() {
        val verifier = Verifier(
            VerificationConfig(downloadSizeThreshold = 100, installSizeThreshold = 200),
        )
        val files = listOf(appFile(download = 50, install = 100))

        verifier.verify(files)
    }

    @Test
    fun `fails when download size exceeds threshold`() {
        val verifier = Verifier(
            VerificationConfig(downloadSizeThreshold = 10, installSizeThreshold = 200),
        )
        val files = listOf(appFile(download = 50, install = 100))

        val exception = assertThrows<SizeExceededException> { verifier.verify(files) }

        assertThat(exception.message).contains("Download")
    }

    @Test
    fun `fails when install size exceeds threshold`() {
        val verifier = Verifier(
            VerificationConfig(downloadSizeThreshold = 200, installSizeThreshold = 10),
        )
        val files = listOf(appFile(download = 5, install = 50))

        val exception = assertThrows<SizeExceededException> { verifier.verify(files) }

        assertThat(exception.message).contains("Install")
    }

    @Test
    fun `a size exactly at the threshold passes`() {
        val verifier = Verifier(VerificationConfig(downloadSizeThreshold = 50, installSizeThreshold = 100))
        val files = listOf(appFile(download = 50, install = 100))

        verifier.verify(files)
    }

    @Test
    fun `sizes are summed across all files`() {
        val verifier = Verifier(VerificationConfig(downloadSizeThreshold = 10, installSizeThreshold = 100))
        val files = listOf(appFile(download = 6, install = 1), appFile(download = 6, install = 1))

        val exception = assertThrows<SizeExceededException> { verifier.verify(files) }

        assertThat(exception).hasMessageThat().contains("2 bytes above")
    }

    private fun appFile(download: Long, install: Long) =
        AppFile("com.example.Foo", FileType.CLASS, download, install)
}
