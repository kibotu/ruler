package com.kibotu.ruler.analysis.apk

import com.google.common.truth.Truth.assertThat
import com.kibotu.ruler.analysis.sanitizer.ClassNameSanitizer
import com.kibotu.ruler.analysis.sanitizer.ResourceNameSanitizer
import com.kibotu.ruler.model.FileType
import com.kibotu.ruler.model.ResourceType
import org.junit.jupiter.api.Test

class ApkSanitizerTest {
    private val sanitizer = ApkSanitizer(ClassNameSanitizer(), ResourceNameSanitizer())

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
    fun `an empty dex file does not divide by zero`() {
        val entries = listOf(ApkEntry.Dex("classes.dex", 100, 200, classes = emptyList()))

        assertThat(sanitizer.sanitize(entries)).isEmpty()
    }

    @Test
    fun `distributes native library size across compile units`() {
        val entries = listOf(
            ApkEntry.NativeLibrary(
                name = "/lib/arm64-v8a/libsample.so",
                downloadSize = 300,
                installSize = 900,
                units = listOf(
                    ApkEntry.Default("/src/a.cc", 100, 100),
                    ApkEntry.Default("/src/b.cc", 200, 200),
                ),
            ),
        )

        val files = sanitizer.sanitize(entries)

        assertThat(files.sumOf { it.downloadSize }).isEqualTo(300)
        assertThat(files.sumOf { it.installSize }).isEqualTo(900)
        assertThat(files.map { it.type }.distinct()).containsExactly(FileType.NATIVE_FILE)
    }

    @Test
    fun `section metadata is qualified with its library`() {
        val entries = listOf(
            ApkEntry.NativeLibrary(
                name = "/lib/arm64-v8a/libsample.so",
                downloadSize = 100,
                installSize = 100,
                units = listOf(ApkEntry.Default("[section .text]", 100, 100)),
            ),
        )

        assertThat(sanitizer.sanitize(entries).single().name)
            .isEqualTo("/lib/arm64-v8a/libsample.so/[section .text]")
    }

    @Test
    fun `a native library without compile units stays one file`() {
        val entries = listOf(
            ApkEntry.NativeLibrary("/lib/arm64-v8a/libsample.so", 100, 200, units = emptyList()),
        )

        val file = sanitizer.sanitize(entries).single()

        assertThat(file.type).isEqualTo(FileType.NATIVE_LIB)
        assertThat(file.installSize).isEqualTo(200)
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

    @Test
    fun `merges the resource tables of all apks`() {
        val entries = listOf(
            ApkEntry.Default("/resources.arsc", 10, 20),
            ApkEntry.Default("/resources.arsc", 30, 40),
        )

        val file = sanitizer.sanitize(entries).single()

        assertThat(file.downloadSize).isEqualTo(40)
        assertThat(file.installSize).isEqualTo(60)
    }

    @Test
    fun `resource type follows the de-obfuscated name`() {
        val deobfuscating = ApkSanitizer(
            ClassNameSanitizer(),
            ResourceNameSanitizer.fromMapping("res/drawable/logo.xml -> [res/raw/aB.xml]"),
        )
        val entries = listOf(ApkEntry.Default("/res/raw/aB.xml", 10, 20))

        val file = deobfuscating.sanitize(entries).single()

        assertThat(file.name).isEqualTo("/res/drawable/logo.xml")
        assertThat(file.resourceType).isEqualTo(ResourceType.DRAWABLE)
    }

    @Test
    fun `assigns a resource type to every resource directory`() {
        val entries = listOf(
            ApkEntry.Default("/res/layout/main.xml", 1, 1),
            ApkEntry.Default("/res/values/strings.xml", 1, 1),
            ApkEntry.Default("/res/font/roboto.ttf", 1, 1),
            ApkEntry.Default("/res/anim/fade.xml", 1, 1),
        )

        val types = sanitizer.sanitize(entries).associate { it.name to it.resourceType }

        assertThat(types["/res/layout/main.xml"]).isEqualTo(ResourceType.LAYOUT)
        assertThat(types["/res/values/strings.xml"]).isEqualTo(ResourceType.VALUES)
        assertThat(types["/res/font/roboto.ttf"]).isEqualTo(ResourceType.FONT)
        assertThat(types["/res/anim/fade.xml"]).isEqualTo(ResourceType.OTHER)
    }
}
