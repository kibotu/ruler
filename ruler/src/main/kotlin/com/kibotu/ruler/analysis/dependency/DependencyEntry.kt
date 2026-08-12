package com.kibotu.ruler.analysis.dependency

import java.io.Serializable

/** A single file of a dependency. Serializable, because it is a Gradle task input. */
sealed class DependencyEntry : Serializable {
    abstract val name: String
    abstract val component: String

    /** A file with no special handling. */
    data class Default(
        override val name: String,
        override val component: String,
    ) : DependencyEntry()

    /** A compiled class. Its name is de-obfuscated before attribution. */
    data class Class(
        override val name: String,
        override val component: String,
    ) : DependencyEntry()

    companion object {
        private const val serialVersionUID = 1L
    }
}
