package com.kibotu.ruler.analysis.proguard

import com.google.common.truth.Truth.assertThat
import com.kibotu.ruler.model.AppFile
import com.kibotu.ruler.model.FileType
import com.kibotu.ruler.model.Pinned
import com.kibotu.ruler.model.ProguardRuleFile
import com.kibotu.ruler.model.RuleFlag
import com.kibotu.ruler.model.RuleMatch
import com.kibotu.ruler.model.RuleScope
import org.junit.jupiter.api.Test

class ProguardAnalyzerTest {
    private val analyzer = ProguardAnalyzer()

    private val files = mapOf(
        ":lib" to listOf(cls("com.example.lib.Foo", 10), cls("com.example.lib.Bar", 30)),
        "com.example:core:1.0" to listOf(cls("com.example.core.Core", 5), cls("com.example.core.More", 15)),
        "com.example:extra:1.0" to listOf(cls("com.example.extra.Even", 1), cls("com.example.extra.More", 2)),
        ":app" to listOf(cls("com.example.app.Main", 7)),
    )

    @Test
    fun `rules of another component reach a component's classes`() {
        val rules = mapOf("com.example:core:1.0" to listOf(rule("-keep class com.example.lib.** { *; }")))

        val analysis = analyzer.analyze(files, rules)

        val lib = analysis.reach.getValue(":lib")
        assertThat(lib.classes).isEqualTo(2)
        assertThat(lib.bytes).isEqualTo(40)
        assertThat(lib.ownKept.classes).isEqualTo(0)
        assertThat(lib.othersKept.bytes).isEqualTo(40)
        assertThat(lib.affectedBy.single().source).isEqualTo("com.example:core:1.0")
        assertThat(lib.affectedBy.single().rules).containsExactly(RuleMatch("-keep class com.example.lib.**", 2, 40))
        // The owner is untouched by its own rule: nothing outside it matched.
        assertThat(analysis.reach).doesNotContainKey("com.example:core:1.0")
    }

    @Test
    fun `own keeps are not credited to others`() {
        val rules = mapOf(
            ":lib" to listOf(rule("-keep class com.example.lib.Foo")),
            "com.example:core:1.0" to listOf(rule("-keep class com.example.lib.Foo"), rule("-keep class com.example.lib.Bar")),
        )

        val lib = analyzer.analyze(files, rules).reach.getValue(":lib")

        assertThat(lib.ownKept.classes).isEqualTo(1)
        assertThat(lib.othersKept.classes).isEqualTo(1)
        val core = lib.affectedBy.single()
        assertThat(core.kept).isEqualTo(Pinned(2, 40))
        // :lib keeps Foo itself, so dropping core's rules would only let Bar go.
        assertThat(core.onlyTheirs).isEqualTo(Pinned(1, 30))
    }

    @Test
    fun `only what no one else keeps is theirs alone`() {
        val rules = mapOf(
            "com.example:core:1.0" to listOf(rule("-keep class com.example.lib.** { *; }")),
            "com.example:extra:1.0" to listOf(rule("-keep class com.example.lib.Foo")),
        )

        val affectedBy = analyzer.analyze(files, rules).reach.getValue(":lib").affectedBy

        assertThat(affectedBy.map { Triple(it.source, it.kept.bytes, it.onlyTheirs.bytes) }).containsExactly(
            Triple("com.example:core:1.0", 40L, 30L),
            Triple("com.example:extra:1.0", 10L, 0L),
        ).inOrder()
    }

    @Test
    fun `each directive carries what it keeps, the most first`() {
        val rules = mapOf(
            "com.example:core:1.0" to listOf(
                rule("-keep class com.example.lib.Foo\n-assumenosideeffects class com.example.lib.** { *; }\n-keep class com.example.lib.Bar"),
            ),
        )

        val impact = analyzer.analyze(files, rules).reach.getValue(":lib").affectedBy.single()

        assertThat(impact.classes).isEqualTo(2)
        assertThat(impact.rules).containsExactly(
            RuleMatch("-keep class com.example.lib.Bar", 1, 30),
            RuleMatch("-keep class com.example.lib.Foo", 1, 10),
            RuleMatch("-assumenosideeffects class com.example.lib.**", 2, 0),
        ).inOrder()
    }

