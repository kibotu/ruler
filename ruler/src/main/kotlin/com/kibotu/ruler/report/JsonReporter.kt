package com.kibotu.ruler.report

import com.kibotu.ruler.model.AppReport
import kotlinx.serialization.json.Json
import java.io.File

/** Writes the machine-readable report. */
class JsonReporter {

    /** @return The `report.json` file in [targetDir]. */
    fun write(report: AppReport, targetDir: File): File {
        val reportFile = targetDir.resolve("report.json")
        reportFile.writeText(Json.encodeToString(report))
        return reportFile
    }
}
