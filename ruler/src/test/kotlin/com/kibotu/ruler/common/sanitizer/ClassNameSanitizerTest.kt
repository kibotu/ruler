package com.kibotu.ruler.common.sanitizer

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ClassNameSanitizerTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `deobfuscates class names from mapping file`() {
        val mapping = """
            com.example.Public -> com.example.Obfuscated:
                1:1:void foo():1:1 -> foo
        """.trimIndent()
        val mappingFile = tempDir.resolve("mapping.txt").apply { writeText(mapping) }
        val sanitizer = ClassNameSanitizer(mappingFile)

        assertThat(sanitizer.sanitize("com.example.Obfuscated"))
            .isEqualTo("com.example.Public")
    }

    @Test
    fun `normalizes dex class descriptors`() {
        val sanitizer = ClassNameSanitizer(null as File?)

        assertThat(sanitizer.sanitize("Lcom/example/Foo;"))
            .isEqualTo("com.example.Foo")
    }
}
