package com.kibotu.ruler.plugin

import com.kibotu.ruler.analysis.AppInfo
import com.kibotu.ruler.analysis.DeviceSpec
import com.kibotu.ruler.analysis.RulerConfig
import com.kibotu.ruler.analysis.SizeAnalysis
import com.kibotu.ruler.analysis.apk.ApkCreator
import com.kibotu.ruler.analysis.dependency.DependencyEntry
import com.kibotu.ruler.analysis.verification.VerificationConfig
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

/** Measures one application variant and writes its reports. */
@CacheableTask
abstract class RulerTask : DefaultTask() {

    @get:Input
    abstract val dependencyEntries: MapProperty<String, List<DependencyEntry>>

    @get:Input
    abstract val projectPath: Property<String>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sdkDirectory: DirectoryProperty

    @get:Input
    abstract val appInfo: Property<AppInfo>

    @get:Input
    abstract val deviceSpec: Property<DeviceSpec>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val bundleFile: RegularFileProperty

    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val mappingFile: RegularFileProperty

    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val resourceMappingFile: RegularFileProperty

    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val ownershipFile: RegularFileProperty

    @get:Input
    abstract val defaultOwner: Property<String>

    @get:Input
    abstract val omitFileBreakdown: Property<Boolean>

    @get:Optional
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val unstrippedNativeFiles: ListProperty<RegularFile>

    @get:Optional
    @get:Input
    abstract val bloatyPath: Property<String>

    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val staticDependenciesFile: RegularFileProperty

    @get:Input
    abstract val verificationConfig: Property<VerificationConfig>

    @get:OutputDirectory
    abstract val workingDir: DirectoryProperty

    @get:OutputDirectory
    abstract val reportDir: DirectoryProperty

    @TaskAction
    fun analyze() {
        SizeAnalysis(
            config = RulerConfig(
                projectPath = projectPath.get(),
                apkFilesMap = splitApks(),
                reportDir = reportDir.asFile.get(),
                ownershipFile = ownershipFile.asFile.orNull,
                staticDependenciesFile = staticDependenciesFile.asFile.orNull,
                appInfo = appInfo.get(),
                defaultOwner = defaultOwner.get(),
                omitFileBreakdown = omitFileBreakdown.get(),
                verificationConfig = verificationConfig.get(),
            ),
            dependencyEntries = dependencyEntries.get().values.flatten(),
            mappingFile = mappingFile.asFile.orNull,
            resourceMappingFile = resourceMappingFile.asFile.orNull,
            unstrippedNativeFiles = unstrippedNativeFiles.get().map(RegularFile::getAsFile),
            bloatyPath = bloatyPath.orNull,
            log = logger::lifecycle,
        ).run()
    }

    /** Splits the bundle for the target device. An APK handed in directly is used as-is. */
    private fun splitApks(): Map<String, List<File>> {
        val bundle = bundleFile.asFile.get()
        if (bundle.extension == "apk") {
            return mapOf(ApkCreator.BASE_FEATURE_NAME to listOf(bundle))
        }
        return ApkCreator(sdkDirectory.asFile.get())
            .createSplitApks(bundle, deviceSpec.get(), workingDir.asFile.get())
    }
}
