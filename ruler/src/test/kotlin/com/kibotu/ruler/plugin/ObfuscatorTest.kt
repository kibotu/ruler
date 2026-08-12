package com.kibotu.ruler.plugin

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.io.File

class ObfuscatorTest {

    @Test
    fun `dexguard writes its own mapping files`() {
        assertThat(Obfuscator.DEXGUARD.mappingPath("release"))
            .isEqualTo("outputs/dexguard/mapping/bundle/release/mapping.txt")
        assertThat(Obfuscator.DEXGUARD.resourceMappingPath("release"))
            .isEqualTo("outputs/dexguard/mapping/bundle/release/resourcefilenamemapping.txt")
    }

    @Test
    fun `proguard writes no resource mapping`() {
        assertThat(Obfuscator.PROGUARD.mappingPath("release"))
            .isEqualTo("outputs/proguard/release/mapping/mapping.txt")
        assertThat(Obfuscator.PROGUARD.resourceMappingPath("release")).isNull()
    }

    @Test
    fun `dexguard writes a separate bundle next to the standard one`() {
        val bundle = File("/build/outputs/bundle/release/app-release.aab")

        assertThat(Obfuscator.DEXGUARD.protectedBundle(bundle))
            .isEqualTo(File("/build/outputs/bundle/release/app-release-protected.aab"))
    }

    @Test
    fun `proguard replaces the standard bundle`() {
        val bundle = File("/build/outputs/bundle/release/app-release.aab")

        assertThat(Obfuscator.PROGUARD.protectedBundle(bundle)).isNull()
    }
}
