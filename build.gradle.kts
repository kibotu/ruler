plugins {
    id("org.jetbrains.kotlin.jvm") version "2.1.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.20" apply false
    id("com.android.application") version "8.13.1" apply false
    id("com.android.library") version "8.13.1" apply false
    id("org.jetbrains.kotlin.android") version "2.1.20" apply false
}

import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension

subprojects {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        configure<KotlinJvmExtension> {
            jvmToolchain(17)
        }
    }
}

allprojects {
    group = "net.kibotu"
    version = "3.0.0"

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
