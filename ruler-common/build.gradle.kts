plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    compileOnly(Dependencies.ANDROID_GRADLE_PLUGIN)

    implementation(Dependencies.BUNDLETOOL)
    implementation(Dependencies.PROTOBUF_CORE)
    implementation(Dependencies.ANDROID_TOOLS_COMMON)
    implementation(Dependencies.ANDROID_TOOLS_SDKLIB)
    implementation(Dependencies.DEXLIB)

    implementation(project(":ruler-models"))

    implementation(Dependencies.APK_ANALYZER) {
        exclude(group = "com.android.tools.lint")
    }
    implementation(Dependencies.KOTLINX_SERIALIZATION_JSON)
    implementation(Dependencies.SNAKE_YAML)

    testRuntimeOnly(Dependencies.JUNIT_ENGINE)
    testRuntimeOnly(Dependencies.JUNIT_PLATFORM_LAUNCHER)
    testImplementation(Dependencies.JUNIT_API)
    testImplementation(Dependencies.JUNIT_PARAMS)
    testImplementation(Dependencies.GOOGLE_TRUTH)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Task to preview the report without an Android build
// Usage: ./gradlew :ruler-common:previewReport [-Pjson=path/to/report.json]
tasks.register<JavaExec>("previewReport") {
    description = "Generates a preview report.html from a JSON fixture or real report"
    group = "ruler"

    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.spotify.ruler.common.report.PreviewReportKt")

    val jsonArg = project.findProperty("json")?.toString()
    if (jsonArg != null) {
        args = listOf(jsonArg)
    }
}
