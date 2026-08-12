package com.spotify.ruler.plugin

import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

open class RulerExtension(objects: ObjectFactory) {
    val abi: Property<String> = objects.property(String::class.java)
    val locale: Property<String> = objects.property(String::class.java)
    val screenDensity: Property<Int> = objects.property(Int::class.java)
    val sdkVersion: Property<Int> = objects.property(Int::class.java)

    val ownershipFile: RegularFileProperty = objects.fileProperty()
    val defaultOwner: Property<String> = objects.property(String::class.java)
    val staticDependenciesFile: RegularFileProperty = objects.fileProperty()

    val omitFileBreakdown: Property<Boolean> = objects.property(Boolean::class.java)
    val unstrippedNativeFiles: ListProperty<RegularFile> = objects.listProperty(RegularFile::class.java)

    init {
        defaultOwner.convention("")
        omitFileBreakdown.convention(false)
        unstrippedNativeFiles.set(emptyList())
    }
}
