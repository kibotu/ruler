package com.spotify.ruler.common.report

import com.spotify.ruler.models.AppFile
import com.spotify.ruler.models.AppReport
import com.spotify.ruler.models.ComponentType
import com.spotify.ruler.models.FileType
import com.spotify.ruler.models.Measurable
import com.spotify.ruler.models.ResourceType
import kotlinx.serialization.Serializable

/**
 * Pre-computed aggregates derived from [AppReport].
 * All arithmetic lives here in Kotlin; the JS frontend never re-derives a number.
 *
 * Deliberately not written to report.json — the JSON schema stays unchanged.
 * The HTML template embeds both the report and the insights.
 */
@Serializable
data class ReportInsights(
    val componentTypes: List<Bucket>,
    val fileTypes: List<Bucket>,
    val resourceTypes: List<Bucket>,
    val topComponents: Map<String, List<Entry>>,
    val topFiles: Map<String, List<Entry>>,
    val owners: List<OwnerBucket>,
    val treemap: List<TreemapNode>,
) {
    @Serializable
    data class Bucket(
        val label: String,
        val downloadSize: Long,
        val installSize: Long,
        val count: Int,
    )

    @Serializable
    data class Entry(
        val name: String,
        val component: String? = null,
        val downloadSize: Long,
        val installSize: Long,
    )

    @Serializable
    data class OwnerBucket(
        val owner: String,
        val downloadSize: Long,
        val installSize: Long,
        val componentCount: Int,
        val fileCount: Int,
    )

    @Serializable
    data class TreemapNode(
        val name: String,
        val downloadSize: Long,
        val installSize: Long,
        val children: List<TreemapNode> = emptyList(),
    )

    companion object {
        private const val TOP_N = 20
        private const val TREEMAP_MAX_COMPONENTS = 50
        private const val TREEMAP_MAX_FILES = 30

        fun from(report: AppReport): ReportInsights {
            val allComponents = report.components
            val allFiles = allComponents.flatMap { it.files ?: emptyList() }

            return ReportInsights(
                componentTypes = computeComponentTypes(allComponents),
                fileTypes = computeFileTypes(allFiles),
                resourceTypes = computeResourceTypes(allFiles),
                topComponents = computeTopComponents(allComponents),
                topFiles = computeTopFiles(allFiles),
                owners = computeOwners(report),
                treemap = computeTreemap(report),
            )
        }

        private fun computeComponentTypes(
            components: List<com.spotify.ruler.models.AppComponent>,
        ): List<Bucket> {
            val byType = components.groupBy { it.type }
            return ComponentType.entries.map { type ->
                val group = byType[type] ?: emptyList()
                Bucket(
                    label = type.name,
                    downloadSize = group.sumOf { it.downloadSize },
                    installSize = group.sumOf { it.installSize },
                    count = group.size,
                )
            }.filter { it.count > 0 }
        }

        private fun computeFileTypes(files: List<AppFile>): List<Bucket> {
            val byType = files.groupBy { it.type }
            return FileType.entries.map { type ->
                val group = byType[type] ?: emptyList()
                Bucket(
                    label = type.name,
                    downloadSize = group.sumOf { it.downloadSize },
                    installSize = group.sumOf { it.installSize },
                    count = group.size,
                )
            }.filter { it.count > 0 }
        }

        private fun computeResourceTypes(files: List<AppFile>): List<Bucket> {
            val resourceFiles = files.filter { it.type == FileType.RESOURCE }
            val byType = resourceFiles.groupBy { it.resourceType ?: ResourceType.OTHER }
            return ResourceType.entries.map { type ->
                val group = byType[type] ?: emptyList()
                Bucket(
                    label = type.name,
                    downloadSize = group.sumOf { it.downloadSize },
                    installSize = group.sumOf { it.installSize },
                    count = group.size,
                )
            }.filter { it.count > 0 }
        }

        private fun computeTopComponents(
            components: List<com.spotify.ruler.models.AppComponent>,
        ): Map<String, List<Entry>> {
            return Measurable.SizeType.entries.associate { sizeType ->
                val comparator = when (sizeType) {
                    Measurable.SizeType.DOWNLOAD ->
                        compareByDescending<com.spotify.ruler.models.AppComponent> { it.downloadSize }
                            .thenByDescending { it.installSize }
                    Measurable.SizeType.INSTALL ->
                        compareByDescending<com.spotify.ruler.models.AppComponent> { it.installSize }
                            .thenByDescending { it.downloadSize }
                }
                val sorted = components.sortedWith(comparator)
                val key = sizeType.name.lowercase()
                key to sorted.take(TOP_N).map { comp ->
                    Entry(
                        name = comp.name,
                        downloadSize = comp.downloadSize,
                        installSize = comp.installSize,
                    )
                }
            }
        }

        private fun computeTopFiles(files: List<AppFile>): Map<String, List<Entry>> {
            return Measurable.SizeType.entries.associate { sizeType ->
                val comparator = when (sizeType) {
                    Measurable.SizeType.DOWNLOAD ->
                        compareByDescending<AppFile> { it.downloadSize }
                            .thenByDescending { it.installSize }
                    Measurable.SizeType.INSTALL ->
                        compareByDescending<AppFile> { it.installSize }
                            .thenByDescending { it.downloadSize }
                }
                val sorted = files.sortedWith(comparator)
                val key = sizeType.name.lowercase()
                key to sorted.take(TOP_N).map { file ->
                    Entry(
                        name = file.name,
                        downloadSize = file.downloadSize,
                        installSize = file.installSize,
                    )
                }
            }
        }

        private fun computeOwners(report: AppReport): List<OwnerBucket> {
            val allFiles = report.components.flatMap { it.files ?: emptyList() }

            // File-level ownership when files are present, component-level otherwise
            val componentOwnerMap = report.components.associateBy { it.name }

            data class OwnerAccumulator(
                var downloadSize: Long = 0,
                var installSize: Long = 0,
                var componentCount: Int = 0,
                var fileCount: Int = 0,
            )

            val owners = mutableMapOf<String, OwnerAccumulator>()

            for (component in report.components) {
                val files = component.files
                if (allFiles.isNotEmpty() && files != null) {
                    // File-level: group files by their owner
                    for (file in files) {
                        val owner = file.owner ?: continue
                        val acc = owners.getOrPut(owner) { OwnerAccumulator() }
                        acc.downloadSize += file.downloadSize
                        acc.installSize += file.installSize
                        acc.fileCount++
                        // Count each file as belonging to a component (first file of a component increments componentCount)
                        if (files.indexOf(file) == 0) {
                            acc.componentCount++
                        }
                    }
                } else {
                    // Component-level
                    val owner = component.owner ?: continue
                    val acc = owners.getOrPut(owner) { OwnerAccumulator() }
                    acc.downloadSize += component.downloadSize
                    acc.installSize += component.installSize
                    acc.componentCount++
                    acc.fileCount += (component.files?.size ?: 0)
                }
            }

            return owners.map { (owner, acc) ->
                OwnerBucket(
                    owner = owner,
                    downloadSize = acc.downloadSize,
                    installSize = acc.installSize,
                    componentCount = acc.componentCount,
                    fileCount = acc.fileCount,
                )
            }.sortedByDescending { it.downloadSize }
        }

        private fun computeTreemap(report: AppReport): List<TreemapNode> {
            val components = report.components.sortedByDescending { it.downloadSize }
            val top = components.take(TREEMAP_MAX_COMPONENTS)
            val remainder = components.drop(TREEMAP_MAX_COMPONENTS)

            val nodes = top.map { comp ->
                val files = (comp.files ?: emptyList()).sortedByDescending { it.downloadSize }
                val topFiles = files.take(TREEMAP_MAX_FILES)
                val otherFiles = files.drop(TREEMAP_MAX_FILES)

                val fileChildren = topFiles.map { file ->
                    TreemapNode(
                        name = file.name,
                        downloadSize = file.downloadSize,
                        installSize = file.installSize,
                    )
                } + if (otherFiles.isNotEmpty()) {
                    listOf(
                        TreemapNode(
                            name = "other (${otherFiles.size} files)",
                            downloadSize = otherFiles.sumOf { it.downloadSize },
                            installSize = otherFiles.sumOf { it.installSize },
                        )
                    )
                } else {
                    emptyList()
                }

                TreemapNode(
                    name = comp.name,
                    downloadSize = comp.downloadSize,
                    installSize = comp.installSize,
                    children = fileChildren,
                )
            }

            return if (remainder.isNotEmpty()) {
                nodes + TreemapNode(
                    name = "other (${remainder.size} components)",
                    downloadSize = remainder.sumOf { it.downloadSize },
                    installSize = remainder.sumOf { it.installSize },
                )
            } else {
                nodes
            }
        }
    }
}
