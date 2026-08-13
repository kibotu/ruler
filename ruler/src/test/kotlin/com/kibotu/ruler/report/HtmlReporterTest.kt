package com.kibotu.ruler.report

import com.google.common.truth.Truth.assertThat
import com.kibotu.ruler.model.AppComponent
import com.kibotu.ruler.model.AppFile
import com.kibotu.ruler.model.AppReport
import com.kibotu.ruler.model.ComponentType
import com.kibotu.ruler.model.DynamicFeature
import com.kibotu.ruler.model.FileType
import com.kibotu.ruler.model.ResourceType
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class HtmlReporterTest {
    private val reporter = HtmlReporter()

    /** Carries a unicode name and a name full of markup, so that both survive into every test. */
    private val report = AppReport(
        name = "com.kibotu.ruler.sample",
        version = "1.2.3",
        variant = "release",
        downloadSize = 750,
        installSize = 1250,
        components = listOf(
            AppComponent(
                name = ":module-\u00e4",
                type = ComponentType.INTERNAL,
                downloadSize = 250,
                installSize = 450,
                files = listOf(
                    AppFile("com.kibotu.sample.MainActivity", FileType.CLASS, 100, 200),
                    AppFile("/res/layout/main.xml", FileType.RESOURCE, 150, 250, resourceType = ResourceType.LAYOUT),
                ),
                owner = "app-team",
            ),
            AppComponent(
                name = "</script><script>alert('x')</script>",
                type = ComponentType.EXTERNAL,
                downloadSize = 300,
                installSize = 500,
                files = listOf(AppFile("ext.class", FileType.CLASS, 300, 500)),
                owner = "ext-team",
            ),
        ),
        dynamicFeatures = listOf(
            DynamicFeature(
                name = "dynamic",
                downloadSize = 200,
                installSize = 300,
                files = listOf(AppFile("DynActivity.class", FileType.CLASS, 200, 300)),
                owner = "dynamic-team",
            ),
        ),
    )

    @Test
    fun `writes a single self-contained report`(@TempDir targetDir: File) {
        val file = reporter.write(report, targetDir)

        assertThat(file.name).isEqualTo("report.html")
        assertThat(targetDir.listFiles()!!.map(File::getName)).containsExactly("report.html")
        assertThat(file.readText()).doesNotContain("http://")
        assertThat(file.readText()).doesNotContain("https://")
    }

    @Test
    fun `fills the report into the template`(@TempDir targetDir: File) {
        val html = reporter.write(report, targetDir).readText(Charsets.UTF_8)

        assertThat(html).doesNotContain("__RULER_REPORT__")
        assertThat(Json.decodeFromString<AppReport>(payloadOf(html))).isEqualTo(report)
    }

    @Test
    fun `escapes markup so that the data cannot close its script tag`(@TempDir targetDir: File) {
        val payload = payloadOf(reporter.write(report, targetDir).readText(Charsets.UTF_8))

        assertThat(payload).doesNotContain("<")
        assertThat(payload).contains("\\u003c/script>")
    }

    @Test
    fun `overwrites an existing report`(@TempDir targetDir: File) {
        reporter.write(report, targetDir)

        assertThat(reporter.write(report, targetDir).length()).isGreaterThan(0)
    }

    /** The JSON that the template hands to the page. */
    private fun payloadOf(html: String): String {
        val start = html.indexOf(PAYLOAD_TAG) + PAYLOAD_TAG.length
        return html.substring(start, html.indexOf("</script>", start))
    }

    private companion object {
        const val PAYLOAD_TAG = """<script type="application/json" id="ruler-report">"""
    }
}