    @Test
    fun `keeping members or names keeps no class`() {
        val rules = mapOf(
            "com.example:core:1.0" to listOf(
                rule("-keepclassmembers class com.example.lib.** { *; }\n-keepnames class com.example.lib.Foo"),
            ),
        )

        val analysis = analyzer.analyze(files, rules)

        val impact = analysis.reach.getValue(":lib").affectedBy.single()
        assertThat(impact.kept).isEqualTo(Pinned(0, 0))
        assertThat(impact.rules.map(RuleMatch::bytes)).containsExactly(0L, 0L)
        // They still act on code core does not own.
        assertThat(analysis.rules.getValue("com.example:core:1.0").single().flags!!.map { it.scope })
            .containsExactly(RuleScope.CROSS_DEPENDENCY, RuleScope.CROSS_DEPENDENCY)
    }

    @Test
    fun `the app's rules reach components, but its classes are no target`() {
        val rules = mapOf(
            ":app" to listOf(rule("-keep class com.example.lib.**")),
            "com.example:core:1.0" to listOf(rule("-keep class com.example.app.**")),
        )

        val analysis = analyzer.analyze(files, rules, app = ":app")

        assertThat(analysis.reach.keys).containsExactly(":lib")
        assertThat(analysis.reach.getValue(":lib").affectedBy.single().source).isEqualTo(":app")
        // Keeping libraries is what the app's rules are for, so they are no cross dependency, and
        // a library keeping the app's classes is left to the app.
        assertThat(analysis.rules.getValue(":app").single().flags).isNull()
        assertThat(analysis.rules.getValue("com.example:core:1.0").single().flags).isNull()
    }

    @Test
    fun `rules that match by hierarchy or every class are not attributed`() {
        val rules = mapOf(
            "com.example:core:1.0" to listOf(
                rule("-keep class * extends com.example.lib.Foo"),
                rule("-keep class ** { *; }"),
            ),
        )

        val analysis = analyzer.analyze(files, rules)

        assertThat(analysis.reach).isEmpty()
        assertThat(analysis.rules.getValue("com.example:core:1.0").flatMap { it.flags.orEmpty() }).isEmpty()
    }

    @Test
    fun `assumenosideeffects reaches classes without pinning them`() {
        val rules = mapOf("com.example:core:1.0" to listOf(rule("-assumenosideeffects class com.example.lib.Foo { *; }")))

        val analysis = analyzer.analyze(files, rules)

        // It rewrites that class, so it reaches it, but it keeps nothing, so nobody is bigger for it.
        val lib = analysis.reach.getValue(":lib")
        assertThat(lib.othersKept.classes).isEqualTo(0)
        assertThat(lib.affectedBy.single().classes).isEqualTo(1)
        assertThat(lib.affectedBy.single().kept).isEqualTo(Pinned(0, 0))
        assertThat(analysis.rules.getValue("com.example:core:1.0").single().flags!!.single().scope)
            .isEqualTo(RuleScope.CROSS_DEPENDENCY)
    }

    @Test
    fun `a keep rule that pins another component's classes is a cross dependency`() {
        val rules = mapOf("com.example:core:1.0" to listOf(rule("-keep class com.example.lib.** { *; }")))

        val flag = analyzer.analyze(files, rules).rules.getValue("com.example:core:1.0").single().flags!!.single()

        assertThat(flag.line).isEqualTo(1)
        assertThat(flag.directive).isEqualTo("-keep class com.example.lib.**")
        assertThat(flag.scope).isEqualTo(RuleScope.CROSS_DEPENDENCY)
        assertThat(flag.targets).containsExactly(":lib")
    }

    @Test
    fun `a cross dependency names what it costs most first, and never itself`() {
        val rules = mapOf(":lib" to listOf(rule("-keep class com.example.** { *; }")))

        val flag = analyzer.analyze(files, rules, app = ":app").rules.getValue(":lib").single().flags!!.single()

        // :lib keeps its own classes with the same directive, which is not a cross dependency.
        assertThat(flag.targets).containsExactly("com.example:core:1.0", "com.example:extra:1.0").inOrder()
    }

