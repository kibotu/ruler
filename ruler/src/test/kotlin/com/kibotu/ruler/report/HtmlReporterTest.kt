package com.kibotu.ruler.report

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
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
    fun `sorts the treemap on the metric it lays out by`() {
        // The treemap sized cells by installSize while picking which components and files
        // to show by downloadSize, so the largest cell was not necessarily the component
        // that survived the top-N cut. It should not mention download size at all.
        val body = bodyOf("buildTreemapNodes")

        assertThat(body).contains("b.installSize-a.installSize")
        assertThat(body).doesNotContain("downloadSize")
    }

    @Test
    fun `counts a co-owned component under every owner`() {
        // The chart counted only the primary owner while the drill-down counted all of
        // them, so the two disagreed and the chart's totals fell short of the app total.
        val body = bodyOf("prepareOwnerGroupData")

        assertThat(body).contains("allOwners(c)")
        assertThat(body).doesNotContain("displayOwner")
    }

    @Test
    fun `lets a tab override the size key used by its module cards`() {
        // The ownership drill-down and the dynamic feature list used the Breakdown tab's
        // sort, so their own sort control had no effect and their headers disagreed with
        // the file lists underneath them.
        assertThat(bodyOf("renderModuleCard"))
            .startsWith("function renderModuleCard(component,index,prefix,displayLabel,sizeKey)")
        assertThat(template()).contains("renderModuleCard(c,i,'o'+idx,label,key)")
        assertThat(template()).contains("renderModuleCard(comp,i,'d',label,key)")
    }

    @Test
    fun `overwrites an existing report`(@TempDir targetDir: File) {
        reporter.write(report, targetDir)

        assertThat(reporter.write(report, targetDir).length()).isGreaterThan(0)
    }

    /** The page's script, as shipped in the template resource. */
    private fun template(): String =
        checkNotNull(javaClass.getResourceAsStream("/ruler-report.html")) {
            "template resource not found"
        }.bufferedReader().readText()

    /**
     * [functionName] out of [template], with its comments stripped and its whitespace
     * collapsed.
     *
     * Scoping an assertion to one function keeps it from passing or failing on an
     * unrelated part of the page. Stripping comments keeps it from tripping over prose,
     * and the collapsed whitespace keeps it from failing on a reformat.
     */
    private fun bodyOf(functionName: String): String {
        val script = template()
        val start = script.indexOf("function $functionName(")
        assertWithMessage("$functionName is not in the template").that(start).isAtLeast(0)
        var depth = 0
        var i = script.indexOf('{', start)
        while (i < script.length) {
            if (script[i] == '{') depth++
            if (script[i] == '}' && --depth == 0) break
            i++
        }
        assertWithMessage("$functionName is not closed").that(i).isLessThan(script.length)
        return script.substring(start, i + 1)
            .replace(Regex("//[^\\n]*"), " ")
            .replace(Regex("\\s+"), " ")
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
