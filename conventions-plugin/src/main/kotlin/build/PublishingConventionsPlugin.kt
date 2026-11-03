package build

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPomDeveloperSpec
import org.gradle.api.publish.maven.MavenPomLicenseSpec
import org.gradle.api.publish.maven.MavenPomScm
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension
import org.gradle.plugins.signing.SigningExtension
import org.gradle.api.publish.maven.MavenPomDistributionManagement
import org.gradle.api.publish.maven.MavenPomRelocation
import javax.inject.Inject

open class PublishingConventionsExtension @Inject constructor() {
    var publicationType: String = "PLUGIN"
    var relocationGroupId: String? = null
    var relocationArtifactId: String? = null

    /**
     * POM identity overrides. `null` (default) keeps the cccp defaults
     * (Apache-2.0 + cccp-education), so existing consumers are unchanged.
     * A non-cccp consumer sets these to publish under its own licence and
     * developer (CNV-13 — proprietary payment-scaffold providers).
     */
    var licenseName: String? = null
    var licenseUrl: String? = null
    var developerId: String? = null
    var developerName: String? = null
    var developerEmail: String? = null

    /** Explicit POM url override (highest precedence over gradlePlugin.website). */
    var website: String? = null

    /** Explicit SCM url override (highest precedence over gradlePlugin.vcsUrl). */
    var vcsUrl: String? = null
}

class PublishingConventionsPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.pluginManager.apply("maven-publish")

        val extension = project.extensions.create(
            "publishingConventions",
            PublishingConventionsExtension::class.java
        )

        project.afterEvaluate {
            configurePublishing(project, extension)
            configureSigning(project)
        }
    }

    private fun configurePublishing(project: Project, extension: PublishingConventionsExtension) {
        val publishing = project.extensions.getByType(PublishingExtension::class.java)
        publishing.publications.withType(MavenPublication::class.java).configureEach(
            Action { publication ->
                configurePom(project, extension, publication)
            }
        )
    }

    private fun configurePom(project: Project, extension: PublishingConventionsExtension, publication: MavenPublication) {
        val website = resolveWebsite(project, extension)
        val vcsUrl = resolveVcsUrl(project, extension)
        val identity = PublishingIdentity.resolve(
            licenseName = extension.licenseName,
            licenseUrl = extension.licenseUrl,
            developerId = extension.developerId,
            developerName = extension.developerName,
            developerEmail = extension.developerEmail,
        )

        publication.pom(
            Action { pom: MavenPom ->
                pom.url.set(website)

                pom.developers(
                    Action { devs: MavenPomDeveloperSpec ->
                        devs.developer(
                            Action { dev ->
                                dev.id.set(identity.developerId)
                                dev.name.set(identity.developerName)
                                dev.email.set(identity.developerEmail)
                            }
                        )
                    }
                )

                pom.scm(
                    Action { scm: MavenPomScm ->
                        val cleanUrl = vcsUrl.removePrefix("https://").removeSuffix(".git")
                        scm.connection.set("scm:git:git://$cleanUrl.git")
                        scm.developerConnection.set("scm:git:ssh://$cleanUrl.git")
                        scm.url.set(vcsUrl)
                    }
                )

                pom.licenses(
                    Action { lic: MavenPomLicenseSpec ->
                        lic.license(
                            Action { license ->
                                license.name.set(identity.licenseName)
                                license.url.set(identity.licenseUrl)
                            }
                        )
                    }
                )

                val relGroupId = extension.relocationGroupId
                val relArtifactId = extension.relocationArtifactId
                if (relGroupId != null || relArtifactId != null) {
                    pom.distributionManagement(
                        Action { dm: MavenPomDistributionManagement ->
                            dm.relocation(
                                Action { r: MavenPomRelocation ->
                                    relGroupId?.let { r.groupId.set(it) }
                                    relArtifactId?.let { r.artifactId.set(it) }
                                }
                            )
                        }
                    )
                }
            }
        )
    }

    private fun configureSigning(project: Project) {
        if (shouldSign(project)) {
            project.pluginManager.apply("signing")
            val publishing = project.extensions.getByType(PublishingExtension::class.java)
            val signing = project.extensions.getByType(SigningExtension::class.java)
            signing.useGpgCmd()

            signing.setRequired { shouldSign(project) }

            if (publishing.publications.isNotEmpty()) {
                signing.sign(publishing.publications)
            }
        }
    }

    private fun shouldSign(project: Project): Boolean =
        SigningPolicy.shouldSign(
            isCi = System.getenv(SigningPolicy.CI_ENV_VAR) == "true",
            isPublishing = System.getenv(SigningPolicy.PUBLISH_ENV_VAR) == "true",
            version = project.version.toString()
        )

    private fun resolveWebsite(project: Project, extension: PublishingConventionsExtension): String {
        extension.website?.takeIf { it.isNotBlank() }?.let { return it }
        if (extension.publicationType == "PLUGIN") {
            try {
                val gradlePlugin = project.extensions.getByType(GradlePluginDevelopmentExtension::class.java)
                if (gradlePlugin.website.isPresent) {
                    return gradlePlugin.website.get()
                }
            } catch (_: Exception) {
            }
        }
        return "https://github.com/cccp-education"
    }

    private fun resolveVcsUrl(project: Project, extension: PublishingConventionsExtension): String {
        extension.vcsUrl?.takeIf { it.isNotBlank() }?.let { return it }
        if (extension.publicationType == "PLUGIN") {
            try {
                val gradlePlugin = project.extensions.getByType(GradlePluginDevelopmentExtension::class.java)
                if (gradlePlugin.vcsUrl.isPresent) {
                    return gradlePlugin.vcsUrl.get()
                }
            } catch (_: Exception) {
            }
        }
        return "https://github.com/cccp-education"
    }
}
