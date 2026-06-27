package build

import io.cucumber.java8.En
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File

class FunctionalTestConventionsSteps : En {
    private lateinit var testProjectDir: File
    private lateinit var taskListResult: BuildResult

    init {
        Given("a project applies the functional-test plugin") {
            testProjectDir = createTempDir("functional-test-")
            testProjectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"test-project\"")
            testProjectDir.resolve("build.gradle.kts").writeText("""
                plugins {
                    id("education.cccp.build.functional-test")
                }
            """)
            taskListResult = runTasks("tasks", "--all")
        }

        Then("the functionalTest source set is created") {
            assert(taskListResult.output.contains("functionalTest")) {
                "Expected functionalTest in tasks output\n${taskListResult.output}"
            }
        }

        Then("the functionalTest task is registered") {
            assert(taskListResult.output.contains("functionalTest")) {
                "Expected functionalTest task in output\n${taskListResult.output}"
            }
        }

        Then("the check task depends on functionalTest") {
            testProjectDir.resolve("build.gradle.kts").writeText("""
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
            assert(result.task(":functionalTest")?.outcome != null) {
                "Expected functionalTest task to run during check"
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
            testProjectDir = createTempDir("functional-test-")
            testProjectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"test-project\"")
            testProjectDir.resolve("build.gradle.kts").writeText("""
                plugins {
                    id("education.cccp.build.functional-test")
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
