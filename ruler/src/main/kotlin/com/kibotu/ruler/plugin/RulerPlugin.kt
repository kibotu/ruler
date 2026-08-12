package com.kibotu.ruler.plugin

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import com.kibotu.ruler.analysis.AppInfo
import com.kibotu.ruler.analysis.DeviceSpec
import com.kibotu.ruler.analysis.verification.VerificationConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property

private const val EXTENSION_NAME = "ruler"

/** Registers an `analyze<Variant>Bundle` task for every application variant. */
class RulerPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val ruler = project.extensions.create(EXTENSION_NAME, RulerExtension::class.java)
        val verification = (ruler as ExtensionAware).extensions
            .create("verification", RulerVerificationExtension::class.java)

        project.plugins.withId("com.android.application") {
            val androidComponents =
                project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)

            androidComponents.onVariants { variant ->
                val variantName = variant.name.replaceFirstChar { it.titlecase() }

                project.tasks.register("analyze${variantName}Bundle", RulerTask::class.java) { task ->
                    task.group = EXTENSION_NAME
                    task.description = "Measures the size of the $variantName app bundle."

                    EntryParser().parse(variant.runtimeConfiguration).forEach(task.dependencyEntries::put)

                    task.projectPath.set(project.path)
                    task.sdkDirectory.set(androidComponents.sdkComponents.sdkDirectory)

                    task.appInfo.set(appInfo(project, variant))
                    task.deviceSpec.set(deviceSpec(ruler))

                    task.bundleFile.set(project.getBundleFile(variant))
                    task.mappingFile.set(project.getMappingFile(variant))
                    task.resourceMappingFile.set(project.getResourceMappingFile(variant))

                    task.ownershipFile.set(ruler.ownershipFile)
                    task.defaultOwner.set(ruler.defaultOwner)
                    task.staticDependenciesFile.set(ruler.staticDependenciesFile)
                    task.omitFileBreakdown.set(ruler.omitFileBreakdown)
                    task.unstrippedNativeFiles.set(ruler.unstrippedNativeFiles)
                    task.bloatyPath.set(ruler.bloatyPath)

                    val buildDir = project.layout.buildDirectory
                    task.workingDir.set(buildDir.dir("intermediates/ruler/${variant.name}"))
                    task.reportDir.set(buildDir.dir("reports/ruler/${variant.name}"))

                    task.verificationConfig.set(
                        VerificationConfig(
                            downloadSizeThreshold = verification.downloadSizeThreshold.get(),
                            installSizeThreshold = verification.installSizeThreshold.get(),
                        ),
                    )

                    // DexGuard writes its bundle after the standard one, so the artifact provider
                    // alone does not order the tasks correctly.
                    task.dependsOn("bundle$variantName")
                }
            }
        }
    }

    private fun appInfo(project: Project, variant: ApplicationVariant) = project.provider {
        AppInfo(
            applicationId = variant.applicationId.get(),
            versionName = variant.outputs.first().versionName.orNull ?: "-",
            variantName = variant.name,
        )
    }

    private fun deviceSpec(extension: RulerExtension) = DeviceSpec(
        abi = extension.abi.required("abi"),
        locale = extension.locale.required("locale"),
        screenDensity = extension.screenDensity.required("screenDensity"),
        sdkVersion = extension.sdkVersion.required("sdkVersion"),
    )

    private fun <T : Any> Property<T>.required(name: String): T =
        checkNotNull(orNull) {
            "ruler { $name } is not set. Ruler needs a device specification to split the bundle."
        }
}
