package com.kibotu.ruler.analysis.bloaty

import com.kibotu.ruler.analysis.apk.ApkEntry
import java.io.File
import java.io.IOException

private const val CSV_COLUMNS = 3

/** Reads native library size per compile unit with the Bloaty CLI. */
class Bloaty(path: String? = null) {

    private val executable: String? by lazy {
        (path ?: run("which", "bloaty").singleOrNull())?.takeIf(String::isNotEmpty)
    }

    /** Empty when Bloaty is not installed, or the unstripped library is missing. Bloaty needs the
     *  debug symbols that stripping removes. */
    fun parseCompileUnits(bytes: ByteArray, debugFile: File?): List<ApkEntry.Default> {
        val bloaty = executable ?: return emptyList()
        if (debugFile == null) return emptyList()

        val library = File.createTempFile("native-lib", ".so").apply {
            deleteOnExit()
            writeBytes(bytes)
        }

        return run(
            bloaty,
            "--debug-file=${debugFile.absolutePath}",
            library.absolutePath,
            "-d", "compileunits",
            "-n", "0",
            "--csv",
        ).mapNotNull { line ->
            val columns = line.split(",")
            if (columns.size != CSV_COLUMNS) return@mapNotNull null
            val size = columns.last().toLongOrNull() ?: return@mapNotNull null
            ApkEntry.Default(columns.first().substringAfter("../.."), size, size)
        }
    }

    /** Merges stderr into stdout so that a chatty command cannot fill the error pipe and block. */
    private fun run(vararg command: String): List<String> = try {
        val process = ProcessBuilder(*command)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readLines() }
        process.waitFor()
        output
    } catch (_: IOException) {
        emptyList() // The command is not on this machine. Bloaty analysis is optional.
    }
}
