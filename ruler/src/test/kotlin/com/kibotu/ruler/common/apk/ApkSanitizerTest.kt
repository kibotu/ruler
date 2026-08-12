package com.kibotu.ruler.common.apk

import com.google.common.truth.Truth.assertThat
import com.kibotu.ruler.common.sanitizer.ClassNameSanitizer
import com.kibotu.ruler.common.sanitizer.ResourceNameSanitizer
import com.kibotu.ruler.models.FileType
import org.junit.jupiter.api.Test

class ApkSanitizerTest {
    private val sanitizer = ApkSanitizer(
        ClassNameSanitizer(null as java.io.File?),
        ResourceNameSanitizer(null as java.io.File?),
    )

    @Test
    fun `distributes dex size proportionally across classes`() {
        val entries = listOf(
            ApkEntry.Dex(
                name = "classes.dex",
                downloadSize = 100,
                installSize = 200,
                classes = listOf(
                    ApkEntry.Default("com.example.A", 30, 30),
                    ApkEntry.Default("com.example.B", 70, 70),
                ),
            ),
        )

        val files = sanitizer.sanitize(entries)

        assertThat(files).hasSize(2)
        assertThat(files.sumOf { it.downloadSize }).isEqualTo(100)
        assertThat(files.sumOf { it.installSize }).isEqualTo(200)
        assertThat(files.map { it.type }.distinct()).containsExactly(FileType.CLASS)
    }

    @Test
    fun `discards bundletool-only files`() {
        val entries = listOf(
            ApkEntry.Default("/META-INF/MANIFEST.MF", 10, 10),
            ApkEntry.Default("/res/xml/splits0.xml", 20, 20),
            ApkEntry.Default("/assets/config.json", 30, 30),
        )

        val files = sanitizer.sanitize(entries)

        assertThat(files).hasSize(1)
        assertThat(files.single().name).isEqualTo("/assets/config.json")
        assertThat(files.single().type).isEqualTo(FileType.ASSET)
    }

    @Test
    fun `keeps largest android manifest`() {
        val entries = listOf(
            ApkEntry.Default("/AndroidManifest.xml", 10, 10),
            ApkEntry.Default("/AndroidManifest.xml", 50, 80),
        )

        val files = sanitizer.sanitize(entries)

        assertThat(files).hasSize(1)
        assertThat(files.single().installSize).isEqualTo(80)
    }
}
