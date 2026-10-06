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
        // It sized cells by installSize while picking what to show by downloadSize, so the
        // largest cell was not necessarily the component that survived the top-N cut.
        val body = bodyOf("buildTreemapNodes")

        assertThat(body).contains("b.installSize-a.installSize")
        assertThat(body).doesNotContain("downloadSize")
    }

    @Test
    fun `scores a candidate treemap row against the value left to lay out`() {
        // Normalising the row cost by the region's pixel area left the side length it
        // minimised unrelated to the side the row was laid out on: p90 aspect ratio 17:1,
        // 45% of cells worse than 3:1, against 1.5 and none laid out by d3.
        val body = bodyOf("squarify")

        assertThat(body).doesNotContain("area/remaining")
        assertThat(body).contains("remaining*TREEMAP_RATIO")
    }

    @Test
    fun `gives every treemap cell its share of the region`() {
        // Sizing both sides from the row made the row cover the square of its own thickness
        // rather than the region's area, and the cells drifted off the canvas.
        val body = bodyOf("squarify")

        assertThat(body).contains("n.installSize*w/sum")
        assertThat(body).contains("n.installSize*h/sum")
    }

    @Test
    fun `does not lay a treemap group out inside its own padding`() {
        // A group barely larger than its gutter got a box smaller than the gutter, leaving a
        // sub-pixel cell. Those 1px stripes made the chart unreadable.
        val body = bodyOf("treemapRects")

        assertThat(body).contains("r.width-2*TREEMAP_PAD>=TREEMAP_MIN_GROUP")
        assertThat(body).contains("r.height-2*TREEMAP_PAD>=TREEMAP_MIN_GROUP")
    }

    @Test
    fun `draws the treemap at its final size before animating it`() {
        // Cells emitted at zero size only got one from the animation, so reduced motion,
        // scripting off, and any headless screenshot all captured an empty chart.
        val body = bodyOf("treemap")

        assertThat(body).contains("""width="'+cw+'" height="'+ch+'"""")
        assertThat(body).doesNotContain("""width="0" height="0"""")
    }

    @Test
    fun `restores the treemap when its animation frames do not arrive`() {
        // Frames are throttled in a background tab and never arrive in some headless
        // captures, which left every cell stranded part-grown.
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
        // The chart counted only the primary owner while the drill-down counted all of them,
        // so the totals disagreed and fell short of the app total.
        val body = bodyOf("prepareOwnerGroupData")

        assertThat(body).contains("allOwners(c)")
        assertThat(body).doesNotContain("displayOwner")
    }

    @Test
    fun `lets a tab override the size key used by its module cards`() {
        // The ownership drill-down and the feature list used the Breakdown tab's sort, so
        // their own sort control had no effect and their headers disagreed with the lists.
        assertThat(bodyOf("renderModuleCard"))
            .startsWith("function renderModuleCard(component,index,prefix,displayLabel,sizeKey)")
        assertThat(template()).contains("renderModuleCard(c,i,'o'+idx,label,key)")
        assertThat(template()).contains("renderModuleCard(comp,i,'d',label,key)")
    }

    @Test
    fun `splits a module card's files into four labelled sections`() {
        // One list held everything that was not an asset, so dex, native code, resources
        // and the manifest were indistinguishable under a single "Source Files" heading.
        // The order is assets, resources, then code, and it is fixed so that two cards
        // read the same way and can be compared by scanning down them.
        val titles = Regex("title:'([^']+)'").findAll(fileParts()).map { it.groupValues[1] }.toList()

        assertThat(titles).containsExactly("Asset Files", "Resources", "Native", "Source Files").inOrder()
    }

    @Test
    fun `does not rank a module card's sections by size`() {
        // Ranking put whichever section was largest first, so the order a card read in
        // depended on the component and two components could not be scanned the same way.
        assertThat(bodyOf("renderModuleDetails")).contains("FILE_PARTS.map(function(p){return renderFilePart(p,files,listSizeKey);})")
        assertThat(template()).doesNotContain("b.total-a.total")
    }

    @Test
    fun `gives every file type exactly one section`() {
        // A file no bucket claims disappears from the card, and one two buckets claim is
        // counted twice. Tying this to the enum means a type added later fails here rather
        // than quietly going missing.
        val parts = fileParts()

        FileType.entries.forEach { type ->
            val claimed = Regex("f\\.type==='${type.name}'").findAll(parts).count()
            assertWithMessage("$type is claimed by $claimed sections").that(claimed).isEqualTo(1)
        }
    }

    @Test
    fun `lists a module card's files largest first`() {
        // Caliper orders its resource list by size, and a section listed any other way
        // buries the file that made it worth opening. This is within a section: the
        // sections themselves keep the fixed order.
        assertThat(bodyOf("renderFilePart"))
            .contains("fileSize(b,sizeKey)-fileSize(a,sizeKey)")
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

    /** [functionName] out of [template], comments stripped and whitespace collapsed, so an assertion
     *  cannot pass or fail on an unrelated part of the page. */
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

    /** The buckets a module card's file lists are split into, as the template declares them. */
    private fun fileParts(): String =
        template().substringAfter("var FILE_PARTS=[").substringBefore("];")

    /** The JSON that the template hands to the page. */
    private fun payloadOf(html: String): String {
        val start = html.indexOf(PAYLOAD_TAG) + PAYLOAD_TAG.length
        return html.substring(start, html.indexOf("</script>", start))
    }

    private companion object {
        const val PAYLOAD_TAG = """<script type="application/json" id="ruler-report">"""
    }
}
