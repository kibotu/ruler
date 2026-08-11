package com.spotify.ruler.common.report

import com.google.common.truth.Truth.assertThat
import com.spotify.ruler.models.AppComponent
import com.spotify.ruler.models.AppFile
import com.spotify.ruler.models.AppReport
import com.spotify.ruler.models.ComponentType
import com.spotify.ruler.models.DynamicFeature
import com.spotify.ruler.models.FileType
import com.spotify.ruler.models.ResourceType
import org.junit.jupiter.api.Test

class ReportInsightsTest {

    private fun createReport(
        components: List<AppComponent> = emptyList(),
        dynamicFeatures: List<DynamicFeature> = emptyList(),
    ) = AppReport(
        name = "com.test.app",
        version = "1.0.0",
        variant = "release",
        downloadSize = components.sumOf { it.downloadSize },
        installSize = components.sumOf { it.installSize },
        components = components,
        dynamicFeatures = dynamicFeatures,
    )

    @Test
    fun `component type distribution`() {
        val report = createReport(
            components = listOf(
                AppComponent(":app", ComponentType.INTERNAL, 100, 200, null),
                AppComponent(":lib", ComponentType.INTERNAL, 50, 80, null),
                AppComponent("com.ext:lib", ComponentType.EXTERNAL, 200, 300, null),
            )
        )
        val insights = ReportInsights.from(report)

        assertThat(insights.componentTypes).hasSize(2)
        val internal = insights.componentTypes.first { it.label == "INTERNAL" }
        assertThat(internal.downloadSize).isEqualTo(150)
        assertThat(internal.installSize).isEqualTo(280)
        assertThat(internal.count).isEqualTo(2)
        val external = insights.componentTypes.first { it.label == "EXTERNAL" }
        assertThat(external.downloadSize).isEqualTo(200)
        assertThat(external.count).isEqualTo(1)
    }

    @Test
    fun `file type distribution`() {
        val report = createReport(
            components = listOf(
                AppComponent(":app", ComponentType.INTERNAL, 300, 500, listOf(
                    AppFile("Main.class", FileType.CLASS, 100, 200),
                    AppFile("/res/layout/main.xml", FileType.RESOURCE, 100, 150, resourceType = ResourceType.LAYOUT),
                    AppFile("/assets/data.db", FileType.ASSET, 100, 150),
                ))
            )
        )
        val insights = ReportInsights.from(report)

        assertThat(insights.fileTypes).hasSize(3)
        assertThat(insights.fileTypes.first { it.label == "CLASS" }.count).isEqualTo(1)
        assertThat(insights.fileTypes.first { it.label == "RESOURCE" }.count).isEqualTo(1)
        assertThat(insights.fileTypes.first { it.label == "ASSET" }.count).isEqualTo(1)
    }

    @Test
    fun `resource type distribution`() {
        val report = createReport(
            components = listOf(
                AppComponent(":app", ComponentType.INTERNAL, 200, 300, listOf(
                    AppFile("/res/layout/a.xml", FileType.RESOURCE, 50, 80, resourceType = ResourceType.LAYOUT),
                    AppFile("/res/layout/b.xml", FileType.RESOURCE, 50, 80, resourceType = ResourceType.LAYOUT),
                    AppFile("/res/drawable/c.png", FileType.RESOURCE, 100, 140, resourceType = ResourceType.DRAWABLE),
                ))
            )
        )
        val insights = ReportInsights.from(report)

        assertThat(insights.resourceTypes.first { it.label == "LAYOUT" }.count).isEqualTo(2)
        assertThat(insights.resourceTypes.first { it.label == "DRAWABLE" }.count).isEqualTo(1)
    }

