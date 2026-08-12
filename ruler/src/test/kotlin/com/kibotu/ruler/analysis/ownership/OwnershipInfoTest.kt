package com.kibotu.ruler.analysis.ownership

import com.google.common.truth.Truth.assertThat
import com.kibotu.ruler.model.ComponentType
import org.junit.jupiter.api.Test

class OwnershipInfoTest {

    @Test
    fun `exact match returns owner`() {
        val entries = listOf(
            OwnershipEntry(":app", "app-team"),
            OwnershipEntry(":lib", "lib-team"),
        )
        val info = OwnershipInfo(entries, "")

        assertThat(info.getOwner(":app", ComponentType.INTERNAL)).isEqualTo("app-team")
        assertThat(info.getOwner(":lib", ComponentType.INTERNAL)).isEqualTo("lib-team")
    }

    @Test
    fun `wildcard star matches any characters`() {
        val entries = listOf(
            OwnershipEntry("com.mycompany.*", "core-team"),
        )
        val info = OwnershipInfo(entries, "")

        assertThat(info.getOwner("com.mycompany.Foo", ComponentType.INTERNAL)).isEqualTo("core-team")
        assertThat(info.getOwner("com.mycompany.bar.Baz", ComponentType.INTERNAL)).isEqualTo("core-team")
        assertThat(info.getOwner("com.other.Foo", ComponentType.INTERNAL)).isNull()
    }

    @Test
    fun `wildcard question mark matches single character`() {
        val entries = listOf(
            OwnershipEntry("Feature?", "feature-team"),
        )
        val info = OwnershipInfo(entries, "")

        assertThat(info.getOwner("FeatureA", ComponentType.INTERNAL)).isEqualTo("feature-team")
        assertThat(info.getOwner("FeatureX", ComponentType.INTERNAL)).isEqualTo("feature-team")
        assertThat(info.getOwner("Feature", ComponentType.INTERNAL)).isNull()
        assertThat(info.getOwner("FeatureAB", ComponentType.INTERNAL)).isNull()
    }

    @Test
    fun `mid-string wildcard works`() {
        val entries = listOf(
            OwnershipEntry("*Feature*", "feature-team"),
        )
        val info = OwnershipInfo(entries, "")

        assertThat(info.getOwner("CoreFeature", ComponentType.INTERNAL)).isEqualTo("feature-team")
        assertThat(info.getOwner("FeatureModule", ComponentType.INTERNAL)).isEqualTo("feature-team")
        assertThat(info.getOwner("SomeFeatureModule", ComponentType.INTERNAL)).isEqualTo("feature-team")
        assertThat(info.getOwner("Other", ComponentType.INTERNAL)).isNull()
    }

    @Test
    fun `first match wins (YAML order)`() {
        val entries = listOf(
            OwnershipEntry("com.specific.Module", "specific-team"),
            OwnershipEntry("com.*", "generic-team"),
        )
        val info = OwnershipInfo(entries, "")

        assertThat(info.getOwner("com.specific.Module", ComponentType.INTERNAL)).isEqualTo("specific-team")
        assertThat(info.getOwner("com.other.Module", ComponentType.INTERNAL)).isEqualTo("generic-team")
    }

    @Test
    fun `case insensitive matching`() {
        val entries = listOf(
            OwnershipEntry("com.MyCompany.*", "core-team"),
        )
        val info = OwnershipInfo(entries, "")

        assertThat(info.getOwner("com.mycompany.Foo", ComponentType.INTERNAL)).isEqualTo("core-team")
        assertThat(info.getOwner("COM.MYCOMPANY.BAR", ComponentType.INTERNAL)).isEqualTo("core-team")
    }

    @Test
    fun `external component version stripped for lookup`() {
        val entries = listOf(
            OwnershipEntry("com.external:library", "third-party"),
        )
        val info = OwnershipInfo(entries, "")

        assertThat(info.getOwner("com.external:library:1.2.3", ComponentType.EXTERNAL)).isEqualTo("third-party")
        assertThat(info.getOwner("com.external:library", ComponentType.EXTERNAL)).isEqualTo("third-party")
    }

    @Test
    fun `unmatched returns null when defaultOwner is empty`() {
        val entries = listOf(
            OwnershipEntry(":app", "app-team"),
        )
        val info = OwnershipInfo(entries, "")

        assertThat(info.getOwner(":unknown", ComponentType.INTERNAL)).isNull()
    }

    @Test
    fun `unmatched returns defaultOwner when set`() {
        val entries = listOf(
            OwnershipEntry(":app", "app-team"),
        )
        val info = OwnershipInfo(entries, "fallback-team")

        assertThat(info.getOwner(":unknown", ComponentType.INTERNAL)).isEqualTo("fallback-team")
    }

    @Test
    fun `getInternal returns entry internal flag`() {
        val entries = listOf(
            OwnershipEntry("com.internal.*", "internal-team", internal = true),
            OwnershipEntry("com.external.*", "external-team", internal = false),
            OwnershipEntry("com.default.*", "default-team"),
        )
        val info = OwnershipInfo(entries, "")

        assertThat(info.getInternal("com.internal.Foo", ComponentType.INTERNAL)).isTrue()
        assertThat(info.getInternal("com.external.Foo", ComponentType.INTERNAL)).isFalse()
        assertThat(info.getInternal("com.default.Foo", ComponentType.INTERNAL)).isNull()
        assertThat(info.getInternal("com.unmatched.Foo", ComponentType.INTERNAL)).isNull()
    }

    @Test
    fun `file owner inherits from component`() {
        val entries = listOf(
            OwnershipEntry(":app", "app-team"),
        )
        val info = OwnershipInfo(entries, "")

        assertThat(info.getOwner("com.Foo", ":app", ComponentType.INTERNAL)).isEqualTo("app-team")
    }

    @Test
    fun `file owner overrides component owner`() {
        val entries = listOf(
            OwnershipEntry(":app", "app-team"),
            OwnershipEntry("com.Special*", "special-team"),
        )
        val info = OwnershipInfo(entries, "")

        assertThat(info.getOwner("com.SpecialClass", ":app", ComponentType.INTERNAL)).isEqualTo("special-team")
        assertThat(info.getOwner("com.RegularClass", ":app", ComponentType.INTERNAL)).isEqualTo("app-team")
    }

    @Test
    fun `feature owner lookup`() {
        val entries = listOf(
            OwnershipEntry("dynamic", "dynamic-team"),
        )
        val info = OwnershipInfo(entries, "")

        assertThat(info.getOwner("dynamic")).isEqualTo("dynamic-team")
        assertThat(info.getInternal("dynamic")).isNull()
    }

    @Test
    fun `globToRegex escapes special regex characters`() {
        val regex = OwnershipInfo.globToRegex("com.foo[bar](baz)")
        assertThat(regex.matches("com.foo[bar](baz)")).isTrue()
        assertThat(regex.matches("com.fooXbarYbazZ")).isFalse()
    }
}
