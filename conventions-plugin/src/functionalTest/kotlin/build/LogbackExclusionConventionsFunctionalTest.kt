package build

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LogbackExclusionConventionsFunctionalTest {

    @TempDir
    lateinit var testProjectDir: File

    private val buildFile: File get() = testProjectDir.resolve("build.gradle.kts")
    private val settingsFile: File get() = testProjectDir.resolve("settings.gradle.kts")

    @Test
    fun `plugin applies without error`() {
        settingsFile.writeText("rootProject.name = \"test-project\"")
        buildFile.writeText("""
            plugins {
                id("java")
                id("education.cccp.build.logback-exclusion")
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
    fun `plugin excludes logback-classic from testRuntimeClasspath`() {
        settingsFile.writeText("rootProject.name = \"test-project\"")
        buildFile.writeText("""
            plugins {
                id("java")
                id("education.cccp.build.logback-exclusion")
            }
            dependencies {
                testRuntimeOnly("ch.qos.logback:logback-classic:1.5.26")
                testImplementation("org.slf4j:slf4j-api:2.0.17")
            }
        """)

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("dependencies", "--configuration", "testRuntimeClasspath")
            .withPluginClasspath()
            .build()

        assertFalse(result.output.contains("ch.qos.logback:logback-classic"),
            "Expected logback-classic excluded from testRuntimeClasspath\n${result.output}")
        assertTrue(result.output.contains("org.slf4j:slf4j-api"),
            "Expected slf4j-api preserved in testRuntimeClasspath\n${result.output}")
    }

    @Test
    fun `plugin excludes logback-classic from testImplementation`() {
        settingsFile.writeText("rootProject.name = \"test-project\"")
        buildFile.writeText("""
            plugins {
                id("java")
                id("education.cccp.build.logback-exclusion")
            }
            dependencies {
                testRuntimeOnly("ch.qos.logback:logback-classic:1.5.26")
                testImplementation("org.slf4j:slf4j-api:2.0.17")
            }
        """)

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("dependencies", "--configuration", "testRuntimeClasspath")
            .withPluginClasspath()
            .build()

        assertFalse(result.output.contains("ch.qos.logback:logback-classic"),
            "Expected logback-classic excluded from testRuntimeClasspath\n${result.output}")
        assertTrue(result.output.contains("org.slf4j:slf4j-api"),
            "Expected slf4j-api preserved in testRuntimeClasspath\n${result.output}")
    }
}
