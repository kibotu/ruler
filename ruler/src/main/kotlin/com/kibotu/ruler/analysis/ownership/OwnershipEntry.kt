package com.kibotu.ruler.analysis.ownership

/**
 * A single entry in the ownership file.
 *
 * @param identifier Pattern matching component/file names. Supports glob `*` and `?`.
 * @param owners Team names. The first is primary; the rest only show in the report.
 * @param internal Overrides the structural type. Null keeps it.
 */
data class OwnershipEntry(
    val identifier: String,
    val owners: List<String>,
    val internal: Boolean? = null,
) {
    constructor(identifier: String, owner: String, internal: Boolean? = null) : this(identifier, listOf(owner), internal)
}
