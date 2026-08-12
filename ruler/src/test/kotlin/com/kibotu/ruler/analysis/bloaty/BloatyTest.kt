package com.kibotu.ruler.analysis.bloaty

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class BloatyTest {

    @Test
    fun `returns nothing without an unstripped library`(@TempDir tempDir: File) {
        val bloaty = Bloaty(path = "/usr/bin/true")

        assertThat(bloaty.parseCompileUnits("stripped".toByteArray(), debugFile = null)).isEmpty()
    }

    @Test
    fun `returns nothing when bloaty is not installed`(@TempDir tempDir: File) {
        val debugFile = tempDir.resolve("libsample.so").apply { writeText("debug") }
        val bloaty = Bloaty(path = tempDir.resolve("no-such-bloaty").absolutePath)

        assertThat(bloaty.parseCompileUnits("stripped".toByteArray(), debugFile)).isEmpty()
    }

    @Test
    fun `parses the csv that bloaty prints`(@TempDir tempDir: File) {
        val debugFile = tempDir.resolve("libsample.so").apply { writeText("debug") }
        val fakeBloaty = fakeBloaty(
            tempDir,
            "compileunits,vmsize,filesize",
            "../../src/audio/mixer.cc,1024,2048",
            "../../src/video/decoder.cc,512,4096",
            "[section .text],0,128",
            "malformed line",
        )

        val units = Bloaty(fakeBloaty.absolutePath).parseCompileUnits("stripped".toByteArray(), debugFile)

        assertThat(units.map { it.name }).containsExactly(
            "/src/audio/mixer.cc",
            "/src/video/decoder.cc",
            "[section .text]",
        ).inOrder()
        assertThat(units.first().installSize).isEqualTo(2048)
    }

    /** A script that prints canned CSV, so the test does not need Bloaty installed. */
    private fun fakeBloaty(tempDir: File, vararg lines: String): File {
        return tempDir.resolve("bloaty").apply {
            writeText(lines.joinToString("\n", prefix = "#!/bin/sh\ncat <<'EOF'\n", postfix = "\nEOF\n"))
            setExecutable(true)
        }
    }
}
