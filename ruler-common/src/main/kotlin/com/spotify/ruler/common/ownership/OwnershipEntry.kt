package com.spotify.ruler.common.ownership

/** A single entry in the ownership file. */
data class OwnershipEntry(
    val identifier: String,
    val owner: String,
)
