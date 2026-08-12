package com.kibotu.ruler.common

import com.google.common.truth.Truth.assertThat
import com.kibotu.ruler.common.apk.ApkCreator.Companion.BASE_FEATURE_NAME
import com.kibotu.ruler.common.dependency.DependencyComponent
import com.kibotu.ruler.common.models.AppInfo
import com.kibotu.ruler.common.models.DeviceSpec
import com.kibotu.ruler.common.models.RulerConfig
import com.kibotu.ruler.common.verification.VerificationConfig
import com.kibotu.ruler.models.ComponentType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BaseRulerTaskTest {
    @Test
    fun `run writes json and html reports`(@TempDir tempDir: File) {
        val apk = createTestApk(tempDir)
        val reportDir = tempDir.resolve("reports").apply { mkdirs() }
        val task = TestRulerTask(
            rulerConfig = RulerConfig(
                projectPath = ":sample:app",
                apkFilesMap = mapOf(BASE_FEATURE_NAME to listOf(apk)),
                reportDir = reportDir,
                ownershipFile = null,
                staticDependenciesFile = null,
                appInfo = AppInfo("debug", "com.kibotu.ruler.sample", "1.0"),
                deviceSpec = DeviceSpec("arm64-v8a", "en", 480, 36),
                defaultOwner = "default-team",
                omitFileBreakdown = false,
                verificationConfig = VerificationConfig(
                    downloadSizeThreshold = Long.MAX_VALUE,
                    installSizeThreshold = Long.MAX_VALUE,
                ),
            ),
            dependencies = mapOf(
                ":sample:app" to listOf(
                    DependencyComponent(":sample:app", ComponentType.INTERNAL),
                ),
            ),
        )

        task.run()

        assertThat(reportDir.resolve("report.json").exists()).isTrue()
        assertThat(reportDir.resolve("report.html").exists()).isTrue()
        assertThat(task.messages).isNotEmpty()
    }

    private fun createTestApk(tempDir: File): File {
        val apk = tempDir.resolve("test.apk")
        ZipOutputStream(apk.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("assets/config.json"))
            zip.write("""{"enabled":true}""".toByteArray())
            zip.closeEntry()
        }
        return apk
    }

    private class TestRulerTask(
        private val rulerConfig: RulerConfig,
        private val dependencies: Map<String, List<DependencyComponent>>,
    ) : BaseRulerTask {
        val messages = mutableListOf<String>()

        override fun print(content: String) {
            messages += content
        }

        override fun provideMappingFile(): File? = null
        override fun provideResourceMappingFile(): File? = null
        override fun rulerConfig(): RulerConfig = rulerConfig
        override fun provideUnstrippedLibraryFiles(): List<File> = emptyList()
        override fun provideBloatyPath(): String? = null
        override fun provideDependencies(): Map<String, List<DependencyComponent>> = dependencies
    }
}
