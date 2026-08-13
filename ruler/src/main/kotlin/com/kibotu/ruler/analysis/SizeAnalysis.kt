package com.kibotu.ruler.analysis

import com.kibotu.ruler.analysis.apk.ApkCreator.Companion.BASE_FEATURE_NAME
import com.kibotu.ruler.analysis.apk.ApkParser
import com.kibotu.ruler.analysis.apk.ApkSanitizer
import com.kibotu.ruler.analysis.attribution.Attributor
import com.kibotu.ruler.analysis.attribution.StaticAttribution
import com.kibotu.ruler.analysis.dependency.DependencyComponent
import com.kibotu.ruler.analysis.dependency.DependencyEntry
import com.kibotu.ruler.analysis.dependency.DependencySanitizer
import com.kibotu.ruler.analysis.dependency.StaticComponent
import com.kibotu.ruler.analysis.ownership.OwnershipFileParser
import com.kibotu.ruler.analysis.ownership.OwnershipInfo
import com.kibotu.ruler.analysis.sanitizer.ClassNameSanitizer
import com.kibotu.ruler.analysis.sanitizer.ResourceNameSanitizer
import com.kibotu.ruler.analysis.verification.Verifier
import com.kibotu.ruler.model.AppFile
import com.kibotu.ruler.model.ComponentType
import com.kibotu.ruler.report.HtmlReporter
import com.kibotu.ruler.report.JsonReporter
import com.kibotu.ruler.report.ReportBuilder
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Measures an app bundle and writes the JSON and HTML reports.
 *
 * @param config What to measure and where to write the result.
 * @param dependencyEntries Every file of every runtime dependency, with its declaring component.
 * @param mappingFile R8, ProGuard, or DexGuard mapping file. Class names stay obfuscated without it.
 * @param resourceMappingFile DexGuard resource name mapping file.
 * @param unstrippedNativeFiles Unstripped `.so` files that Bloaty reads debug symbols from.
 * @param bloatyPath Path to the Bloaty executable. Ruler looks it up on `PATH` when this is null.
 * @param log Receives one line for each report that Ruler writes.
 */
class SizeAnalysis(
    private val config: RulerConfig,
    private val dependencyEntries: List<DependencyEntry>,
    private val mappingFile: File? = null,
    private val resourceMappingFile: File? = null,
    private val unstrippedNativeFiles: List<File> = emptyList(),
    private val bloatyPath: String? = null,
    private val log: (String) -> Unit = ::println,
) {

    private val classNameSanitizer = ClassNameSanitizer(mappingFile)

    fun run() {
        val files = filesPerFeature()
        val dependencies = DependencySanitizer(classNameSanitizer).sanitize(dependencyEntries)

        val defaultComponent = dependencies.values.flatten()
            .firstOrNull { it.name == config.projectPath }
            ?: DependencyComponent(config.projectPath, ComponentType.INTERNAL)

        val components = Attributor(defaultComponent, staticAttributions())
            .attribute(files.getValue(BASE_FEATURE_NAME), dependencies)
        val dynamicFeatures = files.filterKeys { it != BASE_FEATURE_NAME }

        writeReports(components, dynamicFeatures, ownershipInfo())

        Verifier(config.verificationConfig).verify(components.values.flatten())
    }

    /** Reads every APK of every feature and turns its entries into de-obfuscated app files. */
    private fun filesPerFeature(): Map<String, List<AppFile>> {
        val apkParser = ApkParser(unstrippedNativeFiles, bloatyPath)
        val apkSanitizer = ApkSanitizer(
            classNameSanitizer,
            ResourceNameSanitizer(resourceMappingFile),
        )
        return config.apkFilesMap.mapValues { (_, apks) ->
            apkSanitizer.sanitize(apks.flatMap(apkParser::parse))
        }
    }

    private fun writeReports(
        components: Map<DependencyComponent, List<AppFile>>,
        dynamicFeatures: Map<String, List<AppFile>>,
        ownershipInfo: OwnershipInfo?,
    ) {
        val report = ReportBuilder().build(
            appInfo = config.appInfo,
            components = components,
            features = dynamicFeatures,
            ownershipInfo = ownershipInfo,
            omitFileBreakdown = config.omitFileBreakdown,
            appProjectPath = config.projectPath,
        )

        val json = JsonReporter().write(report, config.reportDir)
        log("Wrote JSON report to ${json.toPath().toUri()}")

        val html = HtmlReporter().write(report, config.reportDir)
        log("Wrote HTML report to ${html.toPath().toUri()}")
    }

    /** Manual attribution rules, most specific first. */
    private fun staticAttributions(): List<StaticAttribution> {
        val file = config.staticDependenciesFile ?: return emptyList()
        return Json.decodeFromString<List<StaticComponent>>(file.readText()).map {
            StaticAttribution(
                path = Regex.escape(it.path).toRegex(),
                component = DependencyComponent(it.id, ComponentType.INTERNAL),
            )
        }
    }

    private fun ownershipInfo(): OwnershipInfo? {
        val file = config.ownershipFile ?: return null
        return OwnershipInfo(OwnershipFileParser().parse(file), config.defaultOwner)
    }
}
