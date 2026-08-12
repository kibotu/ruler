plugins {
    id("com.android.library")
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
    implementation(Dependencies.CONSTRAINTLAYOUT)
}

kotlin {
    jvmToolchain(17)
}
