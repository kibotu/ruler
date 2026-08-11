pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "net.kibotu.ruler") {
                useModule("net.kibotu:ruler-gradle-plugin:3.0.0")
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

include(":ruler-models")
include(":ruler-common")
include(":ruler-gradle-plugin")

//if (startParameter.projectProperties.containsKey("withSample")) {
    include(":sample:app")
    include(":sample:lib")
//}
