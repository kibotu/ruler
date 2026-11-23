package com.kibotu.ruler.analysis.proguard

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class RuleParserTest {

    @Test
    fun `reads keep directives with their line and patterns`() {
        val rules = RuleParser.parse(
            """
            # comment
            -keep,allowobfuscation class com.a.**, com.b.Foo { *; }
            """.trimIndent(),
        )

        val rule = rules.single()
        assertThat(rule.line).isEqualTo(2)
        assertThat(rule.pins).isTrue()
        assertThat(rule.patterns).containsExactly("com.a.**", "com.b.Foo").inOrder()
        assertThat(rule.constrained).isFalse()
    }

    @Test
    fun `member blocks may span lines`() {
        val rules = RuleParser.parse("-keepclassmembers class com.a.Foo {\n  <init>();\n}\n-keep class com.b.Bar")

        assertThat(rules.map { it.patterns.single() }).containsExactly("com.a.Foo", "com.b.Bar").inOrder()
        assertThat(rules[1].line).isEqualTo(4)
    }

    @Test
    fun `hierarchy and annotations constrain a rule`() {
        val rules = RuleParser.parse(
            """
            -keep class * extends androidx.fragment.app.Fragment
            -keep @androidx.annotation.Keep class * { *; }
            """.trimIndent(),
        )

        assertThat(rules.map { it.constrained }).containsExactly(true, true)
        assertThat(rules.map { it.patterns.single() }).containsExactly("*", "*")
    }

    @Test
    fun `negated patterns and options without a class are dropped`() {
        val rules = RuleParser.parse("-dontwarn com.a.**\n-keep class !com.a.Foo\n-optimizationpasses 5")

        assertThat(rules).isEmpty()
    }

    @Test
    fun `assumenosideeffects rewrites but does not keep`() {
        val rule = RuleParser.parse("-assumenosideeffects class android.util.Log { *; }").single()

        assertThat(rule.pins).isFalse()
    }

    @Test
    fun `only keeps that may not shrink a class pin it`() {
        val rules = RuleParser.parse(
            """
            -keep class com.a.A
            -keepclasseswithmembers class com.a.B { <init>(); }
            -keep,allowshrinking class com.a.C
            -keepnames class com.a.D
            -keepclassmembers class com.a.E { *; }
            -keepclassmembernames class com.a.F { *; }
            -keepclasseswithmembernames class com.a.G { native <methods>; }
            """.trimIndent(),
        )

        assertThat(rules.map { it.pins }).containsExactly(true, true, false, false, false, false, false).inOrder()
    }

    @Test
    fun `a directive after -if is conditional`() {
        val rules = RuleParser.parse("-if class com.a.Foo\n-keep class com.a.Bar\n-keep class com.a.Baz")

        assertThat(rules.map { it.patterns.single() to it.conditional })
            .containsExactly("com.a.Bar" to true, "com.a.Baz" to false).inOrder()
    }

    @Test
    fun `wildcards follow proguard semantics`() {
        assertThat(RuleParser.regexOf("com.a.*").matches("com.a.Foo")).isTrue()
        assertThat(RuleParser.regexOf("com.a.*").matches("com.a.b.Foo")).isFalse()
        assertThat(RuleParser.regexOf("com.a.**").matches("com.a.b.Foo")).isTrue()
        assertThat(RuleParser.regexOf("com.a.Foo").matches("com.a.Foo\$Inner")).isFalse()
        assertThat(RuleParser.regexOf("com.a.Foo?").matches("com.a.Foo1")).isTrue()
        assertThat(RuleParser.literalPrefix("com.a.**")).isEqualTo("com.a.")
    }
}
