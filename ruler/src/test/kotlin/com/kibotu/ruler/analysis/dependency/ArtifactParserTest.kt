package com.kibotu.ruler.analysis.dependency

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

class ArtifactParserTest {
    @Test
    fun `jar parser extracts class entries`(@TempDir tempDir: File) {
        val jar = tempDir.resolve("lib.jar")
        JarOutputStream(jar.outputStream()).use { output ->
            output.putNextEntry(JarEntry("com/example/Foo.class"))
            output.write(byteArrayOf(1, 2, 3))
            output.closeEntry()
            output.putNextEntry(JarEntry("META-INF/MANIFEST.MF"))
            output.write("Manifest-Version: 1.0\n".toByteArray())
            output.closeEntry()
        }

        val entries = JarArtifactParser().parseFile(
            ArtifactResult.JarArtifact(jar, "com.example:lib:1.0"),
        )

        assertThat(entries).hasSize(2)
        assertThat(entries.filterIsInstance<DependencyEntry.Class>().map { it.name })
            .containsExactly("com/example/Foo.class")
        assertThat(entries.filterIsInstance<DependencyEntry.Default>().map { it.name })
            .containsExactly("META-INF/MANIFEST.MF")
    }

    @Test
    fun `class parser returns class entry with relative path`(@TempDir tempDir: File) {
        val root = tempDir.resolve("classes")
        val classFile = root.resolve("com/example/Foo.class").apply {
            parentFile.mkdirs()
            writeBytes(byteArrayOf(1, 2, 3))
        }

        val entries = ClassArtifactParser().parseFile(
            ArtifactResult.ClassArtifact(classFile, root, "project ':app'"),
        )

        assertThat(entries).containsExactly(
            DependencyEntry.Class("/com/example/Foo.class", "project ':app'"),
        )
    }

    @Test
    fun `default parser returns default entry with relative path`(@TempDir tempDir: File) {
        val root = tempDir.resolve("assets")
        val assetFile = root.resolve("license.html").apply {
            parentFile.mkdirs()
            writeText("<html/>")
        }

        val entries = DefaultArtifactParser().parseFile(
            ArtifactResult.DefaultArtifact(assetFile, root, "project ':lib'"),
        )

        assertThat(entries).containsExactly(
            DependencyEntry.Default("/license.html", "project ':lib'"),
        )
    }

    @Test
    fun `dependency parser dispatches artifact types`(@TempDir tempDir: File) {
        val jar = tempDir.resolve("lib.jar")
        JarOutputStream(jar.outputStream()).use { output ->
            output.putNextEntry(JarEntry("com/example/Bar.class"))
            output.write(byteArrayOf(4, 5, 6))
            output.closeEntry()
        }

        val entries = DependencyParser().parse(
            listOf(ArtifactResult.JarArtifact(jar, "com.example:lib:1.0")),
        )

        assertThat(entries.single()).isEqualTo(
            DependencyEntry.Class("com/example/Bar.class", "com.example:lib:1.0"),
        )
    }
}
