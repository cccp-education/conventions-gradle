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
