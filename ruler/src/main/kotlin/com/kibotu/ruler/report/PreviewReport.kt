package com.kibotu.ruler.report

import com.kibotu.ruler.model.AppReport
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Renders `report.html` from a `report.json`, so that the template can be worked on without an
 * Android build.
 *
 * @param args The report to read, followed by the directory to write to.
 */
fun main(args: Array<String>) {
    require(args.size == 2) { "Usage: previewReport <report.json> <output-dir>" }

    val reportFile = File(args[0])
    require(reportFile.exists()) { "JSON report not found: $reportFile" }
    val outputDir = File(args[1]).apply { mkdirs() }

    val report = Json.decodeFromString<AppReport>(reportFile.readText())
    println("Preview report written to ${HtmlReporter().write(report, outputDir).toURI()}")
}
