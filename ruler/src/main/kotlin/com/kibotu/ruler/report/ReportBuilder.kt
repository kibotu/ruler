package com.kibotu.ruler.report

import com.kibotu.ruler.analysis.dependency.DependencyComponent
import com.kibotu.ruler.analysis.AppInfo
import com.kibotu.ruler.analysis.ownership.OwnershipInfo
import com.kibotu.ruler.model.AppComponent
import com.kibotu.ruler.model.AppFile
import com.kibotu.ruler.model.AppReport
import com.kibotu.ruler.model.DynamicFeature
import com.kibotu.ruler.model.Measurable

/** Builds an [AppReport] from raw component and feature data. */
class ReportBuilder {
    private val comparator = compareBy(Measurable::downloadSize).thenBy(Measurable::installSize)

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
            val componentOwners = ownershipInfo?.getOwners(component.name, component.type)
                ?: if (isAppComponent) listOf(APP_OWNER) else null
            val (componentOwner, componentAdditionalOwners) = splitOwners(componentOwners)
            AppComponent(
                name = component.name,
                type = component.type,
                downloadSize = files.sumOf(AppFile::downloadSize),
                installSize = files.sumOf(AppFile::installSize),
                owner = componentOwner,
                additionalOwners = componentAdditionalOwners,
                internal = ownershipInfo?.getInternal(component.name, component.type)
                    ?: if (isAppComponent) true else null,
                files = mapReportFiles(
                    files = files,
                    omitFileBreakdown = omitFileBreakdown,
                    ownersForFile = { file ->
                        ownershipInfo?.getOwners(file.name, component.name, component.type)
                    },
                ),
            )
        }.sortedWith(comparator.reversed()),
        dynamicFeatures = features.map { (feature, files) ->
            val (featureOwner, featureAdditionalOwners) = splitOwners(ownershipInfo?.getOwners(feature))
            DynamicFeature(
                name = feature,
                downloadSize = files.sumOf(AppFile::downloadSize),
                installSize = files.sumOf(AppFile::installSize),
                owner = featureOwner,
                additionalOwners = featureAdditionalOwners,
                internal = ownershipInfo?.getInternal(feature),
                files = mapReportFiles(
                    files = files,
                    omitFileBreakdown = omitFileBreakdown,
                    ownersForFile = { file -> ownershipInfo?.getOwners(file.name, feature) },
                ),
            )
        }.sortedWith(comparator.reversed()),
    )

    private fun mapReportFiles(
        files: List<AppFile>,
        omitFileBreakdown: Boolean,
        ownersForFile: (AppFile) -> List<String>?,
    ): List<AppFile>? {
        if (omitFileBreakdown) return null
        return files.map { file ->
            val (owner, additionalOwners) = splitOwners(ownersForFile(file))
            AppFile(
                name = file.name,
                type = file.type,
                downloadSize = file.downloadSize,
                installSize = file.installSize,
                owner = owner,
                additionalOwners = additionalOwners,
                resourceType = file.resourceType,
            )
        }.sortedWith(comparator.reversed())
    }

    private fun splitOwners(owners: List<String>?): Pair<String?, List<String>?> {
        if (owners.isNullOrEmpty()) return null to null
        val additionalOwners = owners.drop(1).ifEmpty { null }
        return owners.first() to additionalOwners
    }

    companion object {
        const val APP_OWNER = "App"
    }
}
