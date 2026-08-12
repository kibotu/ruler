package com.kibotu.ruler.analysis.attribution

import com.google.common.truth.Truth.assertThat
import com.kibotu.ruler.analysis.dependency.DependencyComponent
import com.kibotu.ruler.model.AppFile
import com.kibotu.ruler.model.ComponentType
import com.kibotu.ruler.model.FileType
import org.junit.jupiter.api.Test

class AttributorTest {
    private val defaultComponent = DependencyComponent(":app", ComponentType.INTERNAL)
    private val libComponent = DependencyComponent(":lib", ComponentType.INTERNAL)
    private val externalComponent = DependencyComponent("com.example:lib:1.0", ComponentType.EXTERNAL)

    @Test
    fun `attributes class to single matching dependency`() {
        val dependencies = mapOf(
            "com.example.Foo" to listOf(libComponent),
        )
        val files = listOf(appFile("com.example.Foo", FileType.CLASS))

        val result = Attributor(defaultComponent).attribute(files, dependencies)

        assertThat(result[libComponent]).hasSize(1)
    }

    @Test
    fun `attributes dagger factory to produced type`() {
        val dependencies = mapOf(
            "com.example.Service" to listOf(libComponent),
        )
        val files = listOf(appFile("com.example.Service_Factory", FileType.CLASS))

        val result = Attributor(defaultComponent).attribute(files, dependencies)

        assertThat(result[libComponent]).hasSize(1)
    }

    @Test
    fun `strips resource version qualifiers`() {
        val dependencies = mapOf(
            "/layout/activity_main.xml" to listOf(libComponent),
        )
        val files = listOf(appFile("/res/layout-v21/activity_main.xml", FileType.RESOURCE))

        val result = Attributor(defaultComponent).attribute(files, dependencies)

        assertThat(result[libComponent]).hasSize(1)
    }

    @Test
    fun `normalizes split vector drawable names`() {
        val dependencies = mapOf(
            "/drawable-anydpi-v24/ic_icon.xml" to listOf(libComponent),
        )
        val files = listOf(
            appFile("/res/drawable-anydpi-v24/\$ic_icon__1.xml", FileType.RESOURCE),
        )

        val result = Attributor(defaultComponent).attribute(files, dependencies)

        assertThat(result[libComponent]).hasSize(1)
    }

    @Test
    fun `attributes lzma native library to original dependency`() {
        val dependencies = mapOf(
            "/arm64-v8a/libfoo.so" to listOf(externalComponent),
        )
        val files = listOf(appFile("/lib/arm64-v8a/libfoo.lzma.so", FileType.NATIVE_LIB))

        val result = Attributor(defaultComponent).attribute(files, dependencies)

        assertThat(result[externalComponent]).hasSize(1)
    }

    @Test
    fun `prefers the longer static attribution path`() {
        val shortMatch = DependencyComponent(":short", ComponentType.INTERNAL)
        val longMatch = DependencyComponent(":long", ComponentType.INTERNAL)
        val staticAttributions = listOf(
            StaticAttribution(Regex("client-core"), shortMatch),
            StaticAttribution(Regex("client-core/shared/playlist"), longMatch),
        )
        val files = listOf(appFile("client-core/shared/playlist/track.dat", FileType.NATIVE_FILE))

        val result = Attributor(defaultComponent, staticAttributions).attribute(files, emptyMap())

        assertThat(result[longMatch]).hasSize(1)
    }

    @Test
    fun `static attribution is a fallback for non-native files`() {
        val staticComponent = DependencyComponent(":generated", ComponentType.INTERNAL)
        val attributions = listOf(StaticAttribution(Regex("generated"), staticComponent))
        val files = listOf(
            appFile("/assets/generated/data.bin", FileType.ASSET),
            appFile("/assets/known.bin", FileType.ASSET),
        )
        val dependencies = mapOf("/known.bin" to listOf(libComponent))

        val result = Attributor(defaultComponent, attributions).attribute(files, dependencies)

        assertThat(result[staticComponent]!!.single().name).isEqualTo("/assets/generated/data.bin")
        assertThat(result[libComponent]!!.single().name).isEqualTo("/assets/known.bin")
    }

    @Test
    fun `attributes dagger module to its interface`() {
        val dependencies = mapOf("com.example.NetworkModule" to listOf(libComponent))
        val files = listOf(appFile("com.example.NetworkModule_ProvideClientFactory", FileType.CLASS))

        val result = Attributor(defaultComponent).attribute(files, dependencies)

        assertThat(result[libComponent]).hasSize(1)
    }

    @Test
    fun `attributes lambda by its package`() {
        val dependencies = mapOf("com.example.Foo" to listOf(libComponent))
        val files = listOf(appFile("com.example.-\$\$Lambda\$Foo\$abc", FileType.CLASS))

        val result = Attributor(defaultComponent).attribute(files, dependencies)

        assertThat(result[libComponent]).hasSize(1)
    }

    @Test
    fun `attributes external synthetic class by its simple name`() {
        val dependencies = mapOf("com.other.Foo" to listOf(libComponent))
        val files = listOf(appFile("com.example.Foo\$\$ExternalSyntheticLambda0", FileType.CLASS))

        val result = Attributor(defaultComponent).attribute(files, dependencies)

        assertThat(result[libComponent]).hasSize(1)
    }

    @Test
    fun `attributes unknown class by its package`() {
        val dependencies = mapOf("com.example.Known" to listOf(libComponent))
        val files = listOf(appFile("com.example.Unknown", FileType.CLASS))

        val result = Attributor(defaultComponent).attribute(files, dependencies)

        assertThat(result[libComponent]).hasSize(1)
    }

    @Test
    fun `an ambiguous package is not attributed`() {
        val dependencies = mapOf(
            "com.example.One" to listOf(libComponent),
            "com.example.Two" to listOf(externalComponent),
        )
        val files = listOf(appFile("com.example.Unknown", FileType.CLASS))

        val result = Attributor(defaultComponent).attribute(files, dependencies)

        assertThat(result[defaultComponent]).hasSize(1)
    }

    @Test
    fun `a file in two components is not attributed`() {
        val dependencies = mapOf("/license.txt" to listOf(libComponent, externalComponent))
        val files = listOf(appFile("/license.txt", FileType.OTHER))

        val result = Attributor(defaultComponent).attribute(files, dependencies)

        assertThat(result[defaultComponent]).hasSize(1)
    }

    @Test
    fun `falls back to default component`() {
        val files = listOf(appFile("com.unknown.Unknown", FileType.CLASS))

        val result = Attributor(defaultComponent).attribute(files, emptyMap())

        assertThat(result[defaultComponent]).hasSize(1)
    }

    private fun appFile(name: String, type: FileType) =
        AppFile(name, type, downloadSize = 10, installSize = 20)
}
