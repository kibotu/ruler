package com.kibotu.ruler.plugin

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import com.kibotu.ruler.analysis.AppInfo
import com.kibotu.ruler.analysis.DeviceSpec
import com.kibotu.ruler.analysis.verification.VerificationConfig
import com.kibotu.ruler.report.HtmlReporter
import com.kibotu.ruler.report.JsonReporter
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

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
                val reportDir = project.layout.buildDirectory.dir("reports/ruler/${variant.name}")
                val reportPaths = project.registerReportPathsTask(variantName, reportDir)

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

                    task.workingDir.set(project.layout.buildDirectory.dir("intermediates/ruler/${variant.name}"))
                    task.reportDir.set(reportDir)

                    task.verificationConfig.set(
                        VerificationConfig(
                            downloadSizeThreshold = verification.downloadSizeThreshold.get(),
                            installSizeThreshold = verification.installSizeThreshold.get(),
                        ),
                    )

                    // DexGuard writes its bundle after the standard one, so the artifact provider
                    // alone does not order the tasks correctly.
                    task.dependsOn("bundle$variantName")

                    task.finalizedBy(reportPaths)
                }
            }
        }
    }

    /**
     * A task that says where the reports are.
     *
     * The analysis is cacheable, so Gradle skips its action once nothing has changed, and anything
     * the action logs goes with it. This task declares no outputs, so it always runs and the paths
     * are printed on every build, whether the analysis ran, was up to date, or came from the cache.
     */
    private fun Project.registerReportPathsTask(
        variantName: String,
        reportDir: Provider<Directory>,
    ): TaskProvider<Task> = tasks.register("printRuler${variantName}Reports") { task ->
        task.description = "Prints where the $variantName reports are."
        task.doLast {
            val directory = reportDir.get().asFile.toPath()
            it.logger.lifecycle("JSON report: ${directory.resolve(JsonReporter.FILE_NAME).toUri()}")
            it.logger.lifecycle("HTML report: ${directory.resolve(HtmlReporter.FILE_NAME).toUri()}")
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
