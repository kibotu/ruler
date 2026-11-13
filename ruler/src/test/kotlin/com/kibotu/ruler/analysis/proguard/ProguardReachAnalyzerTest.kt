package com.kibotu.ruler.analysis.proguard

import com.google.common.truth.Truth.assertThat
import com.kibotu.ruler.model.AppFile
import com.kibotu.ruler.model.FileType
import com.kibotu.ruler.model.ProguardRuleFile
import org.junit.jupiter.api.Test

class ProguardReachAnalyzerTest {
    private val analyzer = ProguardReachAnalyzer()

    private val files = mapOf(
        ":lib" to listOf(cls("com.example.lib.Foo", 10), cls("com.example.lib.Bar", 30)),
        "com.example:core:1.0" to listOf(cls("com.example.core.Core", 5)),
        ":app" to listOf(cls("com.example.app.Main", 7)),
    )

    @Test
    fun `rules of another component reach a component's classes`() {
        val rules = mapOf("com.example:core:1.0" to listOf(rule("-keep class com.example.lib.** { *; }")))

        val reach = analyzer.analyze(files, rules)

        val lib = reach.getValue(":lib")
        assertThat(lib.classes).isEqualTo(2)
        assertThat(lib.bytes).isEqualTo(40)
        assertThat(lib.ownKept.classes).isEqualTo(0)
        assertThat(lib.othersKept.bytes).isEqualTo(40)
        assertThat(lib.affectedBy.single().source).isEqualTo("com.example:core:1.0")
        assertThat(lib.affectedBy.single().rules).containsExactly("-keep class com.example.lib.**")
        // The owner is untouched by its own rule: nothing outside it matched.
        assertThat(reach).doesNotContainKey("com.example:core:1.0")
    }

    @Test
    fun `own keeps are not credited to others`() {
        val rules = mapOf(
            ":lib" to listOf(rule("-keep class com.example.lib.Foo")),
            "com.example:core:1.0" to listOf(rule("-keep class com.example.lib.Foo"), rule("-keep class com.example.lib.Bar")),
        )

        val lib = analyzer.analyze(files, rules).getValue(":lib")

        assertThat(lib.ownKept.classes).isEqualTo(1)
        assertThat(lib.othersKept.classes).isEqualTo(1)
        assertThat(lib.affectedBy.single().classes).isEqualTo(2)
    }

    @Test
    fun `the app is neither source nor target`() {
        val rules = mapOf(
            ":app" to listOf(rule("-keep class com.example.lib.**")),
            "com.example:core:1.0" to listOf(rule("-keep class com.example.app.**")),
        )

        assertThat(analyzer.analyze(files, rules, ignored = ":app")).isEmpty()
    }

    @Test
    fun `rules that match by hierarchy or every class are not attributed`() {
        val rules = mapOf(
            "com.example:core:1.0" to listOf(
                rule("-keep class * extends com.example.lib.Foo"),
                rule("-keep class ** { *; }"),
            ),
        )

        assertThat(analyzer.analyze(files, rules)).isEmpty()
    }

    @Test
    fun `assumenosideeffects reaches classes without pinning them`() {
        val rules = mapOf("com.example:core:1.0" to listOf(rule("-assumenosideeffects class com.example.lib.Foo { *; }")))

        val lib = analyzer.analyze(files, rules).getValue(":lib")

        assertThat(lib.othersKept.classes).isEqualTo(0)
        assertThat(lib.affectedBy.single().classes).isEqualTo(1)
    }

    private fun cls(name: String, size: Long) = AppFile(name, FileType.CLASS, size, size)

    private fun rule(content: String) = ProguardRuleFile("Consumer rules", "proguard.txt", content, 1)
}
