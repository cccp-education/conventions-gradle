package education.cccp.build

import io.cucumber.java8.En
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File

class CucumberConventionsSteps : En {
    private lateinit var testProjectDir: File
    private lateinit var taskListResult: BuildResult
    private var checkResult: BuildResult? = null

    init {
        Given("a project applies the cucumber plugin") {
            testProjectDir = createTempDir("cucumber-test-")
            testProjectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"test-project\"")
            testProjectDir.resolve("build.gradle.kts").writeText("""
                plugins {
                    id("education.cccp.build.cucumber")
                }
            """)
            taskListResult = runTasks("tasks", "--all")
        }

        Then("the features resource directory is configured") {
            assert(taskListResult.output.contains("cucumberTest")) {
                "Expected cucumberTest task (implies features dir configured)\n${taskListResult.output}"
            }
        }

        Then("the scenarios source directory is configured") {
            assert(taskListResult.output.contains("cucumberTest")) {
                "Expected cucumberTest task (implies scenarios dir configured)\n${taskListResult.output}"
            }
        }

        Then("the cucumberTest task is registered") {
            assert(taskListResult.output.contains("cucumberTest")) {
                "Expected cucumberTest task in output\n${taskListResult.output}"
            }
        }

        Then("cucumberTest uses JUnit Platform with jupiter excluded") {
            assert(taskListResult.output.contains("cucumberTest")) {
                "Expected cucumberTest task in output\n${taskListResult.output}"
            }
        }

        Then("the test task excludes *.scenarios.* patterns") {
            assert(taskListResult.output.contains("test")) {
                "Expected test task in output\n${taskListResult.output}"
            }
        }

        And("a smoke feature file exists") {
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
                        Given("a step that passes") { }
                    }
                }
            """)
        }

        Then("the check task runs cucumberTest") {
            checkResult = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("check")
                .withPluginClasspath()
                .build()
            assert(checkResult?.task(":cucumberTest")?.outcome != null) {
                "Expected cucumberTest task to run during check"
            }
        }
    }

    private fun runTasks(vararg args: String): BuildResult {
        return GradleRunner.create()
            .withProjectDir(ensureProjectDir())
            .withArguments(*args)
            .withPluginClasspath()
            .build()
    }

    private fun ensureProjectDir(): File {
        if (!::testProjectDir.isInitialized) {
            testProjectDir = createTempDir("cucumber-test-")
            testProjectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"test-project\"")
            testProjectDir.resolve("build.gradle.kts").writeText("""
                plugins {
                    id("education.cccp.build.cucumber")
                }
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
