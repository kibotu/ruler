package com.kibotu.ruler.plugin

import com.google.common.truth.Truth.assertThat
import com.kibotu.ruler.analysis.dependency.DependencyEntry
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.result.ResolvedVariantResult
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.capabilities.Capability
import org.gradle.api.component.Artifact
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

class EntryParserTest {

    @Test
    fun `code artifacts are split into classes and other files`(@TempDir tempDir: File) {
        val jar = tempDir.resolve("lib.jar")
        JarOutputStream(jar.outputStream()).use { output ->
            output.putNextEntry(JarEntry("com/example/Foo.class"))
            output.write(byteArrayOf(1, 2, 3))
            output.closeEntry()
            output.putNextEntry(JarEntry("META-INF/services/Bar"))
            output.write(byteArrayOf(4))
            output.closeEntry()
        }

        val entries = extract(holdsCode = true, artifact(jar, "com.example:lib:1.0"))

        assertThat(entries.filterIsInstance<DependencyEntry.Class>().map { it.name })
            .containsExactly("com/example/Foo.class")
        assertThat(entries.filterIsInstance<DependencyEntry.Default>().map { it.name })
            .containsExactly("META-INF/services/Bar")
    }

    @Test
    fun `class directories are walked recursively`(@TempDir tempDir: File) {
        val classes = tempDir.resolve("classes")
        classes.resolve("com/example/Foo.class").apply {
            parentFile.mkdirs()
            writeBytes(byteArrayOf(1))
        }

        val entries = extract(holdsCode = true, artifact(classes, "project ':lib'"))

        assertThat(entries).containsExactly(
            DependencyEntry.Class("/com/example/Foo.class", "project ':lib'"),
        )
    }

    @Test
    fun `asset artifacts are never treated as code`(@TempDir tempDir: File) {
        val assets = tempDir.resolve("assets")
        assets.resolve("data.jar").apply {
            parentFile.mkdirs()
            writeBytes(byteArrayOf(1))
        }

        val entries = extract(holdsCode = false, artifact(assets, "project ':lib'"))

        assertThat(entries).containsExactly(
            DependencyEntry.Default("/data.jar", "project ':lib'"),
        )
    }

    @Test
    fun `entries carry the component that declares them`(@TempDir tempDir: File) {
        val resources = tempDir.resolve("res")
        resources.resolve("layout/main.xml").apply {
            parentFile.mkdirs()
            writeText("<View/>")
        }

        val entries = extract(holdsCode = false, artifact(resources, "androidx.core:core:1.0"))

        assertThat(entries.single().component).isEqualTo("androidx.core:core:1.0")
    }

    private fun extract(holdsCode: Boolean, vararg artifacts: ResolvedArtifactResult) =
        EntryParser.EntryExtractor(holdsCode).transform(artifacts.toList())

    private fun artifact(file: File, component: String) = object : ResolvedArtifactResult {
        override fun getFile(): File = file

        override fun getId(): ComponentArtifactIdentifier = object : ComponentArtifactIdentifier {
            override fun getComponentIdentifier(): ComponentIdentifier = ComponentIdentifier { component }
            override fun getDisplayName(): String = file.name
        }

        override fun getType(): Class<out Artifact> = Artifact::class.java

        // EntryParser reads only the file and the component identifier.
        override fun getVariant(): ResolvedVariantResult = throw UnsupportedOperationException()
        override fun getAttributes(): AttributeContainer = throw UnsupportedOperationException()
        override fun getCapabilities(): Collection<Capability> = throw UnsupportedOperationException()
    }
}
