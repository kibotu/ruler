pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "net.kibotu.ruler") {
                useModule("net.kibotu:ruler:3.0.0")
            }
        }
    }
    repositories {
        mavenLocal()
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
}

rootProject.name = "ruler"

include(":ruler")
include(":sample:app")
include(":sample:lib")
