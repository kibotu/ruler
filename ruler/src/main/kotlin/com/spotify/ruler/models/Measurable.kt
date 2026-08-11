package com.spotify.ruler.models

/** Piece of an app whose size can be measured. */
interface Measurable {
    val downloadSize: Long
    val installSize: Long

    enum class SizeType { DOWNLOAD, INSTALL }

    fun getSize(sizeType: SizeType) = when (sizeType) {
        SizeType.DOWNLOAD -> downloadSize
        SizeType.INSTALL -> installSize
    }

    data class Mutable(override var downloadSize: Long, override var installSize: Long) : Measurable
}
