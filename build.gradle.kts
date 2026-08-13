import io.github.gradlenexus.publishplugin.NexusPublishExtension
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.plugins.signing.Sign

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.pluginPublish) apply false
    alias(libs.plugins.nexusPublish)
}

allprojects {
    group = "net.kibotu"
    findProperty("version")?.toString()?.let {
        version = it
    }
}

extensions.configure(NexusPublishExtension::class) {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))

            val username = System.getenv("SONATYPE_USERNAME")
            val password = System.getenv("SONATYPE_PASSWORD")
            if (username != null && password != null) {
                this.username.set(username)
                this.password.set(password)
            }
        }
    }
}

subprojects {
    tasks.withType<PublishToMavenRepository>().configureEach {
        mustRunAfter(tasks.withType<Sign>())
    }
}
