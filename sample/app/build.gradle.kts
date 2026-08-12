plugins {
    alias(libs.plugins.android.application)
    id("net.kibotu.ruler")
}

android {
    namespace = "com.kibotu.ruler.sample.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.kibotu.ruler.sample"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        release {
            isShrinkResources = true
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
    bundle {
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
        language {
            enableSplit = false
        }
    }
}

dependencies {
    implementation(project(":lib"))
}

ruler {
    abi.set("arm64-v8a")
    locale.set("en")
    screenDensity.set(480)
    sdkVersion.set(36)

    ownershipFile.set(project.layout.projectDirectory.file("ownership.yaml"))

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
