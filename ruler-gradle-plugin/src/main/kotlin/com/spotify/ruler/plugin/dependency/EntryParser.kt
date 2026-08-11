package com.spotify.ruler.plugin.dependency

import com.spotify.ruler.common.dependency.ArtifactResult
import com.spotify.ruler.common.dependency.DependencyEntry
import com.spotify.ruler.common.dependency.DependencyParser
import org.gradle.api.Transformer
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.attributes.Attribute
import org.gradle.api.provider.Provider
import java.io.File

/** Responsible for parsing and extracting entries from dependencies. */
class EntryParser {

    /** Parses and returns the list of entries contained in all dependencies of the given [configuration]. */
    fun parse(configuration: Configuration): Map<String, Provider<List<DependencyEntry>>> {
        return listOf("android-classes").associateWith { artifactType ->
            parseFile(configuration, artifactType, true)
        } + listOf("android-res", "android-assets", "android-jni").associateWith { artifactType ->
            parseFile(configuration, artifactType, false)
        }
    }

    private fun getArtifactView(
        configuration: Configuration,
        artifactType: String
    ) = configuration.incoming.artifactView { viewConfiguration ->
        viewConfiguration.attributes { attributeContainer ->
            attributeContainer.attribute(
                Attribute.of("artifactType", String::class.java),
                artifactType
            )
        }
    }.artifacts.resolvedArtifacts

    private fun parseFile(
        configuration: Configuration,
        artifactType: String,
        isJarOrClass: Boolean,
    ) = getArtifactView(configuration, artifactType).map { artifactResult ->
        DependencyEntryExtractor(isJarOrClass).transform(artifactResult)
    }

    internal class DependencyEntryExtractor(private val isJarOrClass: Boolean) :
        Transformer<List<DependencyEntry>, Collection<ResolvedArtifactResult>> {

        private val parser = DependencyParser()

        override fun transform(artifacts: Collection<ResolvedArtifactResult>): List<DependencyEntry> {
            return artifacts.flatMap { artifactResult ->
                val artifactFiles = artifactResult.file.walkTopDown().filter(File::isFile)
                val component = getComponentIdentifier(artifactResult)
                if (isJarOrClass) {
                    artifactFiles.map { artifactFile ->
                        when (artifactFile.extension.lowercase()) {
                            "jar" -> ArtifactResult.JarArtifact(artifactFile, component)
                            "class" -> ArtifactResult.ClassArtifact(
                                artifactFile,
                                artifactResult.file,
                                component,
                            )
                            else -> ArtifactResult.DefaultArtifact(
                                artifactFile,
                                artifactResult.file,
                                component,
                            )
                        }
                    }
                } else {
                    artifactFiles.map { artifactFile ->
                        ArtifactResult.DefaultArtifact(artifactFile, artifactResult.file, component)
                    }
                }
            }.run(parser::parse)
        }

        private fun getComponentIdentifier(artifactResult: ResolvedArtifactResult): String {
            return artifactResult.id.componentIdentifier.displayName
        }
    }
}
