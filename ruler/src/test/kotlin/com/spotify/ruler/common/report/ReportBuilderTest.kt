package com.spotify.ruler.common.report

import com.google.common.truth.Truth.assertThat
import com.spotify.ruler.common.dependency.DependencyComponent
import com.spotify.ruler.common.models.AppInfo
import com.spotify.ruler.common.ownership.OwnershipEntry
import com.spotify.ruler.common.ownership.OwnershipInfo
import com.spotify.ruler.models.AppFile
import com.spotify.ruler.models.ComponentType
import com.spotify.ruler.models.FileType
import com.spotify.ruler.models.ResourceType
import org.junit.jupiter.api.Test

class ReportBuilderTest {
    private val builder = ReportBuilder()

    private val appInfo = AppInfo("release", "com.spotify.music", "1.2.3")
    private val components = mapOf(
        DependencyComponent(":app", ComponentType.INTERNAL) to listOf(
            AppFile("com.spotify.MainActivity", FileType.CLASS, 100, 200),
            AppFile("/res/layout/activity_main.xml", FileType.RESOURCE, 150, 250, resourceType = ResourceType.LAYOUT),
        ),
        DependencyComponent(":lib", ComponentType.INTERNAL) to listOf(
            AppFile("/assets/license.html", FileType.ASSET, 500, 600),
        ),
    )
    private val features = mapOf(
        "dynamic" to listOf(
            AppFile("com.spotify.DynamicActivity", FileType.CLASS, 200, 300),
            AppFile("/res/layout/activity_dynamic.xml", FileType.RESOURCE, 100, 250, resourceType = ResourceType.LAYOUT),
        ),
    )
    private val ownershipEntries = listOf(OwnershipEntry(":app", "app-team"), OwnershipEntry("dynamic", "dynamic-team"))
    private val ownershipInfo = OwnershipInfo(ownershipEntries, "default-team")

    @Test
    fun `report is built correctly with ownership`() {
        val report = builder.build(appInfo, components, features, ownershipInfo, omitFileBreakdown = false)

        assertThat(report.name).isEqualTo("com.spotify.music")
        assertThat(report.version).isEqualTo("1.2.3")
        assertThat(report.variant).isEqualTo("release")
        assertThat(report.downloadSize).isEqualTo(750L)
        assertThat(report.installSize).isEqualTo(1050L)
        assertThat(report.components).hasSize(2)
        assertThat(report.dynamicFeatures).hasSize(1)

        // Components are sorted descending by download size
        assertThat(report.components[0].name).isEqualTo(":lib")
        assertThat(report.components[1].name).isEqualTo(":app")

        // Ownership is applied
        assertThat(report.components[0].owner).isEqualTo("default-team")
        assertThat(report.components[1].owner).isEqualTo("app-team")

        // Files within :app are sorted descending
        val appFiles = report.components[1].files!!
        assertThat(appFiles[0].name).isEqualTo("/res/layout/activity_main.xml")
        assertThat(appFiles[1].name).isEqualTo("com.spotify.MainActivity")

        // Dynamic feature ownership
        assertThat(report.dynamicFeatures[0].owner).isEqualTo("dynamic-team")
    }

    @Test
    fun `report is built without file breakdown`() {
        val report = builder.build(appInfo, components, features, ownershipInfo, omitFileBreakdown = true)

        assertThat(report.components.all { it.files == null }).isTrue()
        assertThat(report.dynamicFeatures.all { it.files == null }).isTrue()
    }

    @Test
    fun `ownership is omitted when null`() {
        val report = builder.build(appInfo, components, features, ownershipInfo = null, omitFileBreakdown = false)

        assertThat(report.components.all { it.owner == null }).isTrue()
        assertThat(report.dynamicFeatures.all { it.owner == null }).isTrue()
        // Files still exist but without owners
        assertThat(report.components[1].files!![0].owner).isNull()
    }

    @Test
    fun `empty components and features`() {
        val info = AppInfo("debug", "com.test.app", "0.0.1")
        val report = builder.build(info, emptyMap(), emptyMap(), null, omitFileBreakdown = false)

        assertThat(report.downloadSize).isEqualTo(0L)
        assertThat(report.installSize).isEqualTo(0L)
        assertThat(report.components).isEmpty()
        assertThat(report.dynamicFeatures).isEmpty()
    }

