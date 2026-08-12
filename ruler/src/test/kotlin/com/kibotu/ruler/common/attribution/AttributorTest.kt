package com.kibotu.ruler.common.attribution

import com.google.common.truth.Truth.assertThat
import com.kibotu.ruler.common.dependency.DependencyComponent
import com.kibotu.ruler.models.AppFile
import com.kibotu.ruler.models.ComponentType
import com.kibotu.ruler.models.FileType
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
    fun `prefers longer static dependency regex`() {
        val shortMatch = DependencyComponent(":short", ComponentType.INTERNAL)
        val longMatch = DependencyComponent(":long", ComponentType.INTERNAL)
        val staticDependencies = mapOf(
            Regex("client-core") to listOf(shortMatch),
            Regex("client-core/shared/playlist") to listOf(longMatch),
        )
        val files = listOf(appFile("client-core/shared/playlist/track.dat", FileType.NATIVE_FILE))

        val result = Attributor(defaultComponent, staticDependencies).attribute(files, emptyMap())

        assertThat(result[longMatch]).hasSize(1)
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
