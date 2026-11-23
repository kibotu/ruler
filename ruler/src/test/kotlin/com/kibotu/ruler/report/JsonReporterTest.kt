package com.kibotu.ruler.report

import com.google.common.truth.Truth.assertThat
import com.kibotu.ruler.model.AppComponent
import com.kibotu.ruler.model.AppFile
import com.kibotu.ruler.model.AppReport
import com.kibotu.ruler.model.ComponentType
import com.kibotu.ruler.model.DynamicFeature
import com.kibotu.ruler.model.FileType
import com.kibotu.ruler.model.Pinned
import com.kibotu.ruler.model.ProguardReach
import com.kibotu.ruler.model.ProguardRuleFile
import com.kibotu.ruler.model.RuleFlag
import com.kibotu.ruler.model.RuleImpact
import com.kibotu.ruler.model.RuleMatch
import com.kibotu.ruler.model.RuleScope
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class JsonReporterTest {
    private val reporter = JsonReporter()

    @Test
    fun `report json is written and reads back unchanged`(@TempDir targetDir: File) {
        val report = Json.decodeFromString<AppReport>(
            javaClass.getResource("/fixtures/report-sample.json")!!.readText(),
        )

        val file = reporter.write(report, targetDir)

        assertThat(file.name).isEqualTo("report.json")
        assertThat(Json.decodeFromString<AppReport>(file.readText())).isEqualTo(report)
    }

    /**
     * `report.json` is a published contract, so its field names and shape are locked here. Changing
     * this expectation is a breaking change for every consumer of the report.
     */
    @Test
    fun `report json schema is locked`(@TempDir targetDir: File) {
        val report = AppReport(
            name = "com.test.app",
            version = "1.0.0",
            variant = "debug",
            downloadSize = 100,
            installSize = 200,
            components = listOf(
                AppComponent(
                    name = ":app",
                    type = ComponentType.INTERNAL,
                    downloadSize = 60,
                    installSize = 120,
                    files = listOf(AppFile("com.test.Main", FileType.CLASS, 60, 120, owner = "app-team")),
                    owner = "app-team",
                    internal = true,
                ),
            ),
            dynamicFeatures = listOf(
                DynamicFeature(name = "payments", downloadSize = 40, installSize = 80, files = null),
            ),
        )

        assertThat(reporter.write(report, targetDir).readText()).isEqualTo(
            """
            {"name":"com.test.app","version":"1.0.0","variant":"debug","downloadSize":100,"installSize":200,
            "components":[{"name":":app","type":"INTERNAL","downloadSize":60,"installSize":120,
            "files":[{"name":"com.test.Main","type":"CLASS","downloadSize":60,"installSize":120,
            "owner":"app-team"}],"owner":"app-team","internal":true}],
            "dynamicFeatures":[{"name":"payments","downloadSize":40,"installSize":80,"files":null}]}
            """.trimIndent().replace("\n", ""),
        )
    }

    /**
     * The ProGuard fields name a dependency of the app, so they are part of the same published
     * contract and locked with it. `targets` is the one a consumer reads to know which component a
     * rule keeps, and it is only there when the reach analysis proved it.
     */
    @Test
    fun `proguard schema is locked`(@TempDir targetDir: File) {
        val report = AppReport(
            name = "com.test.app",
            version = "1.0.0",
            variant = "debug",
            downloadSize = 100,
            installSize = 200,
            components = listOf(
                AppComponent(
                    name = ":app",
                    type = ComponentType.INTERNAL,
                    downloadSize = 100,
                    installSize = 200,
                    files = listOf(AppFile("com.test.Main", FileType.CLASS, 100, 200)),
                    proguardRules = listOf(
                        ProguardRuleFile(
                            origin = "Consumer rules",
                            name = "proguard.txt",
                            content = "-keep class com.test.core.** { *; }",
                            ruleCount = 1,
                            namespace = "com.test.app",
                            flags = listOf(RuleFlag(1, "-keep class com.test.core.**", RuleScope.CROSS_DEPENDENCY, listOf(":core"))),
                        ),
                    ),
                    proguardReach = ProguardReach(
                        classes = 1,
                        bytes = 200,
                        ownKept = Pinned(0, 0),
                        othersKept = Pinned(1, 200),
                        affectedBy = listOf(
                            RuleImpact(
                                source = ":core",
                                classes = 1,
                                kept = Pinned(1, 200),
                                onlyTheirs = Pinned(1, 200),
                                rules = listOf(RuleMatch("-keep class com.test.core.**", 1, 200)),
                            ),
                        ),
                    ),
                ),
            ),
            dynamicFeatures = emptyList(),
        )

        assertThat(reporter.write(report, targetDir).readText()).isEqualTo(
            """
            {"name":"com.test.app","version":"1.0.0","variant":"debug","downloadSize":100,"installSize":200,
            "components":[{"name":":app","type":"INTERNAL","downloadSize":100,"installSize":200,
            "files":[{"name":"com.test.Main","type":"CLASS","downloadSize":100,"installSize":200}],
            "proguardRules":[{"origin":"Consumer rules","name":"proguard.txt",
            "content":"-keep class com.test.core.** { *; }","ruleCount":1,"namespace":"com.test.app",
            "flags":[{"line":1,"directive":"-keep class com.test.core.**","scope":"CROSS_DEPENDENCY",
            "targets":[":core"]}]}],
            "proguardReach":{"classes":1,"bytes":200,"ownKept":{"classes":0,"bytes":0},
            "othersKept":{"classes":1,"bytes":200},
            "affectedBy":[{"source":":core","classes":1,"kept":{"classes":1,"bytes":200},
            "onlyTheirs":{"classes":1,"bytes":200},
            "rules":[{"directive":"-keep class com.test.core.**","classes":1,"bytes":200}]}]}}],
            "dynamicFeatures":[]}
            """.trimIndent().replace("\n", ""),
        )
    }

    @Test
    fun `overwrites an existing report`(@TempDir targetDir: File) {
        val report = AppReport("test", "1.0", "debug", 0, 0, emptyList(), emptyList())
        reporter.write(report, targetDir)

        assertThat(reporter.write(report, targetDir).length()).isGreaterThan(0)
    }
}
