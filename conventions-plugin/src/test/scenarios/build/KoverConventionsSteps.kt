package build

import io.cucumber.java8.En
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File

class KoverConventionsSteps : En {

    private lateinit var testProjectDir: File
    private lateinit var taskListResult: BuildResult
    private lateinit var probeResult: BuildResult

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

                tasks.register("probeKover") {
                    doLast {
                        fun invoke(target: Any, name: String): Any =
                            target.javaClass.methods.first { it.name == name }.invoke(target)!!

                        val kover = project.extensions.getByName("kover")
                        val currentProject = invoke(kover, "getCurrentProject")
                        val sources = invoke(currentProject, "getSources")
                        val included = invoke(sources, "getIncludedSourceSets")
                        @Suppress("UNCHECKED_CAST")
                        val includedValues = invoke(included, "get") as Set<String>
                        println("PROBE_INCLUDED=" + includedValues.sorted().joinToString(","))
                        val reports = invoke(kover, "getReports")
                        val total = invoke(reports, "getTotal")
                        println(
                            "PROBE_HTML_ONCHECK=" +
                                invoke(invoke(invoke(total, "getHtml"), "getOnCheck"), "get")
                        )
                        println(
                            "PROBE_XML_ONCHECK=" +
                                invoke(invoke(invoke(total, "getXml"), "getOnCheck"), "get")
                        )
                    }
                }
            """)
            taskListResult = runTasks("tasks", "--all")
        }

        Then("the kover plugin is applied") {
            assert(taskListResult.output.contains("koverXmlReport")) {
                "Expected koverXmlReport task to be present when enabled\n${taskListResult.output}"
            }
        }

        When("the project runs the kover sources probe") {
            probeResult = runTasks("probeKover")
        }

        Then("the measured source sets are exactly {string} and {string}") { first: String, second: String ->
            val expected = listOf(first, second).sorted().joinToString(",")
            assert(probeResult.output.contains("PROBE_INCLUDED=$expected")) {
                "Expected measured source sets to be exactly $expected\n${probeResult.output}"
            }
        }

        Then("the HTML and XML reports are attached to check") {
            assert(probeResult.output.contains("PROBE_HTML_ONCHECK=true")) {
                "Expected the HTML report to be attached to check\n${probeResult.output}"
            }
            assert(probeResult.output.contains("PROBE_XML_ONCHECK=true")) {
                "Expected the XML report to be attached to check\n${probeResult.output}"
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
