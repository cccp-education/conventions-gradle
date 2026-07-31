package build

import io.cucumber.java8.En
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File

class KoverConventionsSteps : En {

    private lateinit var testProjectDir: File
    private lateinit var taskListResult: BuildResult

    init {
        Given("a project applies the kover conventions plugin") {
            testProjectDir = createTempDir("kover-test-")
            testProjectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"test-project\"")
            testProjectDir.resolve("build.gradle.kts").writeText("""
                plugins {
                    id("education.cccp.build.kover")
                }
            """)
            taskListResult = runTasks("tasks", "--all")
        }

        Then("the kover plugin is not applied") {
            assert(!taskListResult.output.contains("koverXmlReport")) {
                "Expected koverXmlReport task to NOT be present when disabled\n${taskListResult.output}"
            }
        }

        Given("a project applies the kover conventions plugin with enabled true") {
            testProjectDir = createTempDir("kover-test-")
            testProjectDir.resolve("settings.gradle.kts").writeText("""
                rootProject.name = "test-project"
                pluginManagement {
                    repositories {
                        mavenLocal()
                        gradlePluginPortal()
                        mavenCentral()
                    }
                }
            """)
            testProjectDir.resolve("build.gradle.kts").writeText("""
                plugins {
                    id("org.jetbrains.kotlinx.kover") version "0.9.8"
                    id("education.cccp.build.kover")
                }
                koverConventions {
                    enabled = true
                }
            """)
            taskListResult = runTasks("tasks", "--all")
        }

        Then("the kover plugin is applied") {
            assert(taskListResult.output.contains("koverXmlReport")) {
                "Expected koverXmlReport task to be present when enabled\n${taskListResult.output}"
            }
        }
    }

    private fun runTasks(vararg args: String): BuildResult {
        return GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments(*args)
            .withPluginClasspath()
            .build()
    }

    private fun createTempDir(prefix: String): File {
        val dir = File.createTempFile(prefix, "")
        dir.delete()
        dir.mkdir()
        dir.deleteOnExit()
        return dir
    }
}
