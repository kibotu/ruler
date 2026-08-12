package com.kibotu.ruler.common.report

import com.kibotu.ruler.models.AppReport
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** Responsible for generating HTML reports from a template with embedded JSON data. */
class HtmlReporter {

    /**
     * Generates a self-contained HTML report.
     *
     * @param report The AppReport to embed.
     * @param insights The pre-computed ReportInsights to embed.
     * @param targetDir Directory where the generated report will be located.
     * @return Generated HTML report file.
     */
    fun write(report: AppReport, insights: ReportInsights, targetDir: File): File {
        val template = readResourceFile("ruler-report.html")
        val format = Json { prettyPrint = false }

        val reportJson = format.encodeToString(report).htmlSafeJson()
        val insightsJson = format.encodeToString(insights).htmlSafeJson()

        val html = template
            .replaceFirst("__RULER_REPORT__", reportJson)
            .replaceFirst("__RULER_INSIGHTS__", insightsJson)

        val reportFile = targetDir.resolve("report.html")
        reportFile.writeText(html, Charsets.UTF_8)
        return reportFile
    }

    /**
     * Escapes `<` as `\u003c` so the JSON cannot close a `<script>` tag.
     * The result is still valid JSON.
     */
    private fun String.htmlSafeJson(): String = replace("<", "\\u003c")

    private fun readResourceFile(fileName: String): String {
        val url = requireNotNull(javaClass.getResource("/$fileName"))
        return url.readText()
    }
}
