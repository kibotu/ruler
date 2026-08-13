package com.kibotu.ruler.report

import com.kibotu.ruler.model.AppReport
import kotlinx.serialization.json.Json
import java.io.File

/** Writes the machine-readable report. */
class JsonReporter {

    /** @return The [FILE_NAME] file in [targetDir]. */
    fun write(report: AppReport, targetDir: File): File {
        val reportFile = targetDir.resolve(FILE_NAME)
        reportFile.writeText(Json.encodeToString(report))
        return reportFile
    }

    companion object {
        const val FILE_NAME = "report.json"
    }
}
