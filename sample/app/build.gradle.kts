plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("net.kibotu.ruler")
}

android {
    namespace = "com.spotify.ruler.sample.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.spotify.ruler.sample"
        minSdk = 21
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    lint {
        warningsAsErrors = true
    }
    packaging {
        resources.excludes.add("**/*.kotlin_builtins")
        resources.excludes.add("kotlin-tooling-metadata.json")
    }
}

dependencies {
    implementation(project(":sample:lib"))
}

ruler {
    abi.set("arm64-v8a")
    locale.set("en")
    screenDensity.set(480)
    sdkVersion.set(36)

    ownershipFile.set(project.layout.projectDirectory.file("ownership.yaml"))
    defaultOwner.set("default-team")

    verification {
        downloadSizeThreshold = 20 * 1000 * 1000
        installSizeThreshold = 20 * 1000 * 1000
    }
}

tasks.named("check").configure {
    dependsOn("analyzeDebugBundle")
    dependsOn("analyzeReleaseBundle")
}

kotlin {
    jvmToolchain(17)
}
