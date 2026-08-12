package com.kibotu.ruler.analysis.sanitizer

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ResourceNameSanitizerTest {
    @Test
    fun `deobfuscates resource paths from mapping`() {
        val mapping = """
            res/drawable/foo.xml -> [res/raw/dVo.xml]
            res/layout/main.xml -> [res/layout/aBc.xml]
        """.trimIndent()
        val sanitizer = ResourceNameSanitizer.fromMapping(mapping)

        assertThat(sanitizer.sanitize("/res/raw/dVo.xml")).isEqualTo("/res/drawable/foo.xml")
        assertThat(sanitizer.sanitize("/res/layout/aBc.xml")).isEqualTo("/res/layout/main.xml")
    }

    @Test
    fun `returns original name when mapping is missing`() {
        val sanitizer = ResourceNameSanitizer.fromMapping("")

        assertThat(sanitizer.sanitize("/res/raw/unknown.xml")).isEqualTo("/res/raw/unknown.xml")
    }

    @Test
    fun `ignores non resource mapping lines`() {
        val mapping = """
            # comment
            com.example.Foo -> com.example.a:
                1:1:void bar():1:1 -> bar
            res/anim/spin.xml -> [res/anim/xYz.xml]
        """.trimIndent()
        val sanitizer = ResourceNameSanitizer.fromMapping(mapping)

        assertThat(sanitizer.sanitize("/res/anim/xYz.xml")).isEqualTo("/res/anim/spin.xml")
    }
}
