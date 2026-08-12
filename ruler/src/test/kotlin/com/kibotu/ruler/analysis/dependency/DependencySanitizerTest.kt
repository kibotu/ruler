package com.kibotu.ruler.analysis.dependency

import com.google.common.truth.Truth.assertThat
import com.kibotu.ruler.analysis.sanitizer.ClassNameSanitizer
import com.kibotu.ruler.model.ComponentType
import org.junit.jupiter.api.Test

class DependencySanitizerTest {
    private val sanitizer = DependencySanitizer(ClassNameSanitizer())

    @Test
    fun `Gradle project dependencies are internal after displayName normalization`() {
        val result = sanitizer.sanitize(
            listOf(
                DependencyEntry.Class(
                    name = "com.kibotu.ruler.sample.lib.LibActivity",
                    component = "project ':sample:lib'",
                ),
            ),
        )

        val components = result.values.single()
        assertThat(components).hasSize(1)
        assertThat(components[0].name).isEqualTo(":sample:lib")
        assertThat(components[0].type).isEqualTo(ComponentType.INTERNAL)
    }

    @Test
    fun `Maven dependencies remain external`() {
        val result = sanitizer.sanitize(
            listOf(
                DependencyEntry.Class(
                    name = "androidx.constraintlayout.widget.ConstraintLayout",
                    component = "androidx.constraintlayout:constraintlayout:2.1.4",
                ),
            ),
        )

        val components = result.values.single()
        assertThat(components[0].type).isEqualTo(ComponentType.EXTERNAL)
    }
}
