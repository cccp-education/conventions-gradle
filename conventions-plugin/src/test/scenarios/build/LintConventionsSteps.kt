package build

import io.cucumber.java8.En
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File

class LintConventionsSteps : En {
    private lateinit var testProjectDir: File
    private lateinit var taskListResult: BuildResult

    init {
        Given("a project applies the lint plugin") {
            testProjectDir = createTempDir("lint-test-")
            testProjectDir.resolve("settings.gradle.kts").writeText("""
                rootProject.name = "test-project"
                pluginManagement {
                    repositories {
                        mavenCentral()
                        gradlePluginPortal()
                    }
                }
            """)
            testProjectDir.resolve("build.gradle.kts").writeText("""
                plugins {
                    id("education.cccp.build.lint")
                }
            """)
            taskListResult = runTasks("tasks", "--all")
        }

        Then("the ktlint plugin is applied") {
            assert(taskListResult.output.contains("ktlint")) {
                "Expected ktlint tasks in output\n${taskListResult.output}"
            }
        }

        Then("the ktlint tasks are available") {
            assert(taskListResult.output.contains("ktlint")) {
                "Expected ktlint tasks in output\n${taskListResult.output}"
            }
        }

        // ── CNV-10.5 — detekt branch coverage ────────────────────────────────
        Then("the plugin does not fail when detekt is conditionally applied") {
            testProjectDir.resolve("build.gradle.kts").writeText("""
                plugins {
                    id("java-base")
                    id("education.cccp.build.lint")
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
            assert(result.output.contains("BUILD SUCCESSFUL")) {
                "Expected build to succeed with conditional detekt\n${result.output}"
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
            testProjectDir = createTempDir("lint-test-")
            testProjectDir.resolve("settings.gradle.kts").writeText("""
                rootProject.name = "test-project"
                pluginManagement {
                    repositories {
                        mavenCentral()
                        gradlePluginPortal()
                    }
                }
            """)
            testProjectDir.resolve("build.gradle.kts").writeText("""
                plugins {
                    id("education.cccp.build.lint")
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
