package build

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertTrue

class PublishingConventionsFunctionalTest {

    @TempDir
    lateinit var testProjectDir: File

    private val buildFile: File get() = testProjectDir.resolve("build.gradle.kts")
    private val settingsFile: File get() = testProjectDir.resolve("settings.gradle.kts")

    @Test
    fun `plugin applies without error`() {
        settingsFile.writeText("rootProject.name = \"test-project\"")
        buildFile.writeText("""
            plugins {
                id("education.cccp.build.publishing")
            }
        """)

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("tasks", "--all")
            .withPluginClasspath()
            .build()

        assertTrue(result.task(":tasks")?.outcome != null)
    }

    @Test
    fun `plugin registers generatePom task`() {
        settingsFile.writeText("rootProject.name = \"test-project\"")
        buildFile.writeText("""
            plugins {
                id("java-library")
                id("education.cccp.build.publishing")
            }

            group = "com.example"
            version = "1.0.0"

            publishing {
                publications {
                    register("maven", org.gradle.api.publish.maven.MavenPublication::class.java) {
                        from(components["java"])
                    }
                }
            }
        """)

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("tasks", "--all")
            .withPluginClasspath()
            .build()

        assertTrue(result.output.contains("generatePom"))
    }

    @Test
    fun `plugin generates pom with developers`() {
        settingsFile.writeText("rootProject.name = \"test-project\"")
        buildFile.writeText("""
            plugins {
                id("java-library")
                id("education.cccp.build.publishing")
            }

            group = "com.example"
            version = "1.0.0"

            publishing {
                publications {
                    register("maven", org.gradle.api.publish.maven.MavenPublication::class.java) {
                        from(components["java"])
                    }
                }
            }
        """)

        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("generatePomFileForMavenPublication")
            .withPluginClasspath()
            .build()

        val pomFile = testProjectDir.resolve("build/publications/maven/pom-default.xml")
        assertTrue(pomFile.exists(), "POM file should exist at ${pomFile.absolutePath}")
        val pomContent = pomFile.readText()
        assertTrue(pomContent.contains("cccp-education"), "POM should contain developer id cccp-education\n$pomContent")
        assertTrue(pomContent.contains("Apache-2.0"), "POM should contain Apache-2.0 license\n$pomContent")
    }
}
