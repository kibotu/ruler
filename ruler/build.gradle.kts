import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.time.Duration

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
    alias(libs.plugins.pluginPublish)
    `java-gradle-plugin`
    signing
}

kotlin {
    jvmToolchain(17)
}

java {
    withSourcesJar()
    withJavadocJar()
}

gradlePlugin {
    website = "https://github.com/kibotu/ruler"
    vcsUrl = "https://github.com/kibotu/ruler.git"

    plugins {
        create("ruler") {
            id = "net.kibotu.ruler"
            displayName = "Ruler"
            description = "Measures the size of your Android app, file by file."
            tags = listOf("android", "apk", "aab", "size", "analysis")
            implementationClass = "com.kibotu.ruler.plugin.RulerPlugin"
        }
    }
}

// TestKit injects a single classpath into the build under test. AGP has to travel with the
// plugin, so that both land in the same classloader and Ruler can see the Android variant API.
// AGP 9 builds Kotlin itself, so the Kotlin plugin and its daemon come along too.
val functionalTestClasspath: Configuration by configurations.creating

dependencies {
    functionalTestClasspath(libs.android.gradlePlugin)
    functionalTestClasspath(libs.kotlin.gradlePlugin)
    functionalTestClasspath(libs.kotlin.daemonClient)
    functionalTestClasspath(libs.kotlin.daemonEmbeddable)

    compileOnly(gradleApi())
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.bundletool)
    compileOnly(libs.protobuf)
    compileOnly(libs.android.tools.common)
    compileOnly(libs.android.tools.sdklib)
    compileOnly(libs.dexlib)

    implementation(libs.android.tools.apkanalyzer) {
        exclude(group = "com.android.tools.lint")
    }
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.snakeyaml)

    testImplementation(gradleTestKit())
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.params)
    testImplementation(libs.truth)
    testImplementation(libs.android.gradlePlugin)
    testRuntimeOnly(libs.junit.engine)
    testRuntimeOnly(libs.junit.platformLauncher)
}

// Relocate the runtime dependencies. Gradle plugins share a classpath, so an
// unshaded snakeyaml or kotlinx-serialization would collide with other plugins.
val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier = ""
    relocate("kotlinx.serialization", "com.kibotu.ruler.shadow.kotlinx.serialization")
    relocate("org.yaml.snakeyaml", "com.kibotu.ruler.shadow.org.yaml.snakeyaml")
    exclude("META-INF/maven/**")
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
    mergeServiceFiles()
    configurations = listOf(project.configurations.runtimeClasspath.get())
}

// Hand the relocated jar to every consumer, including project dependencies from
// the sample's composite build. Disabling the `jar` task instead would leave the
// outgoing artifact pointing at a file that is never produced.
tasks.named<Jar>("jar") {
    archiveClassifier = "plain"
}

listOf("apiElements", "runtimeElements").forEach { name ->
    configurations.named(name) {
        outgoing.artifacts.clear()
        outgoing.artifact(shadowJar)
    }
}

// Apache 2.0 requires the license and the attribution notice to travel with the artifact.
tasks.withType<Jar>().configureEach {
    from(rootDir) {
        include("LICENSE", "NOTICE")
        into("META-INF")
    }
}

tasks.register<JavaExec>("previewReport") {
    group = "ruler"
    description = "Renders report.html from a report.json (defaults to the test fixture)"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "com.kibotu.ruler.report.PreviewReportKt"
    if (project.hasProperty("json")) {
        args(project.property("json").toString())
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    timeout = Duration.ofMinutes(10)
    jvmArgs("-Xmx2g")
    dependsOn(tasks.pluginUnderTestMetadata)
    systemProperty(
        "pluginClasspath",
        provider {
            val plugin = tasks.pluginUnderTestMetadata.get().pluginClasspath
            (plugin + functionalTestClasspath).joinToString(File.pathSeparator) { it.absolutePath }
        },
    )
    testLogging {
        events("passed", "skipped", "failed")
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name = "Ruler"
            description = "Measures the size of your Android app, file by file."
            url = "https://github.com/kibotu/ruler"
            inceptionYear = "2021"
            licenses {
                license {
                    name = "The Apache License, Version 2.0"
                    url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    distribution = "repo"
                }
            }
            developers {
                developer {
                    id = "kibotu"
                    name = "Jan Rabe"
                    url = "https://github.com/kibotu"
                }
            }
            scm {
                url = "https://github.com/kibotu/ruler"
                connection = "scm:git:https://github.com/kibotu/ruler.git"
                developerConnection = "scm:git:ssh://git@github.com/kibotu/ruler.git"
            }
        }
    }
}

signing {
    val key = System.getenv("PGP_SIGNING_KEY")
    val password = System.getenv("PGP_SIGNING_PASSWORD")
    isRequired = key != null && password != null
    if (isRequired) {
        useInMemoryPgpKeys(key, password)
        sign(publishing.publications)
    }
}
