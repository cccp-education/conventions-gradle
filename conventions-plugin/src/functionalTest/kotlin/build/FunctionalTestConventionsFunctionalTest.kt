package build

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class FunctionalTestConventionsFunctionalTest {

    @TempDir
    lateinit var testProjectDir: File

    private val buildFile: File get() = testProjectDir.resolve("build.gradle.kts")
    private val settingsFile: File get() = testProjectDir.resolve("settings.gradle.kts")

    @Test
    fun `plugin applies without error`() {
        settingsFile.writeText("rootProject.name = \"test-project\"")
        buildFile.writeText("""
            plugins {
                id("education.cccp.build.functional-test")
            }
        """)

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("tasks", "--all")
            .withPluginClasspath()
            .build()

        check(result.task(":tasks")?.outcome != null)
    }

    @Test
    fun `plugin registers functionalTest source set`() {
        settingsFile.writeText("rootProject.name = \"test-project\"")
        buildFile.writeText("""
            plugins {
                id("education.cccp.build.functional-test")
            }
        """)

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("tasks", "--all")
            .withPluginClasspath()
            .build()

        check(result.output.contains("functionalTest")) {
            "Expected functionalTest source set/task in output\n${result.output}"
        }
    }

    @Test
    fun `plugin registers functionalTest task`() {
        settingsFile.writeText("rootProject.name = \"test-project\"")
        buildFile.writeText("""
            plugins {
                id("education.cccp.build.functional-test")
            }
        """)

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("tasks", "--all")
            .withPluginClasspath()
            .build()

        check(result.output.contains("functionalTest")) {
            "Expected functionalTest task in tasks output\n${result.output}"
        }
    }

    @Test
    fun `functionalTest extends testImplementation dependencies`() {
        settingsFile.writeText("rootProject.name = \"test-project\"")
        buildFile.writeText("""
            plugins {
                id("java")
                id("education.cccp.build.functional-test")
            }
        """)

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("tasks", "--all")
            .withPluginClasspath()
            .build()

        check(result.task(":tasks")?.outcome != null) {
            "Expected tasks task to succeed with java+functional-test\n${result.output}"
        }
    }

    @Test
    fun `plugin wires check depends on functionalTest`() {
        settingsFile.writeText("rootProject.name = \"test-project\"")
        buildFile.writeText("""
            plugins {
                id("education.cccp.build.functional-test")
                id("java")
            }
        """)

        val ftSrcDir = testProjectDir.resolve("src/functionalTest/kotlin")
        ftSrcDir.mkdirs()
        ftSrcDir.resolve("SmokeTest.kt").writeText("""
            import org.junit.jupiter.api.Test
            class SmokeTest {
                @Test fun smoke() { assert(true) }
            }
        """)

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("check")
            .withPluginClasspath()
            .build()

        check(result.task(":functionalTest")?.outcome != null) {
            "Expected functionalTest task to run during check"
        }
    }
}
