package com.spotify.ruler.common.report

import com.spotify.ruler.models.AppReport
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** Responsible for generating JSON reports. */
class JsonReporter {

    /**
     * Serializes an [AppReport] to a JSON file.
     *
     * @param report The report to serialize.
     * @param targetDir Directory where the generated report will be located.
     * @return Generated JSON report file.
     */
    fun write(report: AppReport, targetDir: File): File {
        val format = Json { prettyPrint = false }
        val reportFile = targetDir.resolve("report.json")
        reportFile.writeText(format.encodeToString(report))
        return reportFile
    }
}
