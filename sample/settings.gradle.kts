// The sample is a separate build so that it consumes Ruler exactly like a real
// consumer does. `includeBuild` substitutes the published plugin with the local
// project, so no `publishToMavenLocal` round-trip is needed.
pluginManagement {
    includeBuild("..")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "ruler-sample"

include(":app")
include(":lib")
