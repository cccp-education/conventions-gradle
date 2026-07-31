package build

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertTrue

class LintConventionsFunctionalTest {

    @TempDir
    lateinit var testProjectDir: File

    private val buildFile: File get() = testProjectDir.resolve("build.gradle.kts")
    private val settingsFile: File get() = testProjectDir.resolve("settings.gradle.kts")

    private fun writeSettings() {
        settingsFile.writeText("""
            rootProject.name = "test-project"
            pluginManagement {
                repositories {
                    mavenCentral()
                    gradlePluginPortal()
                }
            }
        """)
    }

    @Test
    fun `plugin applies without error`() {
        writeSettings()
        buildFile.writeText("""
            plugins {
                id("education.cccp.build.lint")
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
    fun `plugin registers ktlint tasks`() {
        writeSettings()
        buildFile.writeText("""
            plugins {
                id("education.cccp.build.lint")
            }
        """)

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("tasks", "--all")
            .withPluginClasspath()
            .build()

        assertTrue(result.output.contains("ktlint"))
    }

    @Test
    fun `plugin registers detekt task`() {
        writeSettings()
        buildFile.writeText("""
            plugins {
                id("education.cccp.build.lint")
            }
        """)

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("tasks", "--all")
            .withPluginClasspath()
            .build()

        val java25 = System.getProperty("java.version").startsWith("25")
        if (java25) {
            assertTrue(!result.output.contains(":detekt"))
        } else {
            assertTrue(result.output.contains(":detekt"))
        }
    }

    @Test
    fun `check task depends on detekt`() {
        writeSettings()
        buildFile.writeText("""
            plugins {
                id("education.cccp.build.lint")
                id("java-base")
            }
            repositories {
                mavenCentral()
            }
        """)

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("check", "--dry-run")
            .withPluginClasspath()
            .build()

        val java25 = System.getProperty("java.version").startsWith("25")
        if (java25) {
            assertTrue(!result.output.contains(":detekt"), "detekt should be skipped on Java 25")
        } else {
            assertTrue(result.output.contains(":detekt"))
        }
    }
}
