package com.kibotu.ruler.common.bloaty

import com.kibotu.ruler.common.apk.ApkEntry
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

private const val COLUMN_SIZE = 3

/** Optional native library analysis via the Bloaty CLI. */
class Bloaty(val path: String? = null) {

    private val bloatyPath: String? by lazy { findBloatyPath() }

    private fun findBloatyPath(): String? {
        val resolved = path ?: executeCommandAndGetOutput("which bloaty").singleOrNull()
        return resolved?.takeIf { it.isNotEmpty() }
    }

    fun parseNativeLibraryEntry(bytes: ByteArray, debugFile: File?): List<ApkEntry.Default> {
        if (bloatyPath == null || debugFile == null) {
            return emptyList()
        }

        val tmpFile = File.createTempFile("native-lib", ".so").apply {
            writeBytes(bytes)
        }.also { it.deleteOnExit() }

        val command =
            "$bloatyPath --debug-file=${debugFile.absolutePath} ${tmpFile.absolutePath} -d compileunits -n 0 --csv"

        return parseBloatyOutputToApkEntry(command)
    }

    private fun parseBloatyOutputToApkEntry(command: String): List<ApkEntry.Default> {
        val rows = mutableListOf<ApkEntry.Default>()
        val outputLines = executeCommandAndGetOutput(command)

        for (line in outputLines) {
            val cols = line.split(",")
            if (cols.size == COLUMN_SIZE) {
                val size = cols.last().toLongOrNull() ?: continue
                rows += ApkEntry.Default(
                    cols.first().substringAfter("../.."),
                    size,
                    size,
                )
            }
        }
        return rows
    }

    private fun executeCommandAndGetOutput(command: String): List<String> {
        val process = Runtime.getRuntime().exec(command)
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val outputLines = mutableListOf<String>()

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            outputLines.add(line ?: "")
        }

        process.waitFor()
        return outputLines
    }
}
