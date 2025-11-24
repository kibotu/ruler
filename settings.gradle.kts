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

rootProject.name = "ruler"

val withoutSample = providers.gradleProperty("withoutSample").orNull
val skipSample = withoutSample != null || startParameter.projectProperties.containsKey("withoutSample")

if (!skipSample) {
    include(":ruler-frontend")
    include(":ruler-frontend-tests")
}

include(":ruler-gradle-plugin")
include(":ruler-models")
include(":ruler-common")
include(":ruler-cli")

if (!skipSample) {
    include(":ruler-e2e-tests")

    include(":sample:app")
    include(":sample:dynamic")
    include(":sample:lib")
}

//plugins {
//    id("com.gradle.develocity") version "4.2.2" // https://mvnrepository.com/artifact/com.gradle.develocity/com.gradle.develocity.gradle.plugin
//}
//val isCiBuild = System.getenv("CI").toBoolean()
//if (isCiBuild) {
//    develocity {
//
//        buildScan {
//            termsOfUseUrl = "https://gradle.com/terms-of-service"
//            termsOfUseAgree = "yes"
//            publishing.onlyIf { isCiBuild }
//        }
//    }
//}
