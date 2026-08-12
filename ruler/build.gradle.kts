import java.time.Duration

buildscript {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    dependencies {
        classpath(Dependencies.SHADOW_GRADLE_PLUGIN)
        classpath(Dependencies.PLUGIN_PUBLISH_GRADLE_PLUGIN)
        classpath(Dependencies.ANDROID_GRADLE_PLUGIN)
    }
}

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("java-gradle-plugin")
    id("maven-publish")
    id("signing")
}

gradlePlugin {
    website.set("https://github.com/kibotu/ruler")
    vcsUrl.set("https://github.com/kibotu/ruler.git")

    plugins {
        create("rulerPlugin") {
            id = "net.kibotu.ruler"
            displayName = "Ruler - Android App Size Analyzer"
            description = "Gradle plugin for analyzing the size of your Android apps"
            tags.set(listOf("android", "apk", "size", "analysis", "bundle"))
            implementationClass = "com.kibotu.ruler.plugin.RulerPlugin"
        }
    }
}

apply(plugin = "com.gradle.plugin-publish")
apply(plugin = "com.gradleup.shadow")

dependencies {
    compileOnly(gradleApi())
    compileOnly(Dependencies.ANDROID_GRADLE_PLUGIN)
    compileOnly(Dependencies.BUNDLETOOL)
    compileOnly(Dependencies.PROTOBUF_CORE)
    compileOnly(Dependencies.ANDROID_TOOLS_COMMON)
    compileOnly(Dependencies.ANDROID_TOOLS_SDKLIB)
    compileOnly(Dependencies.DEXLIB)

    // These will be included in the fat JAR
    implementation(Dependencies.APK_ANALYZER) {
        exclude(group = "com.android.tools.lint")
    }
    implementation(Dependencies.KOTLINX_SERIALIZATION_JSON)
    implementation(Dependencies.SNAKE_YAML)

    testRuntimeOnly(Dependencies.JUNIT_ENGINE)
    testRuntimeOnly(Dependencies.JUNIT_PLATFORM_LAUNCHER)
    testImplementation(gradleTestKit())
    testImplementation(Dependencies.JUNIT_API)
    testImplementation(Dependencies.JUNIT_PARAMS)
    testImplementation(Dependencies.GOOGLE_TRUTH)
    testImplementation(Dependencies.ANDROID_GRADLE_PLUGIN)
}

tasks.register<JavaExec>("previewReport") {
    group = "ruler"
    description = "Generate an HTML preview from report.json (defaults to the test fixture)"
    classpath(sourceSets["main"].runtimeClasspath)
    mainClass.set("com.kibotu.ruler.common.report.PreviewReportKt")
    if (project.hasProperty("json")) {
        args(project.property("json").toString())
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    timeout.set(Duration.ofMinutes(10))
    jvmArgs("-Xmx2g")
    dependsOn(tasks.pluginUnderTestMetadata)
    systemProperty(
        "pluginClasspath",
        provider {
            tasks.pluginUnderTestMetadata.get().pluginClasspath.joinToString(File.pathSeparator) { it.absolutePath }
        },
    )
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
}

// Configure Shadow plugin to create fat JAR
tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveClassifier.set("")

    // Relocate packages to avoid conflicts
    relocate("kotlinx.serialization", "com.kibotu.ruler.shadow.kotlinx.serialization")
    relocate("org.yaml.snakeyaml", "com.kibotu.ruler.shadow.org.yaml.snakeyaml")

    // Exclude unnecessary files
    exclude("META-INF/maven/**")
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")

    // Keep the plugin descriptor
    mergeServiceFiles()

    // Ensure dependencies are included
    configurations = listOf(project.configurations.runtimeClasspath.get())
}

// Replace the default JAR with the shadow JAR
tasks.named("jar") {
    dependsOn("shadowJar")
    enabled = false
}

// Ensure shadowJar runs before assemble
tasks.named("assemble") {
    dependsOn("shadowJar")
}

publishing {
    configurePublications(project)
}

afterEvaluate {
    publishing {
        publications {
            named<MavenPublication>("pluginMaven") {
                artifacts.removeIf { it.classifier == null || it.classifier == "" }
                artifact(tasks.named("shadowJar")) {
                    classifier = ""
                }
            }
        }
    }
}

signing {
    configureSigning(publishing.publications)
}
