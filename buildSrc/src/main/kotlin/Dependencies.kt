/*
 * Copyright 2021 Spotify AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

object Dependencies {
    const val RULER_GRADLE_PLUGIN = "$RULER_PLUGIN_GROUP:ruler-gradle-plugin:$RULER_PLUGIN_VERSION"

    const val ANDROID_GRADLE_PLUGIN = "com.android.tools.build:gradle:${Versions.ANDROID_GRADLE_PLUGIN}"
    const val KOTLIN_GRADLE_PLUGIN = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.KOTLIN}"
    const val KOTLINX_SERIALIZATION_GRADLE_PLUGIN = "org.jetbrains.kotlin:kotlin-serialization:${Versions.KOTLIN}"
    const val DETEKT_GRADLE_PLUGIN = "io.gitlab.arturbosch.detekt:detekt-gradle-plugin:${Versions.DETEKT_GRADLE_PLUGIN}"
    const val NEXUS_PUBLISH_GRADLE_PLUGIN = "io.github.gradle-nexus.publish-plugin:io.github.gradle-nexus.publish-plugin.gradle.plugin:${Versions.NEXUS_PUBLISH_GRADLE_PLUGIN}"
    const val SHADOW_GRADLE_PLUGIN = "com.gradleup.shadow:shadow-gradle-plugin:${Versions.SHADOW_GRADLE_PLUGIN}"

    const val BUNDLETOOL = "com.android.tools.build:bundletool:${Versions.BUNDLETOOL}"
    const val PROTOBUF_CORE = "com.google.protobuf:protobuf-java:${Versions.PROTOBUF}"
    const val DEXLIB = "com.android.tools.smali:smali-dexlib2:${Versions.DEXLIB}"
    const val ANDROID_TOOLS_COMMON = "com.android.tools:common:${Versions.ANDROID_TOOLS}"
    const val ANDROID_TOOLS_SDKLIB = "com.android.tools:sdklib:${Versions.ANDROID_TOOLS}"
    const val ANDROID_CONSTRAINT_LAYOUT = "androidx.constraintlayout:constraintlayout:${Versions.CONSTRAINT_LAYOUT}"

    const val APK_ANALYZER = "com.android.tools.apkparser:apkanalyzer:${Versions.ANDROID_TOOLS}"
    const val CLIKT = "com.github.ajalt.clikt:clikt:${Versions.CLIKT}"
    const val KOTLINX_SERIALIZATION_CORE = "org.jetbrains.kotlinx:kotlinx-serialization-core:${Versions.KOTLINX_SERIALIZATION}"
    const val KOTLINX_SERIALIZATION_JSON = "org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.KOTLINX_SERIALIZATION}"
    const val SNAKE_YAML = "org.yaml:snakeyaml:${Versions.SNAKE_YAML}"

    const val JUNIT_ENGINE = "org.junit.jupiter:junit-jupiter-engine:${Versions.JUNIT}"
    const val JUNIT_API = "org.junit.jupiter:junit-jupiter-api:${Versions.JUNIT}"
    const val JUNIT_PARAMS = "org.junit.jupiter:junit-jupiter-params:${Versions.JUNIT}"
    const val JUNIT_PLATFORM_LAUNCHER = "org.junit.platform:junit-platform-launcher:${Versions.JUNIT_PLATFORM}"
    const val GOOGLE_TRUTH = "com.google.truth:truth:${Versions.GOOGLE_TRUTH}"
    const val GOOGLE_GUAVA = "com.google.guava:guava:${Versions.GOOGLE_GUAVA}"

    const val KOTLIN_REACT = "org.jetbrains.kotlin-wrappers:kotlin-react:${Versions.KOTLIN_REACT}"
    const val KOTLIN_REACT_DOM = "org.jetbrains.kotlin-wrappers:kotlin-react-dom:${Versions.KOTLIN_REACT}"
    const val KOTLIN_REACT_ROUTER = "org.jetbrains.kotlin-wrappers:kotlin-react-router-dom:${Versions.KOTLIN_REACT_ROUTER}"
    const val KOTLIN_JS_EXTENSIONS = "org.jetbrains.kotlin-wrappers:kotlin-extensions:${Versions.KOTLIN_JS_EXTENSIONS}"

    const val SELENIUM_WEBDRIVER = "org.seleniumhq.selenium:selenium-java:${Versions.SELENIUM_WEBDRIVER}"
    const val SELENIUM_WEBDRIVER_MANAGER = "io.github.bonigarcia:webdrivermanager:${Versions.SELENIUM_WEBDRIVER_MANAGER}"

    const val REACT = "react"
    const val REACT_DOM = "react-dom"
    const val BOOTSTRAP = "bootstrap"
    const val POPPERJS_CORE = "@popperjs/core"
    const val APEX_CHARTS = "apexcharts"

    object Versions {
        const val ANDROID_GRADLE_PLUGIN = "8.13.1" // https://mvnrepository.com/artifact/com.android.tools.build/gradle?repo=google
        const val KOTLIN = "2.2.21" // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-stdlib
        const val DETEKT_GRADLE_PLUGIN = "1.23.8" // https://mvnrepository.com/artifact/io.gitlab.arturbosch.detekt/detekt-gradle-plugin
        const val NEXUS_PUBLISH_GRADLE_PLUGIN = "2.0.0" // https://mvnrepository.com/artifact/io.github.gradle-nexus.publish-plugin/io.github.gradle-nexus.publish-plugin.gradle.plugin
        const val SHADOW_GRADLE_PLUGIN = "9.2.2" // https://plugins.gradle.org/plugin/com.github.johnrengelman.shadow

        const val BUNDLETOOL = "1.18.2" // https://mvnrepository.com/artifact/com.android.tools.build/bundletool
        const val PROTOBUF = "4.33.1" // https://mvnrepository.com/artifact/com.google.protobuf/protobuf-java
        const val DEXLIB = "3.0.9" //  https://mvnrepository.com/artifact/com.android.tools.smali/smali-dexlib2

        const val ANDROID_TOOLS = "31.13.1" // https://mvnrepository.com/artifact/com.android.tools/common?repo=google
        const val CLIKT = "5.0.3" // https://mvnrepository.com/artifact/com.github.ajalt.clikt/clikt
        const val KOTLINX_SERIALIZATION = "1.9.0" // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-serialization-core
        const val SNAKE_YAML = "2.5" // https://mvnrepository.com/artifact/org.yaml/snakeyaml
        const val CONSTRAINT_LAYOUT = "2.2.1" // https://mvnrepository.com/artifact/androidx.constraintlayout/constraintlayout

        const val JUNIT = "6.0.1" // https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-engine
        const val JUNIT_PLATFORM = "1.12.1" // https://mvnrepository.com/artifact/org.junit.platform/junit-platform-launcher
        const val GOOGLE_TRUTH = "1.4.5" // https://mvnrepository.com/artifact/com.google.truth/truth
        const val GOOGLE_GUAVA = "31.1-jre" // https://mvnrepository.com/artifact/com.google.guava/guava

        const val KOTLIN_REACT = "2025.11.11-19.2.0" // https://mvnrepository.com/artifact/org.jetbrains.kotlin-wrappers/kotlin-react - Stable version for Kotlin 1.9
        const val KOTLIN_REACT_ROUTER = "2025.1.6-6.28.0" // https://mvnrepository.com/artifact/org.jetbrains.kotlin-wrappers/kotlin-react-router-dom - Stable version for Kotlin 1.9
        const val KOTLIN_JS_EXTENSIONS = "1.0.1-pre.823" // https://mvnrepository.com/artifact/org.jetbrains.kotlin-wrappers/kotlin-extensions - Stable version for Kotlin 1.9

        const val SELENIUM_WEBDRIVER = "4.38.0" // https://mvnrepository.com/artifact/org.seleniumhq.selenium/selenium-java
        const val SELENIUM_WEBDRIVER_MANAGER = "6.3.3" // https://mvnrepository.com/artifact/io.github.bonigarcia/webdrivermanager

        const val REACT = "19.2.0" // https://www.npmjs.com/package/react - Matching React wrapper version
        const val BOOTSTRAP = "5.3.8" // https://www.npmjs.com/package/bootstrap
        const val POPPERJS_CORE = "2.11.8" // https://www.npmjs.com/package/@popperjs/core - Required peer dependency for Bootstrap
        const val APEX_CHARTS = "5.3.6" // https://www.npmjs.com/package/apexcharts
    }
}
