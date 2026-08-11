plugins {
    id("com.android.library")
}

android {
    namespace = "com.spotify.ruler.sample.lib"
    compileSdk = 37
    defaultConfig {
        minSdk = 23
    }
    lint {
        warningsAsErrors = true
    }
}

dependencies {
    implementation(Dependencies.CONSTRAINTLAYOUT)
}

kotlin {
    jvmToolchain(17)
}
