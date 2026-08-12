package com.kibotu.ruler.common.report

import com.kibotu.ruler.common.dependency.DependencyComponent
import com.kibotu.ruler.common.models.AppInfo
import com.kibotu.ruler.common.ownership.OwnershipInfo
import com.kibotu.ruler.models.AppComponent
import com.kibotu.ruler.models.AppFile
import com.kibotu.ruler.models.AppReport
import com.kibotu.ruler.models.DynamicFeature
import com.kibotu.ruler.models.Measurable

/** Builds an [AppReport] from raw component and feature data. */
class ReportBuilder {
    private val comparator = compareBy(Measurable::downloadSize).thenBy(Measurable::installSize)

    @Suppress("LongParameterList")
    fun build(
        appInfo: AppInfo,
        components: Map<DependencyComponent, List<AppFile>>,
        features: Map<String, List<AppFile>>,
        ownershipInfo: OwnershipInfo?,
        omitFileBreakdown: Boolean,
        appProjectPath: String? = null,
    ): AppReport = AppReport(
        name = appInfo.applicationId,
        version = appInfo.versionName,
        variant = appInfo.variantName,
        downloadSize = components.values.flatten().sumOf(AppFile::downloadSize),
        installSize = components.values.flatten().sumOf(AppFile::installSize),
        components = components.map { (component, files) ->
            val isAppComponent = appProjectPath != null && component.name == appProjectPath
            AppComponent(
                name = component.name,
                type = component.type,
                downloadSize = files.sumOf(AppFile::downloadSize),
                installSize = files.sumOf(AppFile::installSize),
                owner = ownershipInfo?.getOwner(component.name, component.type)
                    ?: if (isAppComponent) APP_OWNER else null,
                internal = ownershipInfo?.getInternal(component.name, component.type)
                    ?: if (isAppComponent) true else null,
                files = mapReportFiles(
                    files = files,
                    omitFileBreakdown = omitFileBreakdown,
                    ownerForFile = { file ->
                        ownershipInfo?.getOwner(file.name, component.name, component.type)
                    },
                ),
            )
        }.sortedWith(comparator.reversed()),
        dynamicFeatures = features.map { (feature, files) ->
            DynamicFeature(
                name = feature,
                downloadSize = files.sumOf(AppFile::downloadSize),
                installSize = files.sumOf(AppFile::installSize),
                owner = ownershipInfo?.getOwner(feature),
                internal = ownershipInfo?.getInternal(feature),
                files = mapReportFiles(
                    files = files,
                    omitFileBreakdown = omitFileBreakdown,
                    ownerForFile = { file -> ownershipInfo?.getOwner(file.name, feature) },
                ),
            )
        }.sortedWith(comparator.reversed()),
    )

    private fun mapReportFiles(
        files: List<AppFile>,
        omitFileBreakdown: Boolean,
        ownerForFile: (AppFile) -> String?,
    ): List<AppFile>? {
        if (omitFileBreakdown) return null
        return files.map { file ->
            AppFile(
                name = file.name,
                type = file.type,
                downloadSize = file.downloadSize,
                installSize = file.installSize,
                owner = ownerForFile(file),
                resourceType = file.resourceType,
            )
        }.sortedWith(comparator.reversed())
    }

    companion object {
        const val APP_OWNER = "App"
    }
}
