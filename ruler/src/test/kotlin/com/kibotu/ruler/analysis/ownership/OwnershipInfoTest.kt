package com.kibotu.ruler.analysis.ownership

import com.google.common.truth.Truth.assertThat
import com.kibotu.ruler.model.ComponentType.EXTERNAL
import com.kibotu.ruler.model.ComponentType.INTERNAL
import org.junit.jupiter.api.Test

class OwnershipInfoTest {

    @Test
    fun `an exact identifier matches its component`() {
        val info = ownership(OwnershipEntry(":app", "app-team"), OwnershipEntry(":lib", "lib-team"))

        assertThat(info.owners(":app", INTERNAL)).containsExactly("app-team")
        assertThat(info.owners(":lib", INTERNAL)).containsExactly("lib-team")
        assertThat(info.owners(":unknown", INTERNAL)).isNull()
    }

    @Test
    fun `star matches any characters and question mark matches one`() {
        val info = ownership(
            OwnershipEntry("com.mycompany.*", "core-team"),
            OwnershipEntry("Feature?", "feature-team"),
        )

        assertThat(info.owners("com.mycompany.bar.Baz", INTERNAL)).containsExactly("core-team")
        assertThat(info.owners("com.other.Foo", INTERNAL)).isNull()
        assertThat(info.owners("FeatureA", INTERNAL)).containsExactly("feature-team")
        assertThat(info.owners("Feature", INTERNAL)).isNull()
        assertThat(info.owners("FeatureAB", INTERNAL)).isNull()
    }

    @Test
    fun `matching ignores case`() {
        val info = ownership(OwnershipEntry("com.MyCompany.*", "core-team"), OwnershipEntry(":App", "app-team"))

        assertThat(info.owners("COM.MYCOMPANY.BAR", INTERNAL)).containsExactly("core-team")
        assertThat(info.owners(":app", INTERNAL)).containsExactly("app-team")
    }

    @Test
    fun `the first matching entry wins`() {
        val info = ownership(
            OwnershipEntry("com.specific.Module", "specific-team"),
            OwnershipEntry("com.*", "generic-team"),
        )

        assertThat(info.owners("com.specific.Module", INTERNAL)).containsExactly("specific-team")
        assertThat(info.owners("com.other.Module", INTERNAL)).containsExactly("generic-team")
    }

    @Test
    fun `an external component matches with and without its version`() {
        val info = ownership(OwnershipEntry("com.external:library", "third-party"))

        assertThat(info.owners("com.external:library:1.2.3", EXTERNAL)).containsExactly("third-party")
        assertThat(info.owners("com.external:library", EXTERNAL)).containsExactly("third-party")
    }

    @Test
    fun `an unmatched name falls back to the default owner`() {
        val info = OwnershipInfo(listOf(OwnershipEntry(":app", "app-team")), defaultOwner = "fallback-team")

        assertThat(info.owners(":unknown", INTERNAL)).containsExactly("fallback-team")
    }

    @Test
    fun `every owner of an entry is reported`() {
        val info = ownership(OwnershipEntry(":app", listOf("core", "platform")))

        assertThat(info.owners(":app", INTERNAL)).containsExactly("core", "platform").inOrder()
    }

    @Test
    fun `a file inherits nothing when no entry names it`() {
        val info = ownership(OwnershipEntry(":app", "app-team"), OwnershipEntry("com.Special*", "special-team"))

        assertThat(info.fileOwners("com.SpecialClass")).containsExactly("special-team")
        assertThat(info.fileOwners("com.RegularClass")).isNull()
    }

    @Test
    fun `the internal override is only set where an entry declares it`() {
        val info = ownership(
            OwnershipEntry("com.internal.*", "internal-team", internal = true),
            OwnershipEntry("com.external.*", "external-team", internal = false),
            OwnershipEntry("com.default.*", "default-team"),
        )

        assertThat(info.internalOverride("com.internal.Foo", INTERNAL)).isTrue()
        assertThat(info.internalOverride("com.external.Foo", INTERNAL)).isFalse()
        assertThat(info.internalOverride("com.default.Foo", INTERNAL)).isNull()
        assertThat(info.internalOverride("com.unmatched.Foo", INTERNAL)).isNull()
    }

    @Test
    fun `a dynamic feature is matched by name`() {
        val info = ownership(OwnershipEntry("dynamic", "dynamic-team"))

        assertThat(info.owners("dynamic")).containsExactly("dynamic-team")
        assertThat(info.internalOverride("dynamic")).isNull()
    }

    @Test
    fun `regex characters in an identifier are matched literally`() {
        val info = ownership(OwnershipEntry("com.foo[bar](baz)", "team"))

        assertThat(info.owners("com.foo[bar](baz)", INTERNAL)).containsExactly("team")
        assertThat(info.owners("com.fooXbarYbazZ", INTERNAL)).isNull()
    }

    @Test
    fun `glob patterns keep matching literally around their wildcards`() {
        val regex = OwnershipInfo.globToRegex("com.foo[bar].*")

        assertThat(regex.matches("com.foo[bar].Baz")).isTrue()
        assertThat(regex.matches("com.fooXbarY.Baz")).isFalse()
    }

    private fun ownership(vararg entries: OwnershipEntry) = OwnershipInfo(entries.toList(), defaultOwner = "")
}
