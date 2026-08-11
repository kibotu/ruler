package com.spotify.ruler.common.apk

import kotlinx.serialization.Serializable

/** Single entry of an APK. */
@Serializable
sealed class ApkEntry {
    abstract val name: String
    abstract val downloadSize: Long
    abstract val installSize: Long

    /** Default APK entry. If an entry has no special type, it is considered to be a default entry. */
    @Serializable
    data class Default(
        override val name: String,
        override val downloadSize: Long,
        override val installSize: Long,
    ) : ApkEntry()

    /** DEX file APK entry containing compiled classes. */
    @Serializable
    data class Dex(
        override val name: String,
        override val downloadSize: Long,
        override val installSize: Long,
        val classes: List<ApkEntry>,
    ) : ApkEntry()

    /** Native Library entry containing files coming from native library. */
    @Serializable
    data class NativeLibrary(
        override val name: String,
        override val downloadSize: Long,
        override val installSize: Long,
        val classes: List<ApkEntry>,
    ): ApkEntry()
}
