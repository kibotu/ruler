package com.kibotu.ruler.common.ownership

/**
 * A single entry in the ownership file.
 *
 * @param identifier Pattern to match component/file names. Supports glob-style `*` (any chars) and `?` (single char).
 * @param owner Team name to assign when matched.
 * @param internal Override for internal/external classification. When `null`, structural type (INTERNAL/EXTERNAL) is used.
 */
data class OwnershipEntry(
    val identifier: String,
    val owner: String,
    val internal: Boolean? = null,
)
