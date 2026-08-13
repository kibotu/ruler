package com.kibotu.ruler.report

import com.kibotu.ruler.analysis.AppInfo
import com.kibotu.ruler.analysis.dependency.DependencyComponent
import com.kibotu.ruler.analysis.ownership.OwnershipInfo
import com.kibotu.ruler.model.AppComponent
import com.kibotu.ruler.model.AppFile
import com.kibotu.ruler.model.AppReport
import com.kibotu.ruler.model.DynamicFeature
import com.kibotu.ruler.model.Measurable

/** Builds an [AppReport] from raw component and feature data. */
class ReportBuilder {
    private val largestFirst =
        compareByDescending(Measurable::downloadSize).thenByDescending(Measurable::installSize)

    fun build(
        appInfo: AppInfo,
        components: Map<DependencyComponent, List<AppFile>>,
        features: Map<String, List<AppFile>>,
        ownershipInfo: OwnershipInfo?,
        omitFileBreakdown: Boolean,
        appProjectPath: String? = null,
    ): AppReport {
        val allFiles = components.values.flatten()
        return AppReport(
            name = appInfo.applicationId,
            version = appInfo.versionName,
            variant = appInfo.variantName,
            downloadSize = allFiles.sumOf(AppFile::downloadSize),
            installSize = allFiles.sumOf(AppFile::installSize),
            components = components.map { (component, files) ->
                // The application module owns itself, unless the ownership file says otherwise.
                val isAppComponent = component.name == appProjectPath
                val declaredOwners = ownershipInfo?.owners(component.name, component.type)
                val (owner, additionalOwners) = splitOwners(
                    declaredOwners ?: listOf(APP_OWNER).takeIf { isAppComponent },
                )
                AppComponent(
                    name = component.name,
                    type = component.type,
                    downloadSize = files.sumOf(AppFile::downloadSize),
                    installSize = files.sumOf(AppFile::installSize),
                    owner = owner,
                    additionalOwners = additionalOwners,
                    internal = ownershipInfo?.internalOverride(component.name, component.type)
                        ?: true.takeIf { isAppComponent },
                    files = reportFiles(files, omitFileBreakdown) { file ->
                        ownershipInfo?.let { it.fileOwners(file.name) ?: declaredOwners }
                    },
                )
            }.sortedWith(largestFirst),
            dynamicFeatures = features.map { (feature, files) ->
                val declaredOwners = ownershipInfo?.owners(feature)
                val (owner, additionalOwners) = splitOwners(declaredOwners)
                DynamicFeature(
                    name = feature,
                    downloadSize = files.sumOf(AppFile::downloadSize),
                    installSize = files.sumOf(AppFile::installSize),
                    owner = owner,
                    additionalOwners = additionalOwners,
                    internal = ownershipInfo?.internalOverride(feature),
                    files = reportFiles(files, omitFileBreakdown) { file ->
                        ownershipInfo?.let { it.fileOwners(file.name) ?: declaredOwners }
                    },
                )
            }.sortedWith(largestFirst),
        )
    }

    private fun reportFiles(
        files: List<AppFile>,
        omitFileBreakdown: Boolean,
        ownersOf: (AppFile) -> List<String>?,
    ): List<AppFile>? {
        if (omitFileBreakdown) return null
        return files.map { file ->
            val (owner, additionalOwners) = splitOwners(ownersOf(file))
            file.copy(owner = owner, additionalOwners = additionalOwners)
        }.sortedWith(largestFirst)
    }

    /** The first owner is the primary one. The rest are reported alongside it. */
    private fun splitOwners(owners: List<String>?): Pair<String?, List<String>?> {
        if (owners.isNullOrEmpty()) return null to null
        return owners.first() to owners.drop(1).ifEmpty { null }
    }

    companion object {
        const val APP_OWNER = "App"
    }
}
