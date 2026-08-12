package com.kibotu.ruler.common

import com.kibotu.ruler.common.apk.ApkCreator.Companion.BASE_FEATURE_NAME
import com.kibotu.ruler.common.apk.ApkParser
import com.kibotu.ruler.common.apk.ApkSanitizer
import com.kibotu.ruler.common.attribution.Attributor
import com.kibotu.ruler.common.dependency.DependencyComponent
import com.kibotu.ruler.common.dependency.StaticComponent
import com.kibotu.ruler.common.models.RulerConfig
import com.kibotu.ruler.common.ownership.OwnershipFileParser
import com.kibotu.ruler.common.ownership.OwnershipInfo
import com.kibotu.ruler.common.report.HtmlReporter
import com.kibotu.ruler.common.report.JsonReporter
import com.kibotu.ruler.common.report.ReportBuilder
import com.kibotu.ruler.common.report.ReportInsights
import com.kibotu.ruler.common.sanitizer.ClassNameSanitizer
import com.kibotu.ruler.common.sanitizer.ResourceNameSanitizer
import com.kibotu.ruler.common.util.toEscapeCharRegex
import com.kibotu.ruler.common.verification.Verificator
import com.kibotu.ruler.models.AppFile
import com.kibotu.ruler.models.ComponentType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

/** Fallback component for Kotlin stdlib classes not present in the dependency graph. */
private const val KOTLIN_STDLIB_COMPONENT = "kotlin"

@Suppress("TooManyFunctions")
interface BaseRulerTask {

    fun print(content: String)
    fun provideMappingFile(): File?
    fun provideResourceMappingFile(): File?
    fun rulerConfig(): RulerConfig
    fun provideUnstrippedLibraryFiles(): List<File>
    fun provideBloatyPath(): String?

    private val rulerConfig: RulerConfig
        get() = rulerConfig()

    fun provideDependencies(): Map<String, List<DependencyComponent>>

    fun provideStaticDependencies(): Map<Regex, List<DependencyComponent>> {
        val staticComponent = rulerConfig.staticDependenciesFile ?: return emptyMap()
        val jsonString = staticComponent.readText()
        val itemList = Json.decodeFromString<List<StaticComponent>>(jsonString)
        return itemList.associate {
            it.path.toEscapeCharRegex() to listOf(DependencyComponent(it.id, ComponentType.INTERNAL))
        }
    }

    fun run() {
        val files = getFilesFromBundle() // Get all relevant files from the provided bundle
        val dependencies = provideDependencies() + mapOf(
            KOTLIN_STDLIB_COMPONENT to listOf(
                DependencyComponent(KOTLIN_STDLIB_COMPONENT, ComponentType.INTERNAL),
            ),
        )
        val mainFiles = files.getValue(BASE_FEATURE_NAME)
        val featureFiles = files.filter { (feature, _) -> feature != BASE_FEATURE_NAME }

        val defaultComponent = dependencies.values.flatten()
            .firstOrNull { it.name == rulerConfig.projectPath }
            ?: DependencyComponent(rulerConfig.projectPath, ComponentType.INTERNAL)

        // Attribute main APK bundle entries and group into components
        val attributor =
            Attributor(defaultComponent, provideStaticDependencies())
        val components = attributor.attribute(mainFiles, dependencies)

        val ownershipInfo = getOwnershipInfo() // Get ownership information for all components
        generateReports(components, featureFiles, ownershipInfo)

        val verificator = rulerConfig.verificationConfig.let(::Verificator)
        verificator.verify(components.values.flatten())
    }

    private fun getFilesFromBundle(): Map<String, List<AppFile>> {
        val apkParser = ApkParser(provideUnstrippedLibraryFiles(), provideBloatyPath())
        val classNameSanitizer = ClassNameSanitizer(provideMappingFile())
        val resourceNameSanitizer = ResourceNameSanitizer(provideResourceMappingFile())
        val apkSanitizer = ApkSanitizer(classNameSanitizer, resourceNameSanitizer)
        val config = rulerConfig()
        return config.apkFilesMap.mapValues { (_, apks) ->
            val entries = apks.flatMap(apkParser::parse)
            apkSanitizer.sanitize(entries)
        }
    }

    private fun getOwnershipInfo(): OwnershipInfo? {
        val ownershipFile = rulerConfig.ownershipFile ?: return null
        val ownershipFileParser = OwnershipFileParser()
        val ownershipEntries = ownershipFileParser.parse(ownershipFile)

        return OwnershipInfo(ownershipEntries, rulerConfig.defaultOwner)
    }

    private fun generateReports(
        components: Map<DependencyComponent, List<AppFile>>,
        features: Map<String, List<AppFile>>,
        ownershipInfo: OwnershipInfo?,
    ) {
        val reportDir = rulerConfig.reportDir

        val reportBuilder = ReportBuilder()
        val report = reportBuilder.build(
            rulerConfig.appInfo,
            components,
            features,
            ownershipInfo,
            rulerConfig.omitFileBreakdown,
            rulerConfig.projectPath,
        )

        val jsonReporter = JsonReporter()
        val jsonReport = jsonReporter.write(report, reportDir)
        print("Wrote JSON report to ${jsonReport.toPath().toUri()}")

        val insights = ReportInsights.from(report)
        val htmlReporter = HtmlReporter()
        val htmlReport = htmlReporter.write(report, insights, reportDir)
        print("Wrote HTML report to ${htmlReport.toPath().toUri()}")
    }
}
