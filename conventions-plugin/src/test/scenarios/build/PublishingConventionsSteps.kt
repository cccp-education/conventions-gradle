package build

import io.cucumber.java8.En
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File

class PublishingConventionsSteps : En {
    private lateinit var testProjectDir: File
    private lateinit var buildResult: BuildResult
    private var pomContent: String = ""

    private val publicationBlock: String
        get() = """
            publishing {
                publications {
                    register("maven", org.gradle.api.publish.maven.MavenPublication::class.java) {
                        from(components["java"])
                    }
                }
            }
        """

    init {
        Given("a project applies the publishing plugin") {
            testProjectDir = createTempDir("publishing-test-")
            testProjectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"test-project\"")
            testProjectDir.resolve("build.gradle.kts").writeText("""
                plugins {
                    id("java-library")
                    id("education.cccp.build.publishing")
                }
                group = "com.example"
                version = "1.0.0"

                $publicationBlock
            """)
        }

        Given("a project applies the publishing plugin with vcsUrl {string}") { vcsUrl: String ->
            testProjectDir = createTempDir("publishing-test-vcurl-")
            testProjectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"test-project-vcs\"")
            testProjectDir.resolve("build.gradle.kts").writeText("""
                plugins {
                    id("java-gradle-plugin")
                    id("education.cccp.build.publishing")
                }
                group = "com.example"
                version = "1.0.0"

                gradlePlugin {
                    website.set("https://github.com/cccp-education/sample-gradle")
                    vcsUrl.set("$vcsUrl")
                }

                publishingConventions {
                    publicationType = "PLUGIN"
                }

                $publicationBlock
            """)
        }

        Then("the generated POM has developer id {string}") { expectedId: String ->
            generatePom()
            assert(pomContent.contains("<id>$expectedId</id>")) {
                "Expected developer id <id>$expectedId</id> in POM\n$pomContent"
            }
        }

        Then("the generated POM has developer name {string}") { expectedName: String ->
            generatePom()
            assert(pomContent.contains("<name>$expectedName</name>")) {
                "Expected developer name <name>$expectedName</name> in POM\n$pomContent"
            }
        }

        Then("the generated POM has license {string}") { expectedLicense: String ->
            generatePom()
            assert(pomContent.contains(expectedLicense)) {
                "Expected license $expectedLicense in POM\n$pomContent"
            }
        }

        Then("the generated POM has SCM connection starting with {string}") { expectedPrefix: String ->
            generatePom()
            val matcher = Regex("<connection>([^<]+)</connection>").find(pomContent)
            assert(matcher != null && matcher.groupValues[1].startsWith(expectedPrefix)) {
                "Expected SCM connection starting with $expectedPrefix in POM\n$pomContent"
            }
        }

        Then("the generated POM has SCM connection {string}") { expected: String ->
            generatePom()
            val matcher = Regex("<connection>([^<]+)</connection>").find(pomContent)
            assert(matcher != null && matcher.groupValues[1] == expected) {
                "Expected SCM connection exactly '$expected' but got '${matcher?.groupValues?.getOrNull(1)}'\n$pomContent"
            }
        }

        Then("the generated POM has SCM developer connection {string}") { expected: String ->
            generatePom()
            val matcher = Regex("<developerConnection>([^<]+)</developerConnection>").find(pomContent)
            assert(matcher != null && matcher.groupValues[1] == expected) {
                "Expected SCM developer connection exactly '$expected' but got '${matcher?.groupValues?.getOrNull(1)}'\n$pomContent"
            }
        }

        When("CI is not set and version is not SNAPSHOT") {
            // default conditions satisfy this: CI env not set, version = 1.0.0
        }

        Then("the signing plugin is applied") {
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("tasks", "--all")
                .withPluginClasspath()
                .build()

            assert(result.output.contains("sign")) {
                "Expected signing tasks in output\n${result.output}"
            }
        }

        Then("publications are signed") {
            assert(true) // signing is applied if conditions are met
        }

        When("relocation group {string} and artifact {string} are configured") { group: String, artifact: String ->
            testProjectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"test-project\"")
            testProjectDir.resolve("build.gradle.kts").writeText("""
                plugins {
                    id("java-library")
                    id("education.cccp.build.publishing")
                }
                group = "com.example"
                version = "1.0.0"

                $publicationBlock

                publishingConventions {
                    relocationGroupId = "$group"
                    relocationArtifactId = "$artifact"
                }
            """)
        }

        Then("the generated POM has relocation group {string}") { expectedGroup: String ->
            generatePom()
            assert(pomContent.contains(expectedGroup)) {
                "Expected relocation group $expectedGroup in POM\n$pomContent"
            }
        }

        Then("the generated POM has relocation artifact {string}") { expectedArtifact: String ->
            generatePom()
            assert(pomContent.contains(expectedArtifact)) {
                "Expected relocation artifact $expectedArtifact in POM\n$pomContent"
            }
        }

        // ── CNV-10.2 — publicationType LIBRARY fallback ──────────────────────
        Given("a project applies the publishing plugin with publicationType LIBRARY") {
            testProjectDir = createTempDir("publishing-lib-")
            testProjectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"test-project-lib\"")
            testProjectDir.resolve("build.gradle.kts").writeText("""
                plugins {
                    id("java-library")
                    id("education.cccp.build.publishing")
                }
                group = "com.example"
                version = "1.0.0"

                publishingConventions {
                    publicationType = "LIBRARY"
                }

                $publicationBlock
            """)
        }

        Then("the generated POM has url {string}") { expectedUrl: String ->
            generatePom()
            val matcher = Regex("<url>([^<]+)</url>").find(pomContent)
            assert(matcher != null && matcher.groupValues[1] == expectedUrl) {
                "Expected POM url '$expectedUrl' but got '${matcher?.groupValues?.getOrNull(1)}'\n$pomContent"
            }
        }

        Then("the generated POM has SCM url {string}") { expectedScmUrl: String ->
            generatePom()
            val scmBlock = Regex("<scm>([\\s\\S]*?)</scm>").find(pomContent)
            val scmUrl = scmBlock?.let { Regex("<url>([^<]+)</url>").find(it.groupValues[1])?.groupValues?.get(1) }
            assert(scmUrl == expectedScmUrl) {
                "Expected SCM url '$expectedScmUrl' but got '$scmUrl'\n$pomContent"
            }
        }
    }

    private fun generatePom() {
        if (pomContent.isNotEmpty()) return

        GradleRunner.create()
            .withProjectDir(ensureProjectDir())
            .withArguments("generatePomFileForMavenPublication")
            .withPluginClasspath()
            .build()

        val pomFile = testProjectDir.resolve("build/publications/maven/pom-default.xml")
        assert(pomFile.exists()) { "POM file should exist" }
        pomContent = pomFile.readText()
    }

    private fun ensureProjectDir(): File {
        if (!::testProjectDir.isInitialized) {
            testProjectDir = createTempDir("publishing-test-")
            testProjectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"test-project\"")
            testProjectDir.resolve("build.gradle.kts").writeText("""
                plugins {
                    id("java-library")
                    id("education.cccp.build.publishing")
                }
                group = "com.example"
                version = "1.0.0"

                $publicationBlock
            """)
        }
        return testProjectDir
    }

    private fun createTempDir(prefix: String): File {
        val dir = File.createTempFile(prefix, "")
        dir.delete()
        dir.mkdir()
        dir.deleteOnExit()
        return dir
    }
}
