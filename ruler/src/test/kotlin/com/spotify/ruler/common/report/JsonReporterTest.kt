package com.spotify.ruler.common.report

import com.google.common.truth.Truth.assertThat
import com.spotify.ruler.models.AppReport
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class JsonReporterTest {
    private val reporter = JsonReporter()

    @Test
    fun `JSON report is written`(@TempDir targetDir: File) {
        val report = AppReport(
            name = "com.test.app",
            version = "1.0.0",
            variant = "debug",
            downloadSize = 100,
            installSize = 200,
            components = emptyList(),
            dynamicFeatures = emptyList(),
        )
        val file = reporter.write(report, targetDir)

        assertThat(file.exists()).isTrue()
        assertThat(file.name).isEqualTo("report.json")

        val parsed = Json.decodeFromString<AppReport>(file.readText())
        assertThat(parsed).isEqualTo(report)
    }

    @Test
    fun `existing report is overwritten`(@TempDir targetDir: File) {
        val report = AppReport("test", "1.0", "debug", 0, 0, emptyList(), emptyList())
        reporter.write(report, targetDir)
        reporter.write(report, targetDir) // Should not throw
    }
}
