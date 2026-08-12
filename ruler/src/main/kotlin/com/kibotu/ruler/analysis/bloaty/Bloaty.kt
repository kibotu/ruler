package com.kibotu.ruler.analysis.bloaty

import com.kibotu.ruler.analysis.apk.ApkEntry
import java.io.File
import java.io.IOException

private const val CSV_COLUMNS = 3

/**
 * Reads native library size per compile unit with the Bloaty CLI.
 *
 * @param path Path to the Bloaty executable. Ruler looks Bloaty up on `PATH` when this is null.
 */
class Bloaty(path: String? = null) {

    private val executable: String? by lazy {
        (path ?: run("which", "bloaty").singleOrNull())?.takeIf(String::isNotEmpty)
    }

    /**
     * Returns one entry for each compile unit in the library.
     *
     * Returns an empty list when Bloaty is not installed, or when the unstripped copy of the
     * library is not available. Bloaty needs the debug symbols that stripping removes.
     */
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

    /**
     * Runs a command and returns its output.
     *
     * The arguments are passed as a list, so a path that contains a space still works. Standard
     * error is merged into standard output, so a chatty command cannot fill the error pipe and
     * block.
     */
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
