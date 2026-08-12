package com.kibotu.ruler.analysis.dependency

/** Extracts the files that each dependency contributes. */
class DependencyParser {

    fun parse(artifacts: List<ArtifactResult>): List<DependencyEntry> = artifacts.flatMap { artifact ->
        when (artifact) {
            is ArtifactResult.JarArtifact -> JarArtifactParser().parseFile(artifact)
            is ArtifactResult.ClassArtifact -> ClassArtifactParser().parseFile(artifact)
            is ArtifactResult.DefaultArtifact -> DefaultArtifactParser().parseFile(artifact)
        }
    }
}
