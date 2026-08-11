package com.spotify.ruler.common.dependency

import java.io.Serializable
import kotlinx.serialization.Serializable as KSerializable

/** Single entry of a dependency. */
@KSerializable
sealed class DependencyEntry: Serializable {
    abstract val name: String
    abstract val component: String

    /** Default dependency entry. If an entry has no special type, it is considered to be a default entry. */
    @KSerializable
    data class Default(
        override val name: String,
        override val component: String,
    ) : DependencyEntry()

    /** Class file dependency entry. */
    @KSerializable
    data class Class(
        override val name: String,
        override val component: String,
    ) : DependencyEntry()

    companion object {
        private const val serialVersionUID = 1L
    }
}