    @Test
    fun `top components per size type`() {
        val report = createReport(
            components = listOf(
                AppComponent(":small", ComponentType.INTERNAL, 10, 50, null),
                AppComponent(":large", ComponentType.INTERNAL, 1000, 500, null),
                AppComponent(":medium", ComponentType.INTERNAL, 100, 800, null),
            )
        )
        val insights = ReportInsights.from(report)

        val topDownload = insights.topComponents["download"]!!
        assertThat(topDownload).hasSize(3)
        assertThat(topDownload[0].name).isEqualTo(":large")
        assertThat(topDownload[1].name).isEqualTo(":medium")
        assertThat(topDownload[2].name).isEqualTo(":small")

        val topInstall = insights.topComponents["install"]!!
        assertThat(topInstall[0].name).isEqualTo(":medium")
        assertThat(topInstall[1].name).isEqualTo(":large")
        assertThat(topInstall[2].name).isEqualTo(":small")
    }

    @Test
    fun `treemap capping`() {
        val components = (1..60).map { i ->
            AppComponent(":module$i", ComponentType.INTERNAL, (60 - i).toLong() * 10, (60 - i).toLong() * 20, null)
        }
        val report = createReport(components = components)
        val insights = ReportInsights.from(report)

        assertThat(insights.treemap).hasSize(51) // 50 components + 1 "other" node
        assertThat(insights.treemap.last().name).startsWith("other")
    }

    @Test
    fun `empty report`() {
        val report = createReport()
        val insights = ReportInsights.from(report)

        assertThat(insights.componentTypes).isEmpty()
        assertThat(insights.fileTypes).isEmpty()
        assertThat(insights.resourceTypes).isEmpty()
        assertThat(insights.topComponents).isNotEmpty()
        assertThat(insights.topFiles).isNotEmpty()
        assertThat(insights.owners).isEmpty()
        assertThat(insights.treemap).isEmpty()
    }

    @Test
    fun `no files scenario`() {
        val report = createReport(
            components = listOf(
                AppComponent(":app", ComponentType.INTERNAL, 100, 200, null),
            )
        )
        val insights = ReportInsights.from(report)

        assertThat(insights.fileTypes).isEmpty()
        assertThat(insights.resourceTypes).isEmpty()
    }

    @Test
    fun `owners computed from file-level ownership`() {
        val report = createReport(
            components = listOf(
                AppComponent(":app", ComponentType.INTERNAL, 300, 500, listOf(
                    AppFile("a.class", FileType.CLASS, 100, 200, owner = "team-a"),
                    AppFile("b.class", FileType.CLASS, 100, 150, owner = "team-a"),
                    AppFile("c.class", FileType.CLASS, 100, 150, owner = "team-b"),
                ), owner = "team-a"),
            )
        )
        val insights = ReportInsights.from(report)

        assertThat(insights.owners).hasSize(2)
        val teamA = insights.owners.first { it.owner == "team-a" }
        assertThat(teamA.downloadSize).isEqualTo(200)
        assertThat(teamA.fileCount).isEqualTo(2)
        val teamB = insights.owners.first { it.owner == "team-b" }
        assertThat(teamB.downloadSize).isEqualTo(100)
        assertThat(teamB.fileCount).isEqualTo(1)
    }

    @Test
    fun `owners computed from component-level when no files`() {
        val report = createReport(
            components = listOf(
                AppComponent(":app", ComponentType.INTERNAL, 100, 200, null, owner = "team-a"),
                AppComponent(":lib", ComponentType.INTERNAL, 50, 80, null, owner = "team-b"),
            )
        )
        val insights = ReportInsights.from(report)

        assertThat(insights.owners).hasSize(2)
        assertThat(insights.owners.first { it.owner == "team-a" }.componentCount).isEqualTo(1)
        assertThat(insights.owners.first { it.owner == "team-b" }.componentCount).isEqualTo(1)
    }

    @Test
    fun `treemap has children when files present`() {
        val report = createReport(
            components = listOf(
                AppComponent(":app", ComponentType.INTERNAL, 200, 300, listOf(
                    AppFile("a.class", FileType.CLASS, 100, 150),
                    AppFile("b.class", FileType.CLASS, 100, 150),
                )),
            )
        )
        val insights = ReportInsights.from(report)

        assertThat(insights.treemap).hasSize(1)
        assertThat(insights.treemap[0].children).hasSize(2)
    }
}
