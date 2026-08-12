package com.kibotu.ruler.analysis.apk

/** A single entry of an APK. */
sealed interface ApkEntry {
    val name: String
    val downloadSize: Long
    val installSize: Long

    /** An entry with no special handling. */
    data class Default(
        override val name: String,
        override val downloadSize: Long,
        override val installSize: Long,
    ) : ApkEntry

    /** A DEX file, with the classes it contains. */
    data class Dex(
        override val name: String,
        override val downloadSize: Long,
        override val installSize: Long,
        val classes: List<Default>,
    ) : ApkEntry

    /** A native library, with the compile units Bloaty found in it. Empty when Bloaty did not run. */
    data class NativeLibrary(
        override val name: String,
        override val downloadSize: Long,
        override val installSize: Long,
        val units: List<Default>,
    ) : ApkEntry
}
