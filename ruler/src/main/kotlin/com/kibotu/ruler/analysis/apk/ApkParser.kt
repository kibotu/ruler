package com.kibotu.ruler.analysis.apk

import com.android.tools.apk.analyzer.ApkSizeCalculator
import com.android.tools.apk.analyzer.dex.DexFiles
import com.kibotu.ruler.analysis.bloaty.Bloaty
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/** Reads the entries of an APK file. */
class ApkParser(
    private val unstrippedNativeFiles: List<File> = emptyList(),
    bloatyPath: String? = null,
) {

    private val bloaty = Bloaty(bloatyPath)

    /** Returns every entry of [apkFile], with DEX files unpacked into classes. */
    fun parse(apkFile: File): List<ApkEntry> {
        val sizeCalculator = ApkSizeCalculator.getDefault()
        val downloadSizes = sizeCalculator.getDownloadSizePerFile(apkFile.toPath())
        val installSizes = sizeCalculator.getInfoPerFile(apkFile.toPath())

        return ZipFile(apkFile).use { zipFile ->
            zipFile.entries().asSequence()
                .filterNot(ZipEntry::isDirectory)
                .mapNotNull { zipEntry ->
                    val name = "/${zipEntry.name}"
                    // The size calculator skips entries it does not understand. Skip them too,
                    // rather than fail the whole build over one unknown entry.
                    val downloadSize = downloadSizes[name] ?: return@mapNotNull null
                    val installSize = installSizes[name]?.size ?: return@mapNotNull null

                    when {
                        name.endsWith(".dex", ignoreCase = true) -> ApkEntry.Dex(
                            name = name,
                            downloadSize = downloadSize,
                            installSize = installSize,
                            classes = parseDex(zipFile.getInputStream(zipEntry).readBytes()),
                        )

                        name.endsWith(".so", ignoreCase = true) -> ApkEntry.NativeLibrary(
                            name = name,
                            downloadSize = downloadSize,
                            installSize = installSize,
                            units = bloaty.parseCompileUnits(
                                bytes = zipFile.getInputStream(zipEntry).readBytes(),
                                debugFile = debugFileFor(name),
                            ),
                        )

                        // Some build systems emit resources under /lib/res/ instead of /res/.
                        else -> ApkEntry.Default(
                            name = name.replace("/lib/res/", "/res/"),
                            downloadSize = downloadSize,
                            installSize = installSize,
                        )
                    }
                }
                .toList()
        }
    }

    private fun parseDex(bytes: ByteArray): List<ApkEntry.Default> {
        return DexFiles.getDexFile(bytes).getClasses().map { classDef ->
            val size = classDef.getSize().toLong()
            ApkEntry.Default(classDef.getType(), size, size)
        }
    }

    /** Finds the unstripped copy of a native library, which holds the debug symbols Bloaty needs. */
    private fun debugFileFor(entryName: String): File? {
        val libName = File(entryName).nameWithoutExtension
        return unstrippedNativeFiles.find { it.name.contains(libName) }
    }
}
