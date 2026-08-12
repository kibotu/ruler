package com.kibotu.ruler.plugin

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

open class RulerVerificationExtension(objects: ObjectFactory) {
    val downloadSizeThreshold: Property<Long> = objects.property(Long::class.java)
    val installSizeThreshold: Property<Long> = objects.property(Long::class.java)

    init {
        downloadSizeThreshold.convention(Long.MAX_VALUE)
        installSizeThreshold.convention(Long.MAX_VALUE)
    }
}
