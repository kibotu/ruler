package com.spotify.ruler.common.ownership

import com.spotify.ruler.models.ComponentType

/**
 * Encapsulates ownership information of different components.
 *
 * @param entries List of ownership entries parsed from the ownership file.
 * @param defaultOwner Owner which should be used if no explicit owner is defined.
 */
class OwnershipInfo(entries: List<OwnershipEntry>, private val defaultOwner: String) {
    private val explicitOwnershipEntries = mutableMapOf<String, String>()
    private val wildcardOwnershipEntries = mutableMapOf<String, String>()

    init {
        entries.forEach { (identifier, owner) ->
            if (identifier.endsWith('*')) {
                wildcardOwnershipEntries[identifier.substringBeforeLast('*')] = owner
            } else {
                explicitOwnershipEntries[identifier] = owner
            }
        }
    }

    fun getOwner(component: String, componentType: ComponentType): String {
        val owner = when (componentType) {
            ComponentType.INTERNAL -> explicitOwnershipEntries[component]
            ComponentType.EXTERNAL -> explicitOwnershipEntries[component.substringBeforeLast(':')]
        }
        return owner ?: getWildcardOwner(component) ?: defaultOwner
    }

    fun getOwner(feature: String): String {
        return explicitOwnershipEntries[feature] ?: getWildcardOwner(feature) ?: defaultOwner
    }

    fun getOwner(file: String, component: String, componentType: ComponentType): String {
        return explicitOwnershipEntries[file] ?: getWildcardOwner(file) ?: getOwner(component, componentType)
    }

    fun getOwner(file: String, feature: String): String {
        return explicitOwnershipEntries[file] ?: getWildcardOwner(file) ?: getOwner(feature)
    }

    private fun getWildcardOwner(identifier: String): String? {
        val matchingIdentifier = wildcardOwnershipEntries.keys
            .filter(identifier::startsWith)
            .maxByOrNull(String::length)
        return wildcardOwnershipEntries[matchingIdentifier]
    }
}
