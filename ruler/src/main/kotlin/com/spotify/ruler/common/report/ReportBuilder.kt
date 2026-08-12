package com.spotify.ruler.common.report

import com.spotify.ruler.common.dependency.DependencyComponent
import com.spotify.ruler.common.models.AppInfo
import com.spotify.ruler.common.ownership.OwnershipInfo
import com.spotify.ruler.models.AppComponent
import com.spotify.ruler.models.AppFile
import com.spotify.ruler.models.AppReport
import com.spotify.ruler.models.DynamicFeature
import com.spotify.ruler.models.Measurable

/**
 * Builds an [AppReport] from raw component and feature data.
 * Extracted from the old JsonReporter — same comparator, same descending sort, same omitFileBreakdown handling.
 */
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
            val owner = ownershipInfo?.getOwner(component.name, component.type)
            val internal = ownershipInfo?.getInternal(component.name, component.type)
            val isAppComponent = appProjectPath != null && component.name == appProjectPath
            AppComponent(
                name = component.name,
                type = component.type,
                downloadSize = files.sumOf(AppFile::downloadSize),
                installSize = files.sumOf(AppFile::installSize),
                owner = owner ?: if (isAppComponent) APP_OWNER else null,
                internal = internal ?: if (isAppComponent) true else null,
                files = if (omitFileBreakdown) {
                    null
                } else {
                    files.map { file ->
                        AppFile(
                            name = file.name,
                            type = file.type,
                            downloadSize = file.downloadSize,
                            installSize = file.installSize,
                            owner = ownershipInfo?.getOwner(file.name, component.name, component.type),
                            resourceType = file.resourceType,
                        )
                    }.sortedWith(comparator.reversed())
                }
            )
        }.sortedWith(comparator.reversed()),
        dynamicFeatures = features.map { (feature, files) ->
            DynamicFeature(
                name = feature,
                downloadSize = files.sumOf(AppFile::downloadSize),
                installSize = files.sumOf(AppFile::installSize),
                owner = ownershipInfo?.getOwner(feature),
                internal = ownershipInfo?.getInternal(feature),
                files = if (omitFileBreakdown) {
                    null
                } else {
                    files.map { file ->
                        AppFile(
                            name = file.name,
                            type = file.type,
                            downloadSize = file.downloadSize,
                            installSize = file.installSize,
                            owner = ownershipInfo?.getOwner(file.name, feature),
                            resourceType = file.resourceType,
                        )
                    }.sortedWith(comparator.reversed())
                }
            )
        }.sortedWith(comparator.reversed()),
    )

    companion object {
        const val APP_OWNER = "App"
    }
}
