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

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("maven-publish")
    id("signing")
    id("io.gitlab.arturbosch.detekt")
}

extra[EXT_POM_NAME] = "Ruler models"
extra[EXT_POM_DESCRIPTION] = "Common models used by the Ruler Gradle plugin"

kotlin {
    jvm()
    js(IR) {
        browser()
        nodejs()
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(Dependencies.KOTLINX_SERIALIZATION_CORE)
            }
        }
    }
}

// Ensure compatible Node.js version for JitPack
rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin> {
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>().apply {
        nodeVersion = "16.13.0"
        nodeDownloadBaseUrl = "https://nodejs.org/dist"
    }
}

rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().apply {
        version = "1.22.19"
    }
}

publishing {
    configurePublications(project)
}

signing {
    configureSigning(publishing.publications)
}
