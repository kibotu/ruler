package com.kibotu.ruler.models

/** Piece of an app whose size can be measured. */
interface Measurable {
    val downloadSize: Long
    val installSize: Long

    enum class SizeType { DOWNLOAD, INSTALL }
}
