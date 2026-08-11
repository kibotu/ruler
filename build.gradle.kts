plugins {
    id("org.jetbrains.kotlin.jvm") version Dependencies.Versions.KOTLIN apply false
    id("org.jetbrains.kotlin.plugin.serialization") version Dependencies.Versions.KOTLIN apply false
    id("com.android.application") version Dependencies.Versions.ANDROID_GRADLE_PLUGIN apply false
    id("com.android.library") version Dependencies.Versions.ANDROID_GRADLE_PLUGIN apply false
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
