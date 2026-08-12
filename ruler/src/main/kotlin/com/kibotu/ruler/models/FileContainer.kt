package com.kibotu.ruler.models

/** Piece of an app that can contain files. */
interface FileContainer : Measurable {
    val name: String
    val owner: String?
    val files: List<AppFile>?
}
