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
    fun `scores a candidate treemap row against the value left to lay out`() {
        // The row cost was normalised by the region's pixel area, which left the side
        // length it minimised unrelated to the side the row was laid out on. Cells came
        // out as slivers: a p90 aspect ratio of 17:1, and 45% of them worse than 3:1,
        // against 1.5 and none for the same data laid out by d3. The row side has to be
        // normalised by the remaining value, as d3 does.
        val body = bodyOf("squarify")

        assertThat(body).doesNotContain("area/remaining")
        assertThat(body).contains("remaining*TREEMAP_RATIO")
    }

    @Test
    fun `gives every treemap cell its share of the region`() {
        // A row spans the region's shorter side and its cells divide the other one, so a
        // cell's width comes from the region's width and its height from the row. Sizing
        // both from the row instead made the row cover the square of the row's thickness
        // rather than the region's area, and the cells drifted off the canvas.
        val body = bodyOf("squarify")

        assertThat(body).contains("n.installSize*w/sum")
        assertThat(body).contains("n.installSize*h/sum")
    }

    @Test
    fun `does not lay a treemap group out inside its own padding`() {
        // A group only a little larger than the gutter it is inset by was handed a box
        // smaller than that gutter, leaving a sub-pixel cell. Those were the 1px stripes
        // that made the chart unreadable.
        val body = bodyOf("treemapRects")

        assertThat(body).contains("r.width-2*TREEMAP_PAD>=TREEMAP_MIN_GROUP")
        assertThat(body).contains("r.height-2*TREEMAP_PAD>=TREEMAP_MIN_GROUP")
    }

    @Test
    fun `draws the treemap at its final size before animating it`() {
        // The cells were emitted at zero size and only the animation gave them one, so
        // reduced motion, scripting turned off, and any headless screenshot all captured
        // an empty chart. The final geometry is what gets written.
        val body = bodyOf("treemap")

        assertThat(body).contains("""width="'+cw+'" height="'+ch+'"""")
        assertThat(body).doesNotContain("""width="0" height="0"""")
    }

    @Test
    fun `restores the treemap when its animation frames do not arrive`() {
        // Frames are throttled in a background tab and never arrive in some headless
        // captures, which left every cell stranded part-grown. A half-drawn treemap reads
        // as broken, so a timer puts the final geometry back.
        val body = bodyOf("growTreemap")

        assertThat(body).contains("setTimeout(settle,")
        assertThat(body).contains("(prefers-reduced-motion: reduce)")
    }

    @Test
    fun `shows a treemap cell's share of the app in its tooltip`() {
        // The tooltip gave an absolute size with nothing to compare it against.
        assertThat(bodyOf("treemap")).contains("('+pct+'%)")
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
