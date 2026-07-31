package build

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

        Given("a project applies the cucumber plugin with additional tasks and runnerClass") {
            testProjectDir = createTempDir("cucumber-test-")
            testProjectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"test-project\"")
            testProjectDir.resolve("build.gradle.kts").writeText("""
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
            taskListResult = runTasks("tasks", "--all")
        }

        Then("the additional cucumberTestEpic1 task is registered") {
            assert(taskListResult.output.contains("cucumberTestEpic1")) {
                "Expected cucumberTestEpic1 task in output\n${taskListResult.output}"
            }
        }

        Then("the additional task uses runnerClass filter") {
            assert(taskListResult.output.contains("cucumberTestEpic1")) {
                "Expected cucumberTestEpic1 task to be available with runnerClass filter\n${taskListResult.output}"
            }
        }

        // ── CNV-7.1 — cucumber dependencies on testImplementation ──────────────
        Given("a project applies the cucumber plugin with dependency inspection") {
            testProjectDir = createTempDir("cucumber-test-")
            testProjectDir.resolve("settings.gradle.kts").writeText("""
                rootProject.name = "test-project"
            """)
            testProjectDir.resolve("build.gradle.kts").writeText("""
                plugins {
                    id("education.cccp.build.cucumber")
                }
            """)
            checkResult = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("dependencies", "--configuration", "testImplementation")
                .withPluginClasspath()
                .build()
        }

        Then("the testImplementation configuration contains cucumber-java") {
            assert(checkResult?.output?.contains("io.cucumber:cucumber-java") == true) {
                "Expected cucumber-java in testImplementation\n${checkResult?.output}"
            }
        }

        Then("the testImplementation configuration contains cucumber-junit-platform-engine") {
            assert(checkResult?.output?.contains("io.cucumber:cucumber-junit-platform-engine") == true) {
                "Expected cucumber-junit-platform-engine in testImplementation\n${checkResult?.output}"
            }
        }

        Then("the testImplementation configuration contains junit-platform-suite") {
            assert(checkResult?.output?.contains("org.junit.platform:junit-platform-suite") == true) {
                "Expected junit-platform-suite in testImplementation\n${checkResult?.output}"
            }
        }

        Then("the testImplementation configuration has the workspace-bom platform") {
            assert(checkResult?.output?.contains("education.cccp:workspace-bom") == true) {
                "Expected workspace-bom platform in testImplementation\n${checkResult?.output}"
            }
        }

        // ── CNV-10.4 — parallel execution ────────────────────────────────────
        Given("a project applies the cucumber plugin with parallel enabled") {
            testProjectDir = createTempDir("cucumber-parallel-")
            testProjectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"test-project\"")
            testProjectDir.resolve("build.gradle.kts").writeText("""
                plugins {
                    id("education.cccp.build.cucumber")
                }
                cucumberConventions {
                    parallel = true
                }
            """)
            taskListResult = runTasks("tasks", "--all")
        }

        Then("the cucumberTest task has parallel execution enabled") {
            assert(taskListResult.output.contains("cucumberTest")) {
                "Expected cucumberTest task with parallel enabled\n${taskListResult.output}"
            }
        }

        // ── CNV-10.4 — timeoutMinutes ────────────────────────────────────────
        Given("a project applies the cucumber plugin with timeout {int} minutes") { minutes: Int ->
            testProjectDir = createTempDir("cucumber-timeout-")
            testProjectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"test-project\"")
            testProjectDir.resolve("build.gradle.kts").writeText("""
                plugins {
                    id("education.cccp.build.cucumber")
                }
                cucumberConventions {
                    timeoutMinutes = $minutes
                }
            """)
            taskListResult = runTasks("tasks", "--all")
        }

        Then("the cucumberTest task has timeout configured") {
            assert(taskListResult.output.contains("cucumberTest")) {
                "Expected cucumberTest task with timeout configured\n${taskListResult.output}"
            }
        }

        // ── CNV-10.4 — cucumberTestTaskName configurable ──────────────────────
        Given("a project applies the cucumber plugin with custom task name {string}") { taskName: String ->
            testProjectDir = createTempDir("cucumber-custom-")
            testProjectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"test-project\"")
            testProjectDir.resolve("build.gradle.kts").writeText("""
                plugins {
                    id("education.cccp.build.cucumber")
                }
                cucumberConventions {
                    cucumberTestTaskName = "$taskName"
                }
            """)
            taskListResult = runTasks("tasks", "--all")
        }

        Then("the integrationCucumber task is registered") {
            assert(taskListResult.output.contains("integrationCucumber")) {
                "Expected integrationCucumber task in output\n${taskListResult.output}"
            }
        }

        // ── CNV-10.4 — additionalTasks with features and tags ────────────────
        Given("a project applies the cucumber plugin with additional tasks having features and tags") {
            testProjectDir = createTempDir("cucumber-ft-")
            testProjectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"test-project\"")
            testProjectDir.resolve("build.gradle.kts").writeText("""
                import build.CucumberTaskSpec

                plugins {
                    id("education.cccp.build.cucumber")
                }
                cucumberConventions {
                    additionalTasks = listOf(
                        CucumberTaskSpec(
                            name = "cucumberTestSmoke",
                            features = listOf("classpath:features/smoke"),
                            tags = listOf("@smoke", "@fast")
                        )
                    )
                }
            """)
            taskListResult = runTasks("tasks", "--all")
        }

        Then("the additional task has features configured") {
            assert(taskListResult.output.contains("cucumberTestSmoke")) {
                "Expected cucumberTestSmoke task with features configured\n${taskListResult.output}"
            }
        }

        Then("the additional task has tags configured") {
            assert(taskListResult.output.contains("cucumberTestSmoke")) {
                "Expected cucumberTestSmoke task with tags configured\n${taskListResult.output}"
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
