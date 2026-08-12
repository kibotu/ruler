package com.kibotu.ruler.common.dependency

import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarFile

interface ArtifactParser<in T> {
    fun parseFile(artifactResult: T): List<DependencyEntry>
}

/** Plain artifact parser which returns a list of all artifact files. */
class DefaultArtifactParser : ArtifactParser<ArtifactResult.DefaultArtifact> {

    override fun parseFile(artifactResult: ArtifactResult.DefaultArtifact): List<DependencyEntry> {
        val name =
            artifactResult.file.absolutePath.removePrefix(artifactResult.resolvedArtifactFile.absolutePath)
        return listOf(DependencyEntry.Default(name, artifactResult.component))
    }
}

/** Artifact parser for .class files that returns a list of the class artifact. */
class ClassArtifactParser : ArtifactParser<ArtifactResult.ClassArtifact> {
    override fun parseFile(artifactResult: ArtifactResult.ClassArtifact): List<DependencyEntry> {
        val name =
            artifactResult.file.absolutePath.removePrefix(artifactResult.resolvedArtifactFile.absolutePath)
        return listOf(DependencyEntry.Class(name, artifactResult.component))
    }
}

/** Artifact parser which parses JAR artifacts and returns the contents of those JAR files. */
class JarArtifactParser : ArtifactParser<ArtifactResult.JarArtifact> {

    override fun parseFile(artifactResult: ArtifactResult.JarArtifact): List<DependencyEntry> {
        val component = artifactResult.component
        return JarFile(artifactResult.file).use { jarFile ->
            jarFile.entries().asSequence().filterNot(JarEntry::isDirectory).map { entry ->
                when {
                    isClassEntry(entry.name) -> DependencyEntry.Class(entry.name, component)
                    else -> DependencyEntry.Default(entry.name, component)
                }
            }.toList()
        }
    }

    private fun isClassEntry(entryName: String): Boolean {
        return entryName.endsWith(".class", ignoreCase = true)
    }
}

sealed interface ArtifactResult {
    data class DefaultArtifact(
        val file: File,
        val resolvedArtifactFile: File,
        val component: String
    ) : ArtifactResult

    data class JarArtifact(val file: File, val component: String) : ArtifactResult

    data class ClassArtifact(val file: File, val resolvedArtifactFile: File, val component: String) : ArtifactResult
}
