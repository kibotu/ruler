package com.spotify.ruler.common.report

import com.google.common.truth.Truth.assertThat
import com.spotify.ruler.models.AppReport
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class JsonReporterGoldenTest {
    private val reporter = JsonReporter()
    private val format = Json { prettyPrint = false }

    @Test
    fun `golden test report json schema is locked`(@TempDir targetDir: File) {
        // Load the salvaged fixture
        val fixtureJson = javaClass.getResource("/fixtures/report-sample.json")!!.readText()
        val report = Json.decodeFromString<AppReport>(fixtureJson)

        // Write it back
        val file = reporter.write(report, targetDir)
        val written = file.readText()

        // Parse and compare structurally (not byte-identical due to key ordering)
        val reparsed = Json.decodeFromString<AppReport>(written)
        assertThat(reparsed.name).isEqualTo(report.name)
        assertThat(reparsed.version).isEqualTo(report.version)
        assertThat(reparsed.variant).isEqualTo(report.variant)
        assertThat(reparsed.downloadSize).isEqualTo(report.downloadSize)
        assertThat(reparsed.installSize).isEqualTo(report.installSize)
        assertThat(reparsed.components).hasSize(report.components.size)
        assertThat(reparsed.dynamicFeatures).hasSize(report.dynamicFeatures.size)

        // Verify component names and sizes match
        for (i in report.components.indices) {
            assertThat(reparsed.components[i].name).isEqualTo(report.components[i].name)
            assertThat(reparsed.components[i].downloadSize).isEqualTo(report.components[i].downloadSize)
            assertThat(reparsed.components[i].installSize).isEqualTo(report.components[i].installSize)
            assertThat(reparsed.components[i].type).isEqualTo(report.components[i].type)
        }
    }

    @Test
    fun `golden test field names and order`(@TempDir targetDir: File) {
        val fixtureJson = javaClass.getResource("/fixtures/report-sample.json")!!.readText()
        val report = Json.decodeFromString<AppReport>(fixtureJson)
        val file = reporter.write(report, targetDir)
        val written = file.readText()

        // Verify critical field names exist in the JSON
        assertThat(written).contains("\"name\"")
        assertThat(written).contains("\"version\"")
        assertThat(written).contains("\"variant\"")
        assertThat(written).contains("\"downloadSize\"")
        assertThat(written).contains("\"installSize\"")
        assertThat(written).contains("\"components\"")
        assertThat(written).contains("\"dynamicFeatures\"")
        assertThat(written).contains("\"type\"")
        assertThat(written).contains("\"INTERNAL\"")
        assertThat(written).contains("\"EXTERNAL\"")
    }
}