    @Test
    fun `sorting is descending by size`() {
        val comps = mapOf(
            DependencyComponent(":small", ComponentType.INTERNAL) to listOf(
                AppFile("small.class", FileType.CLASS, 10, 20),
            ),
            DependencyComponent(":large", ComponentType.INTERNAL) to listOf(
                AppFile("large.class", FileType.CLASS, 1000, 2000),
            ),
            DependencyComponent(":medium", ComponentType.INTERNAL) to listOf(
                AppFile("medium.class", FileType.CLASS, 100, 200),
            ),
        )
        val report = builder.build(appInfo, comps, emptyMap(), null, omitFileBreakdown = false)

        assertThat(report.components[0].name).isEqualTo(":large")
        assertThat(report.components[1].name).isEqualTo(":medium")
        assertThat(report.components[2].name).isEqualTo(":small")
    }

    @Test
    fun `internal flag is set from ownership entry`() {
        val entries = listOf(
            OwnershipEntry(":app", "app-team", internal = true),
            OwnershipEntry(":lib", "lib-team", internal = false),
        )
        val info = OwnershipInfo(entries, "")
        val report = builder.build(appInfo, components, features, info, omitFileBreakdown = false)

        val appComponent = report.components.find { it.name == ":app" }!!
        val libComponent = report.components.find { it.name == ":lib" }!!

        assertThat(appComponent.internal).isTrue()
        assertThat(libComponent.internal).isFalse()
    }

    @Test
    fun `internal flag is null when not specified in ownership`() {
        val entries = listOf(
            OwnershipEntry(":app", "app-team"),
        )
        val info = OwnershipInfo(entries, "")
        val report = builder.build(appInfo, components, features, info, omitFileBreakdown = false)

        val appComponent = report.components.find { it.name == ":app" }!!
        assertThat(appComponent.internal).isNull()
    }

    @Test
    fun `app component auto-tagged when no ownership match`() {
        val comps = mapOf(
            DependencyComponent(":sample:app", ComponentType.INTERNAL) to listOf(
                AppFile("MainActivity.class", FileType.CLASS, 100, 200),
            ),
        )
        val report = builder.build(appInfo, comps, emptyMap(), null, omitFileBreakdown = false, appProjectPath = ":sample:app")

        val appComponent = report.components[0]
        assertThat(appComponent.owner).isEqualTo("App")
        assertThat(appComponent.internal).isTrue()
    }

    @Test
    fun `app component uses ownership over auto-tag`() {
        val entries = listOf(
            OwnershipEntry(":sample:app", "my-team", internal = false),
        )
        val info = OwnershipInfo(entries, "")
        val comps = mapOf(
            DependencyComponent(":sample:app", ComponentType.INTERNAL) to listOf(
                AppFile("MainActivity.class", FileType.CLASS, 100, 200),
            ),
        )
        val report = builder.build(appInfo, comps, emptyMap(), info, omitFileBreakdown = false, appProjectPath = ":sample:app")

        val appComponent = report.components[0]
        assertThat(appComponent.owner).isEqualTo("my-team")
        assertThat(appComponent.internal).isFalse()
    }

    @Test
    fun `non-app component not auto-tagged`() {
        val comps = mapOf(
            DependencyComponent(":lib", ComponentType.INTERNAL) to listOf(
                AppFile("Util.class", FileType.CLASS, 100, 200),
            ),
        )
        val report = builder.build(appInfo, comps, emptyMap(), null, omitFileBreakdown = false, appProjectPath = ":sample:app")

        val libComponent = report.components[0]
        assertThat(libComponent.owner).isNull()
        assertThat(libComponent.internal).isNull()
    }

    @Test
    fun `unmatched owner is null with empty defaultOwner`() {
        val entries = listOf(
            OwnershipEntry(":app", "app-team"),
        )
        val info = OwnershipInfo(entries, "")
        val report = builder.build(appInfo, components, features, info, omitFileBreakdown = false)

        val libComponent = report.components.find { it.name == ":lib" }!!
        assertThat(libComponent.owner).isNull()
    }

    @Test
    fun `dynamic feature internal flag is set from ownership`() {
        val entries = listOf(
            OwnershipEntry("dynamic", "dynamic-team", internal = true),
        )
        val info = OwnershipInfo(entries, "")
        val report = builder.build(appInfo, components, features, info, omitFileBreakdown = false)

        assertThat(report.dynamicFeatures[0].internal).isTrue()
    }
}
