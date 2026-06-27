package education.cccp.build

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
    var publicationType: String = "LIBRARY"
    var relocationGroupId: String? = null
    var relocationArtifactId: String? = null
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

        publication.pom(
            Action { pom: MavenPom ->
                pom.url.set(website)

                pom.developers(
                    Action { devs: MavenPomDeveloperSpec ->
                        devs.developer(
                            Action { dev ->
                                dev.id.set("cccp-education")
                                dev.name.set("CCCP Education")
                                dev.email.set("cccp@cccp.education")
                            }
                        )
                    }
                )

                pom.scm(
                    Action { scm: MavenPomScm ->
                        val cleanUrl = vcsUrl.removePrefix("https://")
                        scm.connection.set("scm:git:git://$cleanUrl.git")
                        scm.developerConnection.set("scm:git:ssh://$cleanUrl.git")
                        scm.url.set(vcsUrl)
                    }
                )

                pom.licenses(
                    Action { lic: MavenPomLicenseSpec ->
                        lic.license(
                            Action { license ->
                                license.name.set("Apache-2.0")
                                license.url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
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

    private fun shouldSign(project: Project): Boolean {
        val isCI = System.getenv("CI") == "true"
        val isSnapshot = project.version.toString().endsWith("-SNAPSHOT")
        return !isCI && !isSnapshot
    }

    private fun resolveWebsite(project: Project, extension: PublishingConventionsExtension): String {
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
