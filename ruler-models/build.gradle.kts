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
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(Dependencies.KOTLINX_SERIALIZATION_CORE)
            }
        }
    }
}

// Create empty javadoc JAR for local publishing
val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

publishing {
    publications.withType<MavenPublication> {
        artifact(javadocJar)
        // Don't apply standard POM configuration for internal modules
        groupId = RULER_PLUGIN_GROUP
        version = findProperty("version")?.toString() ?: RULER_PLUGIN_VERSION
    }
    // Only publish to mavenLocal, not to Maven Central
    // This is an internal dependency used by the Gradle plugin
    repositories {
        mavenLocal()
    }
}

// Don't sign internal modules - they're not published to Maven Central
// signing {
//     configureSigning(publishing.publications)
// }
