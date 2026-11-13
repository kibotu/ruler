package com.kibotu.ruler.analysis.proguard

import com.google.common.truth.Truth.assertThat
import com.kibotu.ruler.model.RuleScope
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ProguardRuleExtractorTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `reads the merged consumer rules of an AAR`() {
        val aar = tempDir.resolve("lib.aar")
        ZipOutputStream(aar.outputStream()).use { output -> output.writeProguardTxt(KEEP_ONE) }

        val rules = ProguardRuleExtractor().parse(listOf(RuleArchive("com.example:lib:1.0", aar)))

        assertThat(rules).hasSize(1)
        val file = rules.values.single().single()
        assertThat(file.origin).isEqualTo("Consumer rules")
        assertThat(file.name).isEqualTo("proguard.txt")
        assertThat(file.content).isEqualTo(KEEP_ONE.trim())
        assertThat(file.ruleCount).isEqualTo(1)
    }

    @Test
    fun `reads targeted R8 rules from inside the AAR's classes jar`() {
        val aar = tempDir.resolve("lib.aar")
        ZipOutputStream(aar.outputStream()).use { output ->
            output.writeProguardTxt("")
            output.writeClassesJar(
                "META-INF/com.android.tools/r8-from-8.0.0-upto-8.2.0/rules.pro" to KEEP_ONE,
                "META-INF/com.android.tools/r8/rules.pro" to DONT_WARN,
            )
        }

        val rules = ProguardRuleExtractor().parse(listOf(RuleArchive("com.example:lib:1.0", aar)))
            .values.single()

        assertThat(rules.map { it.origin }).containsExactly(
            "Targeted R8 rules (r8-from 8.0.0 upto 8.2.0)",
            "Targeted R8 rules (r8)",
        )
        assertThat(rules.map { it.name }).containsExactly(
            "META-INF/com.android.tools/r8-from-8.0.0-upto-8.2.0/rules.pro",
            "META-INF/com.android.tools/r8/rules.pro",
        )
    }

    @Test
    fun `reads consumer and targeted R8 rules of a jar`() {
        val jar = tempDir.resolve("lib.jar")
        ZipOutputStream(jar.outputStream()).use { output ->
            output.put("META-INF/proguard/consumer.pro", KEEP_ONE)
            output.put("META-INF/com.android.tools/r8-upto-8.0.0/rules.pro", DONT_WARN)
        }

        val rules = ProguardRuleExtractor().parse(listOf(RuleArchive("com.example:lib:1.0", jar)))
            .values.single()

        assertThat(rules.map { it.origin }).containsExactly(
            "Consumer rules",
            "Targeted R8 rules (r8-upto 8.0.0)",
        )
    }

    @Test
    fun `counts only directives, not comments or blank lines`() {
        val content = """
            # a comment
            -keep class com.example.** { *; }
            # another comment
            -dontwarn java.lang.**

        """.trimIndent() + "\n"
        val jar = tempDir.resolve("lib.jar")
        ZipOutputStream(jar.outputStream()).use { it.put("META-INF/proguard/rules.pro", content) }

        val file = ProguardRuleExtractor().parse(listOf(RuleArchive("com.example:lib:1.0", jar)))
            .values.single().single()

        assertThat(file.ruleCount).isEqualTo(2)
    }

    @Test
    fun `project components keep their name and modules lose the project prefix`() {
        val jar = tempDir.resolve("lib.jar")
        ZipOutputStream(jar.outputStream()).use { it.put("META-INF/proguard/rules.pro", KEEP_ONE) }
        val extractor = ProguardRuleExtractor()

        val projects = extractor.parse(listOf(RuleArchive("project ':sample:lib'", jar)))
        val modules = extractor.parse(listOf(RuleArchive("com.example:lib:1.0", jar)))

        assertThat(projects.keys).containsExactly(":sample:lib")
        assertThat(modules.keys).containsExactly("com.example:lib:1.0")
    }

    @Test
    fun `archives without rule files report nothing`() {
        val jar = tempDir.resolve("lib.jar")
        ZipOutputStream(jar.outputStream()).use { output ->
            output.put("com/example/Foo.class", byteArrayOf(1))
            output.put("META-INF/MANIFEST.MF", "manifest".toByteArray())
        }

        val rules = ProguardRuleExtractor().parse(listOf(RuleArchive("com.example:lib:1.0", jar)))

        assertThat(rules).isEmpty()
    }

    @Test
    fun `non archives and directories are ignored`() {
        val classes = tempDir.resolve("classes").apply { mkdirs() }
        val extractor = ProguardRuleExtractor()

        val rules = extractor.parse(listOf(RuleArchive("project ':lib'", classes)))

        assertThat(rules).isEmpty()
    }

    @Test
    fun `files are sorted by directive count within a component`() {
        val jar = tempDir.resolve("lib.jar")
        ZipOutputStream(jar.outputStream()).use { output ->
            output.put("META-INF/proguard/one.pro", KEEP_ONE)
            output.put("META-INF/proguard/two.pro", "$KEEP_ONE\n$DONT_WARN")
        }

        val names = ProguardRuleExtractor().parse(listOf(RuleArchive("com.example:lib:1.0", jar)))
            .values.single().map { it.name }

        assertThat(names).containsExactly("META-INF/proguard/two.pro", "META-INF/proguard/one.pro").inOrder()
    }

    @Test
    fun `flags rules that name another package than the component's own`() {
        val aar = tempDir.resolve("lib.aar")
        val rules = """
            -keep class com.example.lib.** { *; }
            -keep class com.example.** { *; }
            -keep class * extends android.app.Activity
            -keep class ** { *; }
            -keep class android.util.Log
            -keepclasseswithmembers class * { native <methods>; }
        """.trimIndent()
        ZipOutputStream(aar.outputStream()).use { output ->
            output.writeProguardTxt(rules)
            output.writeClassesJar("com/example/lib/Foo.class" to "x", "com/example/lib/sub/Bar.class" to "x")
        }

        val file = ProguardRuleExtractor().parse(listOf(RuleArchive("com.example:lib:1.0", aar))).values.single().single()

        assertThat(file.namespace).isEqualTo("com.example.lib")
        assertThat(file.flags!!.map { it.line to it.scope }).containsExactly(
            2 to RuleScope.OUTSIDE_NAMESPACE,
            3 to RuleScope.CROSS_CUTTING,
            4 to RuleScope.ALL_CLASSES,
            5 to RuleScope.OUTSIDE_NAMESPACE,
            // Only matches classes with the member, so it does not pin every class.
            6 to RuleScope.CROSS_CUTTING,
        ).inOrder()
    }

    @Test
    fun `archives without classes have no namespace to flag against`() {
        val aar = tempDir.resolve("lib.aar")
        ZipOutputStream(aar.outputStream()).use { it.writeProguardTxt("-keep class com.other.**") }

        val file = ProguardRuleExtractor().parse(listOf(RuleArchive("com.example:lib:1.0", aar))).values.single().single()

        assertThat(file.namespace).isNull()
        assertThat(file.flags).isNull()
    }

    private fun ZipOutputStream.writeProguardTxt(content: String) = put("proguard.txt", content)

    private fun ZipOutputStream.writeClassesJar(vararg entries: Pair<String, String>) {
        val classesJar = ByteArrayOutputStream().use { bytes ->
            ZipOutputStream(bytes).use { jar -> entries.forEach { (name, content) -> jar.put(name, content) } }
            bytes.toByteArray()
        }
        put("classes.jar", classesJar)
    }

    private fun ZipOutputStream.put(name: String, content: String) = put(name, content.toByteArray())

    private fun ZipOutputStream.put(name: String, bytes: ByteArray) {
        putNextEntry(ZipEntry(name))
        write(bytes)
        closeEntry()
    }

    private companion object {
        private const val KEEP_ONE = "-keep class com.example.** { *; }\n"
        private const val DONT_WARN = "-dontwarn java.lang.**\n"
    }
}
