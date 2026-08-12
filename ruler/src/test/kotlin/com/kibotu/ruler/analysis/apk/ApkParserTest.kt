package com.kibotu.ruler.analysis.apk

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ApkParserTest {
    @Test
    fun `parses asset and resource entries from apk`(@TempDir tempDir: File) {
        val apk = createTestApk(
            tempDir,
            "assets/config.json" to """{"enabled":true}""".toByteArray(),
            "res/raw/greeting.txt" to "hello".toByteArray(),
        )

        val entries = ApkParser().parse(apk)

        assertThat(entries.map { it.name }).containsAtLeast(
            "/assets/config.json",
            "/res/raw/greeting.txt",
        )
        assertThat(entries.all { it.downloadSize >= 0 && it.installSize >= 0 }).isTrue()
    }

    @Test
    fun `normalizes lib res paths to res paths`(@TempDir tempDir: File) {
        val apk = createTestApk(
            tempDir,
            "lib/res/layout/activity_main.xml" to "<LinearLayout/>".toByteArray(),
        )

        val entries = ApkParser().parse(apk)

        assertThat(entries.single().name).isEqualTo("/res/layout/activity_main.xml")
    }

    @Test
    fun `matches unstripped native library by file name`(@TempDir tempDir: File) {
        val unstripped = tempDir.resolve("libsample-unstripped.so").apply { writeText("debug") }
        val apk = createTestApk(
            tempDir,
            "lib/arm64-v8a/libsample.so" to "stripped".toByteArray(),
        )

        val entries = ApkParser(listOf(unstripped)).parse(apk)
        val nativeEntry = entries.single() as ApkEntry.NativeLibrary

        assertThat(nativeEntry.name).endsWith("libsample.so")
        assertThat(nativeEntry.units).isEmpty()
    }

    private fun createTestApk(tempDir: File, vararg files: Pair<String, ByteArray>): File {
        val apk = tempDir.resolve("test.apk")
        ZipOutputStream(apk.outputStream()).use { zip ->
            files.forEach { (path, bytes) ->
                zip.putNextEntry(ZipEntry(path))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return apk
    }
}
