package com.kibotu.ruler.analysis.ownership

/**
 * A single entry in the ownership file.
 *
 * @param identifier Pattern to match component/file names. Supports glob-style `*` (any chars) and `?` (single char).
 * @param owners Team names to assign when matched. The first owner is primary; additional owners are shown in the report only.
 * @param internal Override for internal/external classification. When `null`, structural type (INTERNAL/EXTERNAL) is used.
 */
data class OwnershipEntry(
    val identifier: String,
    val owners: List<String>,
    val internal: Boolean? = null,
) {
    constructor(identifier: String, owner: String, internal: Boolean? = null) : this(identifier, listOf(owner), internal)
}
