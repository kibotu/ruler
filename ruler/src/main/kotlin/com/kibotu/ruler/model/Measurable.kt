package com.kibotu.ruler.model

/** Piece of an app whose size can be measured. */
interface Measurable {
    val downloadSize: Long
    val installSize: Long
}
