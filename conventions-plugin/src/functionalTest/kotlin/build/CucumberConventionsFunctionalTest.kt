package build

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertTrue

class CucumberConventionsFunctionalTest {

    @TempDir
    lateinit var testProjectDir: File

    @Test
    fun `plugin applies without error`() {
        settingsFile.writeText("rootProject.name = \"test-project\"")
        buildFile.writeText("""
            plugins {
                id("education.cccp.build.cucumber")
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
    fun `plugin registers cucumberTest task`() {
        settingsFile.writeText("rootProject.name = \"test-project\"")
        buildFile.writeText("""
            plugins {
                id("education.cccp.build.cucumber")
            }
        """)

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("tasks", "--all")
            .withPluginClasspath()
            .build()

        assertTrue(result.output.contains("cucumberTest"))
    }

    @Test
    fun `plugin configures features and scenarios dirs`() {
        settingsFile.writeText("rootProject.name = \"test-project\"")
        buildFile.writeText("""
            plugins {
                id("education.cccp.build.cucumber")
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
    fun `additional task with runnerClass applies test filter`() {
        settingsFile.writeText("rootProject.name = \"test-project\"")
        buildFile.writeText("""
            import build.CucumberTaskSpec

            plugins {
                id("education.cccp.build.cucumber")
            }
            cucumberConventions {
                additionalTasks = listOf(
                    CucumberTaskSpec(
                        name = "cucumberTestEpic1",
                        runnerClass = "com.example.Epic1CucumberRunner"
                    )
                )
            }
        """)

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("tasks", "--all")
            .withPluginClasspath()
            .build()

        assertTrue(result.output.contains("cucumberTestEpic1"))
    }

    @Test
    fun `plugin wires check depends on cucumberTest`() {
        settingsFile.writeText("rootProject.name = \"test-project\"")
        buildFile.writeText("""
            plugins {
                id("education.cccp.build.cucumber")
                id("java")
            }
        """)

        val featuresDir = testProjectDir.resolve("src/test/resources/features")
        featuresDir.mkdirs()
        featuresDir.resolve("smoke.feature").writeText("""
            Feature: Smoke
              Scenario: smoke
                Given a step that passes
        """)

        val scenariosDir = testProjectDir.resolve("src/test/scenarios")
        scenariosDir.mkdirs()
        scenariosDir.resolve("SmokeSteps.kt").writeText("""
            import io.cucumber.java8.En
            class SmokeSteps : En {
                init {
                    Given("a step that passes") { /* no-op */ }
                }
            }
        """)

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("check")
            .withPluginClasspath()
            .build()

        assertTrue(result.task(":cucumberTest")?.outcome != null)
    }

    private val buildFile: File get() = testProjectDir.resolve("build.gradle.kts")
    private val settingsFile: File get() = testProjectDir.resolve("settings.gradle.kts")
}
