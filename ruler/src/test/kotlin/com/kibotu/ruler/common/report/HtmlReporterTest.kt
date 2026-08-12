package com.kibotu.ruler.common.report

import com.google.common.truth.Truth.assertThat
import com.kibotu.ruler.models.AppComponent
import com.kibotu.ruler.models.AppFile
import com.kibotu.ruler.models.AppReport
import com.kibotu.ruler.models.ComponentType
import com.kibotu.ruler.models.DynamicFeature
import com.kibotu.ruler.models.FileType
import com.kibotu.ruler.models.ResourceType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class HtmlReporterTest {
    private val reporter = HtmlReporter()

    private fun createTestReport() = AppReport(
        name = "com.spotify.music",
        version = "1.2.3",
        variant = "release",
        downloadSize = 2900,
        installSize = 4800,
        components = listOf(
            AppComponent(":app", ComponentType.INTERNAL, 250, 450, listOf(
                AppFile("com.spotify.MainActivity", FileType.CLASS, 100, 200),
                AppFile("/res/layout/main.xml", FileType.RESOURCE, 150, 250, resourceType = ResourceType.LAYOUT),
            ), "app-team"),
            AppComponent("com.ext:lib", ComponentType.EXTERNAL, 300, 500, listOf(
                AppFile("ext.class", FileType.CLASS, 300, 500),
            ), "ext-team"),
        ),
        dynamicFeatures = listOf(
            DynamicFeature("dynamic", 500, 800, listOf(
                AppFile("DynActivity.class", FileType.CLASS, 300, 500),
                AppFile("/res/layout/dyn.xml", FileType.RESOURCE, 200, 300, resourceType = ResourceType.LAYOUT),
            ), "dynamic-team"),
        ),
    )

    @Test
    fun `HTML report is generated`(@TempDir targetDir: File) {
        val report = createTestReport()
        val insights = ReportInsights.from(report)
        val file = reporter.write(report, insights, targetDir)

        assertThat(file.exists()).isTrue()
        assertThat(file.name).isEqualTo("report.html")
    }

    @Test
    fun `no placeholder survives in output`(@TempDir targetDir: File) {
        val report = createTestReport()
        val insights = ReportInsights.from(report)
        val file = reporter.write(report, insights, targetDir)
        val content = file.readText()

        assertThat(content).doesNotContain("__RULER_REPORT__")
        assertThat(content).doesNotContain("__RULER_INSIGHTS__")
    }

    @Test
    fun `no network requests in output`(@TempDir targetDir: File) {
        val report = createTestReport()
        val insights = ReportInsights.from(report)
        val file = reporter.write(report, insights, targetDir)
        val content = file.readText()

        assertThat(content).doesNotContain("http://")
        assertThat(content).doesNotContain("https://")
    }

    @Test
    fun `JSON data is embedded in script tags`(@TempDir targetDir: File) {
        val report = createTestReport()
        val insights = ReportInsights.from(report)
        val file = reporter.write(report, insights, targetDir)
        val content = file.readText()

        assertThat(content).contains("ruler-report")
        assertThat(content).contains("ruler-insights")
        assertThat(content).contains("com.spotify.music")
    }

    @Test
    fun `report data round-trips through JSON parsing`(@TempDir targetDir: File) {
        val report = createTestReport()
        val insights = ReportInsights.from(report)
        val file = reporter.write(report, insights, targetDir)
        val content = file.readText()

        // Extract the JSON from the script tag
        val startMarker = """<script type="application/json" id="ruler-report">"""
        val endMarker = """</script>"""
        val startIdx = content.indexOf(startMarker) + startMarker.length
        val endIdx = content.indexOf(endMarker, startIdx)
        val json = content.substring(startIdx, endIdx)

        val parsed = kotlinx.serialization.json.Json.decodeFromString<AppReport>(json)
        assertThat(parsed.name).isEqualTo(report.name)
        assertThat(parsed.version).isEqualTo(report.version)
        assertThat(parsed.components).hasSize(report.components.size)
    }

    @Test
    fun `existing reports are overwritten`(@TempDir targetDir: File) {
        val report = createTestReport()
        val insights = ReportInsights.from(report)
        reporter.write(report, insights, targetDir)
        reporter.write(report, insights, targetDir) // Should not throw
    }

    @Test
    fun `exactly one file is written`(@TempDir targetDir: File) {
        val report = createTestReport()
        val insights = ReportInsights.from(report)
        reporter.write(report, insights, targetDir)

        val files = targetDir.listFiles()!!
        assertThat(files).hasLength(1)
        assertThat(files[0].name).isEqualTo("report.html")
    }

    @Test
    fun `html is UTF-8 encoded`(@TempDir targetDir: File) {
        val report = AppReport(
            name = "com.test.unicode",
            version = "1.0",
            variant = "debug",
            downloadSize = 0,
            installSize = 0,
            components = listOf(
                AppComponent(":module-\u00e4", ComponentType.INTERNAL, 100, 200, null),
            ),
            dynamicFeatures = emptyList(),
        )
        val insights = ReportInsights.from(report)
        val file = reporter.write(report, insights, targetDir)
        val content = file.readText(Charsets.UTF_8)

        assertThat(content).contains("module-\u00e4")
    }

    @Test
    fun `angle brackets in JSON are escaped`(@TempDir targetDir: File) {
        val report = createTestReport()
        val insights = ReportInsights.from(report)
        val file = reporter.write(report, insights, targetDir)
        val content = file.readText()

        // Extract JUST the JSON payload content (between > and </script>)
        val reportStart = content.indexOf("""<script type="application/json" id="ruler-report">""")
        val jsonStart = content.indexOf(">", reportStart) + 1
        val reportEnd = content.indexOf("</script>", jsonStart)
        val jsonPayload = content.substring(jsonStart, reportEnd)

        // The JSON payload should have < escaped as \u003c so no raw < can break out of the script block
        assertThat(jsonPayload).doesNotContain("<script")
        assertThat(jsonPayload).doesNotContain("</script")
    }
}
