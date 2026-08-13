package com.kibotu.ruler.plugin

import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Applies the plugin to a real Android build.
 *
 * These tests configure the build but never run the analysis, because that needs an Android SDK
 * and a bundle. `sample` covers the analysis end to end.
 */
class RulerPluginFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    @BeforeEach
    fun setUp() {
        writeSettings()
        writeLocalProperties()
        writeManifest()
    }

    @Test
    fun `registers one task per variant`() {
        writeBuildScript()

        val output = run("tasks", "--group=ruler").output

        assertThat(output).contains("analyzeDebugBundle")
        assertThat(output).contains("analyzeReleaseBundle")
    }

    @Test
    fun `the report paths are printed after the analysis`() {
        writeBuildScript()

        val output = run("analyzeDebugBundle", "--dry-run").output

        // The finalizer runs even when the analysis is up to date, which is when the paths would
        // otherwise go unreported.
        assertThat(output).contains(":printRulerDebugReports SKIPPED")
        assertThat(output.indexOf(":printRulerDebugReports"))
            .isGreaterThan(output.indexOf(":analyzeDebugBundle"))
    }

    @Test
    fun `task properties are fully configured`() {
        writeBuildScript()

        // Dry-run realises the task and validates every input and output property.
        val result = run("analyzeDebugBundle", "--dry-run")

        assertThat(result.output).contains(":analyzeDebugBundle SKIPPED")
    }

    @Test
    fun `supports the configuration cache`() {
        writeBuildScript()

        run("analyzeDebugBundle", "--dry-run", "--configuration-cache")
        val second = run("analyzeDebugBundle", "--dry-run", "--configuration-cache")

        assertThat(second.output).contains("Configuration cache entry reused")
    }

    @Test
    fun `verification thresholds are optional`() {
        writeBuildScript(verification = "")

        val result = run("analyzeDebugBundle", "--dry-run")

        assertThat(result.output).contains(":analyzeDebugBundle SKIPPED")
    }

    @Test
    fun `a missing device specification explains itself`() {
        writeBuildScript(deviceSpec = """locale.set("en")""")

        val result = runAndFail("analyzeDebugBundle", "--dry-run")

        assertThat(result.output).contains("ruler { abi } is not set")
    }

    @Test
    fun `no task is registered without the application plugin`() {
        writeBuildScript(androidPlugin = "com.android.library", android = "")

        val output = run("tasks", "--all").output

        assertThat(output).doesNotContain("analyzeDebugBundle")
    }

    private fun run(vararg arguments: String) = runner(*arguments).build()

    private fun runAndFail(vararg arguments: String) = runner(*arguments).buildAndFail()

    private fun runner(vararg arguments: String): GradleRunner = GradleRunner.create()
        .withProjectDir(projectDir)
        .withPluginClasspath(pluginClasspath())
        .withArguments(*arguments, "--stacktrace")
        .forwardOutput()

    private fun pluginClasspath(): List<File> = checkNotNull(System.getProperty("pluginClasspath")) {
        "pluginClasspath system property is not set"
    }.split(File.pathSeparator).map(::File)

    private fun writeSettings() {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement {
                repositories {
                    google()
                    mavenCentral()
                    gradlePluginPortal()
                }
                plugins {
                    id("com.android.application") version "9.3.1"
                    id("com.android.library") version "9.3.1"
                }
            }
            dependencyResolutionManagement {
                repositories {
                    google()
                    mavenCentral()
                }
            }
            rootProject.name = "fixture"
            """.trimIndent(),
        )
        projectDir.resolve("gradle.properties").writeText("android.useAndroidX=true\n")
    }

    private fun writeLocalProperties() {
        val sdkDir = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        if (sdkDir != null) {
            projectDir.resolve("local.properties").writeText("sdk.dir=${sdkDir.replace('\\', '/')}\n")
        }
    }

    private fun writeManifest() {
        projectDir.resolve("src/main").mkdirs()
        projectDir.resolve("src/main/AndroidManifest.xml").writeText("<manifest />")
    }

    private fun writeBuildScript(
        androidPlugin: String = "com.android.application",
        android: String = """
            defaultConfig {
                applicationId = "com.example.fixture"
                minSdk = 23
                versionCode = 1
                versionName = "1.0"
            }
        """.trimIndent(),
        deviceSpec: String = """
            abi.set("arm64-v8a")
            locale.set("en")
            screenDensity.set(480)
            sdkVersion.set(36)
        """.trimIndent(),
        verification: String = """
            verification {
                downloadSizeThreshold = 1_000_000
                installSizeThreshold = 1_000_000
            }
        """.trimIndent(),
    ) {
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("$androidPlugin")
                id("net.kibotu.ruler")
            }

            android {
                namespace = "com.example.fixture"
                compileSdk = 37
                $android
            }

            ruler {
                $deviceSpec
                $verification
            }
            """.trimIndent(),
        )
    }
}
