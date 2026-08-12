package com.kibotu.ruler.plugin

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.Properties
import java.util.zip.ZipFile

class RulerPluginTest {
    private lateinit var pluginClasspath: List<File>

    @BeforeEach
    fun setUp() {
        pluginClasspath = System.getProperty("pluginClasspath")
            ?.split(File.pathSeparator)
            ?.map(::File)
            ?: error("pluginClasspath system property is not set")
    }

    @Test
    fun `plugin metadata registers net kibotu ruler id`() {
        val entryName = "META-INF/gradle-plugins/net.kibotu.ruler.properties"
        val properties = Properties()

        pluginClasspath.forEach { file ->
            when {
                file.isDirectory -> {
                    val descriptor = file.resolve(entryName)
                    if (descriptor.exists()) {
                        properties.load(descriptor.inputStream())
                    }
                }
                file.extension == "jar" -> {
                    ZipFile(file).use { zip ->
                        zip.getEntry(entryName)?.let { entry ->
                            properties.load(zip.getInputStream(entry))
                        }
                    }
                }
            }
        }

        assertThat(properties.getProperty("implementation-class"))
            .isEqualTo("com.kibotu.ruler.plugin.RulerPlugin")
    }
}
