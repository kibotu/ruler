package com.kibotu.ruler.analysis.apk

import com.kibotu.ruler.analysis.sanitizer.ClassNameSanitizer
import com.kibotu.ruler.analysis.sanitizer.ResourceNameSanitizer
import com.kibotu.ruler.model.AppFile
import com.kibotu.ruler.model.FileType
import com.kibotu.ruler.model.ResourceType

/** Turns raw APK entries into app files: removes, merges, de-obfuscates, and resizes them. */
class ApkSanitizer(
    private val classNameSanitizer: ClassNameSanitizer,
    private val resourceNameSanitizer: ResourceNameSanitizer,
) {

    /** Each entry goes to the first bucket that accepts it. */
    fun sanitize(entries: List<ApkEntry>): List<AppFile> {
        val buckets = listOf(
            NativeLibraryBucket(),
            DexBucket(),
            AndroidManifestBucket(),
            BundletoolBucket(),
            ResourcesArscBucket(),
            ResourceBucket(),
            RemainderBucket(),
        )

        entries.forEach { entry ->
            buckets.first { it.accepts(entry) }.add(entry)
        }

        return buckets.flatMap(Bucket::sanitize)
    }

    private abstract class Bucket {
        protected val entries = mutableListOf<ApkEntry>()

        abstract fun accepts(entry: ApkEntry): Boolean

        abstract fun sanitize(): List<AppFile>

        fun add(entry: ApkEntry) = entries.add(entry)
    }

    /** Unpacks DEX files into their classes. Class sizes sum to more than the compressed DEX file
     *  itself, so each gets a proportional share of it and the total stays right. */
    private inner class DexBucket : Bucket() {
        override fun accepts(entry: ApkEntry) = entry is ApkEntry.Dex

        override fun sanitize(): List<AppFile> {
            val dexFiles = entries.filterIsInstance<ApkEntry.Dex>()
            val classSize = dexFiles.sumOf { dex -> dex.classes.sumOf(ApkEntry::installSize) }
            if (classSize == 0L) return emptyList()

            val downloadSize = dexFiles.sumOf(ApkEntry::downloadSize)
            val installSize = dexFiles.sumOf(ApkEntry::installSize)

            return dexFiles.flatMap { dex ->
                dex.classes.map { entry ->
                    AppFile(
                        name = classNameSanitizer.sanitize(entry.name),
                        type = FileType.CLASS,
                        downloadSize = entry.downloadSize * downloadSize / classSize,
                        installSize = entry.installSize * installSize / classSize,
                    )
                }
            }
        }
    }

    /** Splits a native library's size across its compile units, as [DexBucket] does. */
    private class NativeLibraryBucket : Bucket() {
        private val metadataRegex = Regex("\\[section.*?]")

        override fun accepts(entry: ApkEntry) = entry is ApkEntry.NativeLibrary && entry.units.isNotEmpty()

        override fun sanitize(): List<AppFile> {
            return entries.filterIsInstance<ApkEntry.NativeLibrary>().flatMap { library ->
                val unitSize = library.units.sumOf(ApkEntry::installSize)
                if (unitSize == 0L) return@flatMap emptyList()

                library.units.map { unit ->
                    AppFile(
                        // Section metadata has no path of its own, so qualify it with the library.
                        name = if (metadataRegex.matches(unit.name)) "${library.name}/${unit.name}" else unit.name,
                        type = FileType.NATIVE_FILE,
                        downloadSize = unit.downloadSize * library.downloadSize / unitSize,
                        installSize = unit.installSize * library.installSize / unitSize,
                    )
                }
            }
        }
    }

    /** Keeps the base APK's manifest, which is the largest of the per-split manifests. */
    private class AndroidManifestBucket : Bucket() {
        override fun accepts(entry: ApkEntry) = entry.name == "/AndroidManifest.xml"

        override fun sanitize(): List<AppFile> {
            val entry = entries.maxByOrNull(ApkEntry::installSize) ?: return emptyList()
            return listOf(AppFile("/AndroidManifest.xml", FileType.OTHER, entry.downloadSize, entry.installSize))
        }
    }

    /** Discards the files bundletool adds, which the APKs from the Play Store do not have. */
    private class BundletoolBucket : Bucket() {
        private val splitsRegex = Regex("/res/xml/splits\\d+\\.xml")

        override fun accepts(entry: ApkEntry) =
            entry.name == "/META-INF/MANIFEST.MF" || entry.name.matches(splitsRegex)

        override fun sanitize() = emptyList<AppFile>()
    }

    /** Merges the compiled resource tables of all APKs into one file. */
    private class ResourcesArscBucket : Bucket() {
        override fun accepts(entry: ApkEntry) = entry.name == "/resources.arsc"

        override fun sanitize(): List<AppFile> {
            if (entries.isEmpty()) return emptyList()
            return listOf(
                AppFile(
                    name = "/resources.arsc",
                    type = FileType.OTHER,
                    downloadSize = entries.sumOf(ApkEntry::downloadSize),
                    installSize = entries.sumOf(ApkEntry::installSize),
                ),
            )
        }
    }

    /** De-obfuscates resource names, which DexGuard obfuscates. */
    private inner class ResourceBucket : Bucket() {
        override fun accepts(entry: ApkEntry) = entry.name.startsWith("/res/")

        override fun sanitize() = entries.map { entry ->
            val name = resourceNameSanitizer.sanitize(entry.name)
            AppFile(name, FileType.RESOURCE, entry.downloadSize, entry.installSize, resourceType = resourceTypeOf(name))
        }
    }

    /** Assigns a file type to everything that no other bucket claimed. */
    private class RemainderBucket : Bucket() {
        override fun accepts(entry: ApkEntry) = true

        override fun sanitize() = entries.map { entry ->
            val type = when {
                entry.name.startsWith("/assets/") -> FileType.ASSET
                entry.name.startsWith("/lib/") -> FileType.NATIVE_LIB
                else -> FileType.OTHER
            }
            AppFile(entry.name, type, entry.downloadSize, entry.installSize)
        }
    }

    private companion object {
        fun resourceTypeOf(name: String): ResourceType? = when {
            name.startsWith("/res/drawable") -> ResourceType.DRAWABLE
            name.startsWith("/res/layout") -> ResourceType.LAYOUT
            name.startsWith("/res/raw") -> ResourceType.RAW
            name.startsWith("/res/values") -> ResourceType.VALUES
            name.startsWith("/res/font") -> ResourceType.FONT
            name.startsWith("/res/") -> ResourceType.OTHER
            else -> null
        }
    }
}
