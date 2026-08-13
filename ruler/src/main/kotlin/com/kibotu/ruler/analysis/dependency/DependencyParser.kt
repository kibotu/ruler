package com.kibotu.ruler.analysis.dependency

import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarFile

/** Extracts the files that each dependency contributes. */
class DependencyParser {

    fun parse(artifacts: List<ArtifactResult>): List<DependencyEntry> = artifacts.flatMap { artifact ->
        when (artifact) {
            is ArtifactResult.JarArtifact -> jarEntries(artifact)

            is ArtifactResult.ClassArtifact ->
                listOf(DependencyEntry.Class(artifact.file.pathIn(artifact.artifactRoot), artifact.component))

            is ArtifactResult.DefaultArtifact ->
                listOf(DependencyEntry.Default(artifact.file.pathIn(artifact.artifactRoot), artifact.component))
        }
    }

    private fun jarEntries(artifact: ArtifactResult.JarArtifact): List<DependencyEntry> =
        JarFile(artifact.file).use { jar ->
            jar.entries().asSequence().filterNot(JarEntry::isDirectory).map { entry ->
                if (entry.name.endsWith(".class", ignoreCase = true)) {
                    DependencyEntry.Class(entry.name, artifact.component)
                } else {
                    DependencyEntry.Default(entry.name, artifact.component)
                }
            }.toList()
        }

    private fun File.pathIn(root: File): String = absolutePath.removePrefix(root.absolutePath)
}
