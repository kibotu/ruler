package com.spotify.ruler.plugin

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationVariant
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider

/**
 * Returns the bundle file that's going to be analyzed. DexGuard produces a separate bundle instead of overriding
 * the default one, so we have to handle that separately.
 */
internal fun Project.getBundleFile(
    variant: ApplicationVariant
): Provider<RegularFile> {
    val defaultBundleFile = variant.artifacts.get(SingleArtifact.BUNDLE)
    if (!hasDexGuard(project)) {
        return defaultBundleFile
    }

    return defaultBundleFile.flatMap { bundle ->
        val dexGuardBundle =
            bundle.asFile.parentFile.resolve("${bundle.asFile.nameWithoutExtension}-protected.aab")
        if (dexGuardBundle.exists()) {
            project.layout.buildDirectory.file(dexGuardBundle.absolutePath)
        } else {
            defaultBundleFile
        }
    }
}

/**
 * Returns the mapping file used for de-obfuscation. Different obfuscation tools like DexGuard and ProGuard place
 * their mapping files in different directories, so we have to handle those separately.
 */
internal fun Project.getMappingFile(
    variant: ApplicationVariant
): Provider<RegularFile> {
    val defaultMappingFile = variant.artifacts.get(SingleArtifact.OBFUSCATION_MAPPING_FILE)
    val mappingFilePath = when {
        hasDexGuard(project) -> "outputs/dexguard/mapping/bundle/${variant.name}/mapping.txt"
        hasProGuard(project) -> "outputs/proguard/${variant.name}/mapping/mapping.txt"
        else -> return defaultMappingFile
    }

    val mappingFileProvider = project.layout.buildDirectory.file(mappingFilePath)
    return mappingFileProvider.flatMap { mappingFile ->
        if (mappingFile.asFile.exists()) {
            mappingFileProvider
        } else {
            defaultMappingFile
        }
    }
}

/**
 * Returns a mapping file to de-obfuscate resource names. DexGuard supports this feature by default, so we need to
 * handle it accordingly.
 */
internal fun Project.getResourceMappingFile(
    variant: ApplicationVariant
): Provider<RegularFile> {
    val defaultResourceMappingFile = project.objects.fileProperty()

    val resourceMappingFilePath = when {
        hasDexGuard(project) -> "outputs/dexguard/mapping/bundle/${variant.name}/resourcefilenamemapping.txt"
        else -> return defaultResourceMappingFile
    }

    val resourceMappingFileProvider =
        project.layout.buildDirectory.file(resourceMappingFilePath)
    return resourceMappingFileProvider.flatMap { resourceMappingFile ->
        if (resourceMappingFile.asFile.exists()) {
            resourceMappingFileProvider
        } else {
            defaultResourceMappingFile
        }
    }
}

private fun hasDexGuard(project: Project): Boolean {
    return project.pluginManager.hasPlugin("dexguard")
}

private fun hasProGuard(project: Project): Boolean {
    return project.pluginManager.hasPlugin("com.guardsquare.proguard")
}
