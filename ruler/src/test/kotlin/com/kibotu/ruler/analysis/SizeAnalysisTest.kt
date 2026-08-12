package com.kibotu.ruler.analysis

import com.google.common.truth.Truth.assertThat
import com.kibotu.ruler.analysis.apk.ApkCreator.Companion.BASE_FEATURE_NAME
import com.kibotu.ruler.analysis.dependency.DependencyEntry
import com.kibotu.ruler.analysis.verification.SizeExceededException
import com.kibotu.ruler.analysis.verification.VerificationConfig
import com.kibotu.ruler.model.AppReport
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SizeAnalysisTest {

    @Test
    fun `writes both reports`(@TempDir tempDir: File) {
        val messages = mutableListOf<String>()
        val reportDir = analyze(tempDir, log = messages::add)

        assertThat(reportDir.resolve("report.json").exists()).isTrue()
        assertThat(reportDir.resolve("report.html").exists()).isTrue()
        assertThat(messages).hasSize(2)
    }

    @Test
    fun `attributes files to the declaring dependency`(@TempDir tempDir: File) {
        val reportDir = analyze(
            tempDir,
            dependencyEntries = listOf(
                DependencyEntry.Default("/config.json", "com.example:config:1.0"),
            ),
        )

        val report = readReport(reportDir)
        val component = report.components.single { it.name == "com.example:config:1.0" }
        assertThat(component.files!!.map { it.name }).containsExactly("/assets/config.json")
    }

    @Test
    fun `falls back to the app module`(@TempDir tempDir: File) {
        val report = readReport(analyze(tempDir))

        val component = report.components.single()
        assertThat(component.name).isEqualTo(":sample:app")
        assertThat(component.owner).isEqualTo("App")
    }

    @Test
    fun `fails the build above the download threshold`(@TempDir tempDir: File) {
        val exception = assertThrows<SizeExceededException> {
            analyze(tempDir, verification = VerificationConfig(downloadSizeThreshold = 1))
        }

        assertThat(exception).hasMessageThat().contains("Download")
    }

    @Test
    fun `static attribution paths are matched literally`(@TempDir tempDir: File) {
        // The dots and the plus sign would match anything if the path were used as a regex.
        val overrides = tempDir.resolve("static.json").apply {
            writeText("""[{ "path": "c.n+ig.json", "id": ":generated" }]""")
        }

        val report = readReport(analyze(tempDir, staticDependenciesFile = overrides))

        assertThat(report.components.single().name).isEqualTo(":sample:app")
    }

    @Test
    fun `static attribution assigns unattributed files`(@TempDir tempDir: File) {
        val overrides = tempDir.resolve("static.json").apply {
            writeText("""[{ "path": "config.json", "id": ":generated" }]""")
        }

        val report = readReport(analyze(tempDir, staticDependenciesFile = overrides))

        assertThat(report.components.single().name).isEqualTo(":generated")
    }

    @Test
    fun `omits the file breakdown on request`(@TempDir tempDir: File) {
        val report = readReport(analyze(tempDir, omitFileBreakdown = true))

        assertThat(report.components.single().files).isNull()
    }

    private fun analyze(
        tempDir: File,
        dependencyEntries: List<DependencyEntry> = emptyList(),
        verification: VerificationConfig = VerificationConfig(),
        omitFileBreakdown: Boolean = false,
        staticDependenciesFile: File? = null,
        log: (String) -> Unit = {},
    ): File {
        val reportDir = tempDir.resolve("reports").apply { mkdirs() }
        SizeAnalysis(
            config = RulerConfig(
                projectPath = ":sample:app",
                apkFilesMap = mapOf(BASE_FEATURE_NAME to listOf(createApk(tempDir))),
                reportDir = reportDir,
                ownershipFile = null,
                staticDependenciesFile = staticDependenciesFile,
                appInfo = AppInfo("debug", "com.kibotu.ruler.sample", "1.0"),
                defaultOwner = "",
                omitFileBreakdown = omitFileBreakdown,
                verificationConfig = verification,
            ),
            dependencyEntries = dependencyEntries,
            log = log,
        ).run()
        return reportDir
    }

    private fun readReport(reportDir: File): AppReport =
        Json.decodeFromString(reportDir.resolve("report.json").readText())

    private fun createApk(tempDir: File): File {
        val apk = tempDir.resolve("base.apk")
        ZipOutputStream(apk.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("assets/config.json"))
            zip.write("""{"enabled":true}""".toByteArray())
            zip.closeEntry()
        }
        return apk
    }
}