    @Test
    fun `a keep rule inside its own package is a cross dependency when another component owns what it pins`() {
        val nested = mapOf(
            ":feature" to listOf(cls("com.example.feature.Api", 10)),
            ":core" to listOf(cls("com.example.feature.deep.Internal", 40)),
        )
        val rules = mapOf(":feature" to listOf(rule("-keep class com.example.** { *; }")))

        val flag = analyzer.analyze(nested, rules).rules.getValue(":feature").single().flags!!.single()

        // The directive names the shipping component's own package, so only the app itself can show
        // that :core owns the classes it pins.
        assertThat(flag.scope).isEqualTo(RuleScope.CROSS_DEPENDENCY)
        assertThat(flag.targets).containsExactly(":core")
    }

    @Test
    fun `a cross dependency replaces the flag the rule file guessed at`() {
        val guessed = ProguardRuleFile(
            origin = "Consumer rules",
            name = "proguard.txt",
            content = "-keep class com.example.lib.** { *; }\n-keep class com.example.other.** { *; }",
            ruleCount = 2,
            namespace = "com.example.core",
            flags = listOf(
                RuleFlag(1, "-keep class com.example.lib.**", RuleScope.OUTSIDE_NAMESPACE),
                RuleFlag(2, "-keep class com.example.other.**", RuleScope.OUTSIDE_NAMESPACE),
            ),
        )

        val flags = analyzer.analyze(files, mapOf("com.example:core:1.0" to listOf(guessed)))
            .rules.getValue("com.example:core:1.0").single().flags!!

        // What the app proves takes the line over instead of adding a second flag for the directive.
        assertThat(flags.map { it.line to it.scope }).containsExactly(
            1 to RuleScope.CROSS_DEPENDENCY,
            2 to RuleScope.NO_MATCH,
        ).inOrder()
        assertThat(flags.first().targets).containsExactly(":lib")
    }

    @Test
    fun `only the file that carries the directive is flagged`() {
        val rules = mapOf(
            "com.example:core:1.0" to listOf(
                rule("-keep class com.example.core.** { *; }"),
                rule("-keep class com.example.lib.Bar"),
            ),
        )

        val ofCore = analyzer.analyze(files, rules).rules.getValue("com.example:core:1.0")

        assertThat(ofCore.first().flags).isNull()
        assertThat(ofCore.last().flags!!.single().targets).containsExactly(":lib")
    }

    @Test
    fun `a component that ships only rules is named by what it pins`() {
        val rules = mapOf("com.example:wrapper:1.0" to listOf(rule("-keep class com.example.core.** { *; }")))

        val analysis = analyzer.analyze(files, rules)

        assertThat(analysis.rules.getValue("com.example:wrapper:1.0").single().flags!!.single().targets)
            .containsExactly("com.example:core:1.0")
        // It contributes no classes of its own, so it has no reach of its own to report.
        assertThat(analysis.reach).doesNotContainKey("com.example:wrapper:1.0")
    }

    @Test
    fun `a keep rule that matches no class of the app does nothing`() {
        val rules = mapOf(
            "com.example:core:1.0" to listOf(
                rule(
                    """
                    -keep class com.gone.** { *; }
                    -keepnames class com.gone.Names
                    -if class com.example.core.Core
                    -keep class com.gone.Maybe
                    -keep class com.example.feature.**
                    """.trimIndent(),
                ),
            ),
            ":app" to listOf(rule("-keep class com.gone.Legacy")),
        )

        val analysis = analyzer.analyze(files, rules, app = ":app", features = listOf(cls("com.example.feature.Screen", 3)))

        // Keeping names keeps no class, the -if may not hold, and a feature's classes count as the app's.
        assertThat(analysis.rules.getValue("com.example:core:1.0").single().flags!!.map { it.line to it.scope })
            .containsExactly(1 to RuleScope.NO_MATCH)
        assertThat(analysis.rules.getValue(":app").single().flags!!.single().scope).isEqualTo(RuleScope.NO_MATCH)
    }

    private fun cls(name: String, size: Long) = AppFile(name, FileType.CLASS, size, size)

    private fun rule(content: String) = ProguardRuleFile("Consumer rules", "proguard.txt", content, 1)
}
