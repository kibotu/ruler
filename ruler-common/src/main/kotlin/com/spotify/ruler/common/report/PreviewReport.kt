package com.spotify.ruler.common.report

import com.spotify.ruler.models.AppReport
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Standalone entry point for generating a preview report without an Android build.
 * Usage: ./gradlew :ruler-common:previewReport [-Pjson=path/to/report.json]
 */
fun main(args: Array<String>) {
    val jsonText = if (args.isNotEmpty()) {
        val file = File(args[0])
        require(file.exists()) { "JSON file not found: ${args[0]}" }
        file.readText()
    } else {
        val fixture = File("src/test/resources/fixtures/report-sample.json")
        require(fixture.exists()) { "Fixture not found: ${fixture.absolutePath}" }
        fixture.readText()
    }

    val report = Json.decodeFromString<AppReport>(jsonText)
    val insights = ReportInsights.from(report)
    val outputDir = File("build/preview")
    outputDir.mkdirs()
    val htmlFile = HtmlReporter().write(report, insights, outputDir)
    println("Preview report written to ${htmlFile.toURI()}")
}
