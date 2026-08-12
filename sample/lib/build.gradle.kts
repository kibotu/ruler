plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.kibotu.ruler.sample.lib"
    compileSdk = 37
    defaultConfig {
        minSdk = 23
    }
    lint {
        warningsAsErrors = true
    }
}

dependencies {
    implementation(libs.androidx.constraintlayout)
}

kotlin {
    jvmToolchain(17)
}
