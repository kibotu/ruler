package com.kibotu.ruler.analysis.dependency

import java.io.File

/** One file of a resolved dependency, as it appears on the runtime classpath. */
sealed interface ArtifactResult {
    val component: String

    /** A file with no special handling, named relative to [artifactRoot]. */
    data class DefaultArtifact(
        val file: File,
        val artifactRoot: File,
        override val component: String,
    ) : ArtifactResult

    /** A compiled class, named relative to [artifactRoot]. */
    data class ClassArtifact(
        val file: File,
        val artifactRoot: File,
        override val component: String,
    ) : ArtifactResult

    /** A JAR. Its contents are what ends up in the app, not the JAR itself. */
    data class JarArtifact(
        val file: File,
        override val component: String,
    ) : ArtifactResult
}
