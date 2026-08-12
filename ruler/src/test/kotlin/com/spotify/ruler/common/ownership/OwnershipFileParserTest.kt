package com.spotify.ruler.common.ownership

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File

class OwnershipFileParserTest {
    private val parser = OwnershipFileParser()

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `parses basic ownership entries`() {
        val yaml = """
            - identifier: :app
              owner: app-team
            - identifier: :lib
              owner: lib-team
        """.trimIndent()
        val file = tempDir.resolve("ownership.yaml").apply { writeText(yaml) }

        val entries = parser.parse(file)

        assertThat(entries).hasSize(2)
        assertThat(entries[0].identifier).isEqualTo(":app")
        assertThat(entries[0].owner).isEqualTo("app-team")
        assertThat(entries[0].internal).isNull()
        assertThat(entries[1].identifier).isEqualTo(":lib")
        assertThat(entries[1].owner).isEqualTo("lib-team")
    }

    @Test
    fun `parses internal flag as boolean`() {
        val yaml = """
            - identifier: com.mycompany.*
              owner: core-team
              internal: true
            - identifier: com.external.*
              owner: third-party
              internal: false
        """.trimIndent()
        val file = tempDir.resolve("ownership.yaml").apply { writeText(yaml) }

        val entries = parser.parse(file)

        assertThat(entries).hasSize(2)
        assertThat(entries[0].internal).isTrue()
        assertThat(entries[1].internal).isFalse()
    }

    @Test
    fun `parses internal flag as string`() {
        val yaml = """
            - identifier: com.mycompany.*
              owner: core-team
              internal: "true"
            - identifier: com.external.*
              owner: third-party
              internal: "false"
        """.trimIndent()
        val file = tempDir.resolve("ownership.yaml").apply { writeText(yaml) }

        val entries = parser.parse(file)

        assertThat(entries[0].internal).isTrue()
        assertThat(entries[1].internal).isFalse()
    }

    @Test
    fun `throws on missing identifier`() {
        val yaml = """
            - owner: app-team
        """.trimIndent()
        val file = tempDir.resolve("ownership.yaml").apply { writeText(yaml) }

        val exception = assertThrows<IllegalStateException> { parser.parse(file) }
        assertThat(exception.message).contains("Could not parse ownership file")
    }

    @Test
    fun `throws on missing owner`() {
        val yaml = """
            - identifier: :app
        """.trimIndent()
        val file = tempDir.resolve("ownership.yaml").apply { writeText(yaml) }

        val exception = assertThrows<IllegalStateException> { parser.parse(file) }
        assertThat(exception.message).contains("Could not parse ownership file")
    }

    @Test
    fun `preserves entry order`() {
        val yaml = """
            - identifier: first
              owner: team-1
            - identifier: second
              owner: team-2
            - identifier: third
              owner: team-3
        """.trimIndent()
        val file = tempDir.resolve("ownership.yaml").apply { writeText(yaml) }

        val entries = parser.parse(file)

        assertThat(entries.map { it.identifier }).containsExactly("first", "second", "third").inOrder()
    }
}
