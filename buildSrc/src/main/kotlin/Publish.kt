import org.gradle.api.Project
import org.gradle.api.publish.PublicationContainer
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugins.signing.SigningExtension

const val RULER_PLUGIN_GROUP = "net.kibotu"
const val RULER_PLUGIN_VERSION = "3.0.0"
const val EXT_POM_NAME = "POM_NAME"
const val EXT_POM_DESCRIPTION = "POM_DESCRIPTION"

const val ENV_SONATYPE_USERNAME = "SONATYPE_USERNAME"
const val ENV_SONATYPE_PASSWORD = "SONATYPE_PASSWORD"

const val ENV_SIGNING_KEY = "PGP_SIGNING_KEY"
const val ENV_SIGNING_PASSWORD = "PGP_SIGNING_PASSWORD"

fun PublishingExtension.configurePublications(project: Project) {
    publications.withType(MavenPublication::class.java) {
        groupId = RULER_PLUGIN_GROUP
        version = project.version.toString()
    }
}

fun SigningExtension.configureSigning(publications: PublicationContainer) {
    val signingKey = System.getenv(ENV_SIGNING_KEY)
    val signingPassword = System.getenv(ENV_SIGNING_PASSWORD)

    isRequired = signingKey != null && signingPassword != null

    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publications)
    }
}
