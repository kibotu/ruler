package com.kibotu.ruler.report

import com.kibotu.ruler.model.AppReport
import kotlinx.serialization.json.Json
import java.io.File

/** Writes the visual report by filling the data into an HTML template. */
class HtmlReporter {

    /** @return The [FILE_NAME] file in [targetDir]. It loads no external resources. */
    fun write(report: AppReport, targetDir: File): File {
        val template = requireNotNull(javaClass.getResource("/$TEMPLATE")) { "Missing $TEMPLATE" }.readText()
        val html = template.replaceFirst(PLACEHOLDER, Json.encodeToString(report).htmlSafe())

        val reportFile = targetDir.resolve(FILE_NAME)
        reportFile.writeText(html, Charsets.UTF_8)
        return reportFile
    }

    /**
     * Escapes every `<` as a unicode escape, so that the data cannot close the script tag that
     * holds it. The result is still valid JSON.
     */
    private fun String.htmlSafe(): String = replace("<", "\\u003c")

    companion object {
        const val FILE_NAME = "report.html"

        private const val TEMPLATE = "ruler-report.html"
        private const val PLACEHOLDER = "__RULER_REPORT__"
    }
}
