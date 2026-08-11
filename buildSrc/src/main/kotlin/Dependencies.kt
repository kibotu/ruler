object Dependencies {
    const val ANDROID_GRADLE_PLUGIN = "com.android.tools.build:gradle:${Versions.ANDROID_GRADLE_PLUGIN}"
    const val ANDROID_GRADLE_PLUGIN_VERSION = Versions.ANDROID_GRADLE_PLUGIN
    const val KOTLIN_GRADLE_PLUGIN = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.KOTLIN}"
    const val KOTLIN_VERSION = Versions.KOTLIN
    const val KOTLINX_SERIALIZATION_GRADLE_PLUGIN = "org.jetbrains.kotlin:kotlin-serialization:${Versions.KOTLIN}"
    const val SHADOW_GRADLE_PLUGIN = "com.gradleup.shadow:shadow-gradle-plugin:${Versions.SHADOW_GRADLE_PLUGIN}"
    const val PLUGIN_PUBLISH_GRADLE_PLUGIN = "com.gradle.publish:plugin-publish-plugin:${Versions.PLUGIN_PUBLISH_GRADLE_PLUGIN}"

    const val BUNDLETOOL = "com.android.tools.build:bundletool:${Versions.BUNDLETOOL}"
    const val PROTOBUF_CORE = "com.google.protobuf:protobuf-java:${Versions.PROTOBUF}"
    const val DEXLIB = "com.android.tools.smali:smali-dexlib2:${Versions.DEXLIB}"
    const val ANDROID_TOOLS_COMMON = "com.android.tools:common:${Versions.ANDROID_TOOLS}"
    const val ANDROID_TOOLS_SDKLIB = "com.android.tools:sdklib:${Versions.ANDROID_TOOLS}"
    const val APK_ANALYZER = "com.android.tools.apkparser:apkanalyzer:${Versions.ANDROID_TOOLS}"

    const val KOTLINX_SERIALIZATION_JSON = "org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.KOTLINX_SERIALIZATION}"
    const val SNAKE_YAML = "org.yaml:snakeyaml:${Versions.SNAKE_YAML}"

    const val CONSTRAINTLAYOUT = "androidx.constraintlayout:constraintlayout:${Versions.CONSTRAINTLAYOUT}"

    const val JUNIT_ENGINE = "org.junit.jupiter:junit-jupiter-engine:${Versions.JUNIT}"
    const val JUNIT_API = "org.junit.jupiter:junit-jupiter-api:${Versions.JUNIT}"
    const val JUNIT_PARAMS = "org.junit.jupiter:junit-jupiter-params:${Versions.JUNIT}"
    const val JUNIT_PLATFORM_LAUNCHER = "org.junit.platform:junit-platform-launcher:${Versions.JUNIT_PLATFORM}"
    const val GOOGLE_TRUTH = "com.google.truth:truth:${Versions.GOOGLE_TRUTH}"

    object Versions {
        const val ANDROID_GRADLE_PLUGIN = "9.3.1"
        const val KOTLIN = "2.1.20"
        const val SHADOW_GRADLE_PLUGIN = "9.2.2"
        const val PLUGIN_PUBLISH_GRADLE_PLUGIN = "1.3.0"

        const val BUNDLETOOL = "1.18.2"
        const val PROTOBUF = "4.33.1"
        const val DEXLIB = "3.0.9"
        const val ANDROID_TOOLS = "32.3.1"

        const val KOTLINX_SERIALIZATION = "1.9.0"
        const val SNAKE_YAML = "2.5"
        const val CONSTRAINTLAYOUT = "2.2.1"

        const val JUNIT = "5.12.2"
        const val JUNIT_PLATFORM = "1.12.2"
        const val GOOGLE_TRUTH = "1.4.4"
    }
}
