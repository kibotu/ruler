package com.kibotu.ruler.common.apk

import com.android.tools.apk.analyzer.ApkSizeCalculator
import com.android.tools.apk.analyzer.dex.DexFiles
import com.kibotu.ruler.common.bloaty.Bloaty
import java.io.File
import java.util.zip.ZipFile

/** Responsible for parsing and extracting entries from APK files. */
class ApkParser(
    private val unstrippedNativeLibraryPaths: List<File> = emptyList(),
    private val bloatyPath: String? = null
) {

    /** Parses and returns the list of entries contained in the given [apkFile]. */
    fun parse(apkFile: File) : List<ApkEntry> {
        val sizeCalculator = ApkSizeCalculator.getDefault()
        val downloadSizePerFile = sizeCalculator.getDownloadSizePerFile(apkFile.toPath())
        val installSizePerFile = sizeCalculator.getInfoPerFile(apkFile.toPath())
        val bloaty = Bloaty(bloatyPath)
        val apkEntries = mutableListOf<ApkEntry>()
        ZipFile(apkFile).use { zipFile ->
            zipFile.entries().iterator().forEach { zipEntry ->
                val name = "/${zipEntry.name}"
                val downloadSize = downloadSizePerFile.getValue(name)
                val installSize = installSizePerFile.getValue(name).size

                apkEntries += when {
                    isDexEntry(name) -> {
                        val bytes = zipFile.getInputStream(zipEntry).readBytes()
                        ApkEntry.Dex(name, downloadSize, installSize, parseDexEntry(bytes))
                    }
                    isNativeLibraryEntry(name) -> {
                        val bytes = zipFile.getInputStream(zipEntry).readBytes()
                        val native = ApkEntry.NativeLibrary(
                            name,
                            downloadSize,
                            installSize,
                            bloaty.parseNativeLibraryEntry(
                                bytes,
                                debugFileForNativeLibrary(entryName = name)
                            )
                        )
                        native
                    }
                    // Some build systems emit resources under /lib/res/ instead of /res/.
                    else -> ApkEntry.Default(
                        name.replace("/lib/res/", "/res/"),
                        downloadSize,
                        installSize
                    )
                }
            }
        }
        return apkEntries
    }

    /** Parses a DEX entry (represented by its [bytes]) and returns a list of all contained class entries. */
    private fun parseDexEntry(bytes: ByteArray): List<ApkEntry> {
        val dexFile = DexFiles.getDexFile(bytes)
        return dexFile.getClasses().map { classDef ->
            ApkEntry.Default(classDef.getType(), classDef.getSize().toLong(), classDef.getSize().toLong())
        }
    }

    /** Checks if a certain [entryName] represents a DEX entry. */
    private fun isDexEntry(entryName: String): Boolean {
        return entryName.endsWith(".dex", ignoreCase = true)
    }

    /** Checks if a certain [entryName] represents a native library entry. */
    private fun isNativeLibraryEntry(entryName: String): Boolean {
        return entryName.endsWith(".so", ignoreCase = true)
    }

    /** Get the file containing the Unstripped file names to properly parse the native library. */
    private fun debugFileForNativeLibrary(entryName: String): File? {
        val entryFileName = File(entryName).nameWithoutExtension
        return unstrippedNativeLibraryPaths.find {
            it.name.contains(entryFileName)
        }
    }
}
