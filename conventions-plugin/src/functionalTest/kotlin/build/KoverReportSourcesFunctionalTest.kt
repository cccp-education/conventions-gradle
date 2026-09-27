package build

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class KoverReportSourcesFunctionalTest {

    @TempDir
    lateinit var testProjectDir: File

    private val buildFile: File get() = testProjectDir.resolve("build.gradle.kts")
    private val settingsFile: File get() = testProjectDir.resolve("settings.gradle.kts")

    private fun writeProject() {
        settingsFile.writeText(
            """
            rootProject.name = "kover-sources-test"
            pluginManagement {
                repositories {
                    mavenLocal()
                    gradlePluginPortal()
                    mavenCentral()
                }
            }
            """.trimIndent()
        )
        buildFile.writeText(
            """
            plugins {
                id("org.jetbrains.kotlin.jvm") version "2.4.10"
                id("org.jetbrains.kotlinx.kover") version "0.9.8"
                id("education.cccp.build.kover")
            }

            repositories {
                mavenCentral()
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
                    println("PROBE_INCLUDED=" + includedValues.sorted())
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
            """.trimIndent()
        )
    }

    private fun runProbe(): org.gradle.testkit.runner.BuildResult =
        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("probeKover")
            .withPluginClasspath()
            .build()

    @Test
    fun `kover measures main and functionalTest only`() {
        writeProject()

        val result = runProbe()

        assertEquals(TaskOutcome.SUCCESS, result.task(":probeKover")?.outcome)
        assertTrue(
            result.output.contains("PROBE_INCLUDED=[functionalTest, main]"),
            "Expected includedSourceSets to be exactly {main, functionalTest}\n${result.output}"
        )
    }

    @Test
    fun `kover attaches html and xml reports to check`() {
        writeProject()

        val result = runProbe()

        assertTrue(
            result.output.contains("PROBE_HTML_ONCHECK=true"),
            "Expected the HTML report to be attached to check\n${result.output}"
        )
        assertTrue(
            result.output.contains("PROBE_XML_ONCHECK=true"),
            "Expected the XML report to be attached to check\n${result.output}"
        )
    }
}
