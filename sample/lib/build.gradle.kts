plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.spotify.ruler.sample.lib"
    compileSdk = 36
    defaultConfig {
        minSdk = 21
    }
    lint {
        warningsAsErrors = true
    }
}

dependencies {
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
}

kotlin {
    jvmToolchain(17)
}
