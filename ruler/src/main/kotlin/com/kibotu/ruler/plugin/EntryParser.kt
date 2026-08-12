package com.kibotu.ruler.plugin

import com.kibotu.ruler.analysis.dependency.ArtifactResult
import com.kibotu.ruler.analysis.dependency.DependencyEntry
import com.kibotu.ruler.analysis.dependency.DependencyParser
import org.gradle.api.Transformer
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.attributes.Attribute
import org.gradle.api.provider.Provider
import java.io.File

/** Artifact types that hold code. Their files are parsed as classes. */
private val CODE_ARTIFACT_TYPES = listOf("android-classes")

/** Artifact types that hold everything else. */
private val ASSET_ARTIFACT_TYPES = listOf("android-res", "android-assets", "android-jni")

/** Lists the files that each runtime dependency contributes to the app. */
class EntryParser {

    /** @return One lazy list of entries per artifact type. */
    fun parse(configuration: Configuration): Map<String, Provider<List<DependencyEntry>>> {
        return CODE_ARTIFACT_TYPES.associateWith { parse(configuration, it, holdsCode = true) } +
            ASSET_ARTIFACT_TYPES.associateWith { parse(configuration, it, holdsCode = false) }
    }

    private fun parse(configuration: Configuration, artifactType: String, holdsCode: Boolean) =
        configuration.incoming
            .artifactView { view ->
                view.attributes {
                    it.attribute(Attribute.of("artifactType", String::class.java), artifactType)
                }
            }
            .artifacts
            .resolvedArtifacts
            .map(EntryExtractor(holdsCode))

    internal class EntryExtractor(
        private val holdsCode: Boolean,
    ) : Transformer<List<DependencyEntry>, Collection<ResolvedArtifactResult>> {

        override fun transform(artifacts: Collection<ResolvedArtifactResult>): List<DependencyEntry> {
            return DependencyParser().parse(
                artifacts.flatMap { artifact ->
                    val component = artifact.id.componentIdentifier.displayName
                    artifact.file.walkTopDown().filter(File::isFile).map { file ->
                        when {
                            !holdsCode -> ArtifactResult.DefaultArtifact(file, artifact.file, component)
                            file.extension.equals("jar", ignoreCase = true) ->
                                ArtifactResult.JarArtifact(file, component)

                            file.extension.equals("class", ignoreCase = true) ->
                                ArtifactResult.ClassArtifact(file, artifact.file, component)

                            else -> ArtifactResult.DefaultArtifact(file, artifact.file, component)
                        }
                    }
                },
            )
        }
    }
}
