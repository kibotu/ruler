package com.kibotu.ruler.report

import com.kibotu.ruler.model.AppComponent
import com.kibotu.ruler.model.AppFile
import com.kibotu.ruler.model.AppReport
import com.kibotu.ruler.model.ComponentType
import com.kibotu.ruler.model.FileType
import com.kibotu.ruler.model.Measurable
import com.kibotu.ruler.model.ResourceType
import kotlinx.serialization.Serializable

/**
 * Aggregates derived from an [AppReport].
 *
 * All arithmetic happens here. The HTML report reads these numbers and never computes its own.
 * Insights are not written to `report.json`, so that the JSON schema stays stable.
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
            val components = report.components
            val files = components.flatMap { it.files.orEmpty() }

            return ReportInsights(
                componentTypes = buckets(ComponentType.entries, components) { it.type },
                fileTypes = buckets(FileType.entries, files) { it.type },
                resourceTypes = buckets(
                    ResourceType.entries,
                    files.filter { it.type == FileType.RESOURCE },
                ) { it.resourceType ?: ResourceType.OTHER },
                topComponents = topBySize(components, AppComponent::name),
                topFiles = topBySize(files, AppFile::name),
                owners = owners(report),
                treemap = treemap(report),
            )
        }

        /** Totals per enum value, in declaration order. Values with no items are left out. */
        private fun <T : Measurable, K : Enum<K>> buckets(
            order: List<K>,
            items: List<T>,
            keyOf: (T) -> K,
        ): List<Bucket> {
            val grouped = items.groupBy(keyOf)
            return order.mapNotNull { key ->
                val group = grouped[key] ?: return@mapNotNull null
                Bucket(
                    label = key.name,
                    downloadSize = group.sumOf(Measurable::downloadSize),
                    installSize = group.sumOf(Measurable::installSize),
                    count = group.size,
                )
            }
        }

        /** The largest [TOP_N] items, once by download size and once by install size. */
        private fun <T : Measurable> topBySize(items: List<T>, nameOf: (T) -> String): Map<String, List<Entry>> {
            return Measurable.SizeType.entries.associate { sizeType ->
                val comparator = when (sizeType) {
                    Measurable.SizeType.DOWNLOAD ->
                        compareByDescending<T> { it.downloadSize }.thenByDescending { it.installSize }

                    Measurable.SizeType.INSTALL ->
                        compareByDescending<T> { it.installSize }.thenByDescending { it.downloadSize }
                }
                sizeType.name.lowercase() to items.sortedWith(comparator).take(TOP_N).map {
                    Entry(name = nameOf(it), downloadSize = it.downloadSize, installSize = it.installSize)
                }
            }
        }

        /**
         * Totals per owner.
         *
         * Files carry the finer-grained owner, so they are preferred. A report without a file
         * breakdown falls back to the owner of the component.
         */
        private fun owners(report: AppReport): List<OwnerBucket> {
            val hasFiles = report.components.any { !it.files.isNullOrEmpty() }
            val accumulators = mutableMapOf<String, Accumulator>()

            for (component in report.components) {
                val files = component.files
                if (hasFiles && files != null) {
                    files.forEach { file ->
                        val owner = file.owner ?: return@forEach
                        accumulators.getOrPut(owner, ::Accumulator).add(file, component.name)
                    }
                } else {
                    val owner = component.owner ?: continue
                    accumulators.getOrPut(owner, ::Accumulator)
                        .add(component, component.name, fileCount = files?.size ?: 0)
                }
            }

            return accumulators.map { (owner, totals) -> totals.toBucket(owner) }
                .sortedByDescending(OwnerBucket::downloadSize)
        }

        private class Accumulator {
            private var downloadSize = 0L
            private var installSize = 0L
            private var fileCount = 0
            private val components = mutableSetOf<String>()

            fun add(item: Measurable, component: String, fileCount: Int = 1) {
                downloadSize += item.downloadSize
                installSize += item.installSize
                this.fileCount += fileCount
                components += component
            }

            fun toBucket(owner: String) =
                OwnerBucket(owner, downloadSize, installSize, components.size, fileCount)
        }

        /** Components with their files as children, capped so that the treemap stays readable. */
        private fun treemap(report: AppReport): List<TreemapNode> {
            val components = report.components.sortedByDescending(AppComponent::downloadSize)

            val nodes = components.take(TREEMAP_MAX_COMPONENTS).map { component ->
                val files = component.files.orEmpty().sortedByDescending(AppFile::downloadSize)
                TreemapNode(
                    name = component.name,
                    downloadSize = component.downloadSize,
                    installSize = component.installSize,
                    children = files.take(TREEMAP_MAX_FILES).map {
                        TreemapNode(it.name, it.downloadSize, it.installSize)
                    } + remainderNode(files.drop(TREEMAP_MAX_FILES), "files"),
                )
            }

            return nodes + remainderNode(components.drop(TREEMAP_MAX_COMPONENTS), "components")
        }

        /** Rolls everything past the cap into one node, so that the totals still add up. */
        private fun remainderNode(remainder: List<Measurable>, label: String): List<TreemapNode> {
            if (remainder.isEmpty()) return emptyList()
            return listOf(
                TreemapNode(
                    name = "other (${remainder.size} $label)",
                    downloadSize = remainder.sumOf(Measurable::downloadSize),
                    installSize = remainder.sumOf(Measurable::installSize),
                ),
            )
        }
    }
}
