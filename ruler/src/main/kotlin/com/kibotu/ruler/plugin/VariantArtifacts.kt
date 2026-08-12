package com.kibotu.ruler.plugin

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationVariant
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import java.io.File

/**
 * Third-party obfuscators write their bundle and mapping files to their own directories, instead
 * of replacing the standard ones.
 */
internal enum class Obfuscator(val pluginId: String) {
    DEXGUARD("dexguard"),
    PROGUARD("com.guardsquare.proguard"),
    ;

    /** Path of the mapping file, relative to the build directory. */
    fun mappingPath(variant: String): String = when (this) {
        DEXGUARD -> "outputs/dexguard/mapping/bundle/$variant/mapping.txt"
        PROGUARD -> "outputs/proguard/$variant/mapping/mapping.txt"
    }

    /** Path of the resource name mapping file, or null when the obfuscator has none. */
    fun resourceMappingPath(variant: String): String? = when (this) {
        DEXGUARD -> "outputs/dexguard/mapping/bundle/$variant/resourcefilenamemapping.txt"
        PROGUARD -> null
    }

    /** The separate bundle that the obfuscator writes, or null when it replaces the standard one. */
    fun protectedBundle(bundle: File): File? = when (this) {
        DEXGUARD -> bundle.parentFile.resolve("${bundle.nameWithoutExtension}-protected.aab")
        PROGUARD -> null
    }

    companion object {
        fun of(project: Project): Obfuscator? =
            entries.firstOrNull { project.pluginManager.hasPlugin(it.pluginId) }
    }
}

/**
 * The bundle to analyze.
 *
 * The obfuscator has not run at configuration time, so its output may appear later. Every lookup
 * below therefore resolves lazily and falls back when the file never appears.
 */
internal fun Project.getBundleFile(variant: ApplicationVariant): Provider<RegularFile> {
    val defaultBundle = variant.artifacts.get(SingleArtifact.BUNDLE)
    val obfuscator = Obfuscator.of(this) ?: return defaultBundle
    val buildDirectory = layout.buildDirectory

    return defaultBundle.flatMap { bundle ->
        val protectedBundle = obfuscator.protectedBundle(bundle.asFile)
        if (protectedBundle?.exists() == true) {
            buildDirectory.file(protectedBundle.absolutePath)
        } else {
            defaultBundle
        }
    }
}

/** The mapping file that de-obfuscates class names. */
internal fun Project.getMappingFile(variant: ApplicationVariant): Provider<RegularFile> {
    val defaultMapping = variant.artifacts.get(SingleArtifact.OBFUSCATION_MAPPING_FILE)
    val obfuscator = Obfuscator.of(this) ?: return defaultMapping

    val obfuscatorMapping = layout.buildDirectory.file(obfuscator.mappingPath(variant.name))
    return obfuscatorMapping.flatMap { file ->
        if (file.asFile.exists()) obfuscatorMapping else defaultMapping
    }
}

/** The mapping file that de-obfuscates resource names. Only DexGuard writes one. */
internal fun Project.getResourceMappingFile(variant: ApplicationVariant): Provider<RegularFile> {
    val none = objects.fileProperty()
    val path = Obfuscator.of(this)?.resourceMappingPath(variant.name) ?: return none

    val mapping = layout.buildDirectory.file(path)
    return mapping.flatMap { file -> if (file.asFile.exists()) mapping else none }
}
