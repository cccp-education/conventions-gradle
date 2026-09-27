package build

import io.cucumber.java8.En
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import java.io.File

class KoverThresholdCheckSteps : En {

    private lateinit var testProjectDir: File
    private var threshold: Int = 0
    private var customXmlPath: String? = null
    private var result: BuildResult? = null
    private var failed = false

    init {
        Given("a project applies the kover conventions plugin with a covered source") {
            testProjectDir = createTempDir("kover-threshold-check-")
            testProjectDir.resolve("settings.gradle.kts").writeText(
                """
                rootProject.name = "kover-threshold-check"
                pluginManagement {
                    repositories {
                        mavenLocal()
                        gradlePluginPortal()
                        mavenCentral()
                    }
                }
                """.trimIndent()
            )
            testProjectDir.resolve("src/main/kotlin").mkdirs()
            testProjectDir.resolve("src/test/kotlin").mkdirs()
            testProjectDir.resolve("src/main/kotlin/Covered.kt").writeText(
                """
                package sample

                object Covered {
                    fun answer(): Int = 42
                }
                """.trimIndent()
            )
            testProjectDir.resolve("src/test/kotlin/CoveredTest.kt").writeText(
                """
                package sample

                import org.junit.jupiter.api.Assertions.assertEquals
                import org.junit.jupiter.api.Test

                class CoveredTest {
                    @Test
                    fun answer() {
                        assertEquals(42, Covered.answer())
                    }
                }
                """.trimIndent()
            )
        }

        And("the kover threshold is {int}") { value: Int ->
            threshold = value
        }

        And("the kover xml report is configured at {string}") { path: String ->
            customXmlPath = path
        }

        When("the project runs the kover threshold check") {
            writeBuild()
            result = null
            failed = false
            try {
                result = GradleRunner.create()
                    .withProjectDir(testProjectDir)
                    .withArguments("koverThresholdCheck")
                    .withPluginClasspath()
                    .build()
            } catch (_: UnexpectedBuildFailure) {
                failed = true
            }
        }

        Then("the kover threshold check succeeds") {
            assert(!failed) { "Expected the threshold check to succeed" }
            assert(result?.task(":koverThresholdCheck")?.outcome != null) {
                "Expected the koverThresholdCheck task to run"
            }
        }

        Then("the kover threshold check fails") {
            assert(failed) { "Expected the threshold check to fail" }
        }

        Then("the kover threshold check reports instruction coverage") {
            assert(result?.output?.contains("Instruction coverage") == true) {
                "Expected the threshold check to report instruction coverage\n${result?.output}"
            }
        }

        Then("the custom xml report {string} exists") { path: String ->
            assert(testProjectDir.resolve("build/$path").exists()) {
                "Expected the custom xml report at build/$path"
            }
        }
    }

    private fun writeBuild() {
        val customBlock =
            if (customXmlPath != null) {
                """
                kover {
                    reports {
                        total {
                            xml {
                                xmlFile.set(layout.buildDirectory.file("$customXmlPath"))
                            }
                        }
                    }
                }
                """.trimIndent()
            } else {
                ""
            }
        testProjectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("org.jetbrains.kotlin.jvm") version "2.4.10"
                id("org.jetbrains.kotlinx.kover") version "0.9.8"
                id("education.cccp.build.kover")
            }

            repositories {
                mavenCentral()
            }

            dependencies {
                testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
                testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.14.3")
            }

            tasks.test {
                useJUnitPlatform()
            }

            koverConventions {
                enabled = true
                threshold = $threshold.0
            }

            $customBlock
            """.trimIndent()
        )
    }

    private fun createTempDir(prefix: String): File {
        val dir = File.createTempFile(prefix, "")
        dir.delete()
        dir.mkdir()
        dir.deleteOnExit()
        return dir
    }
}
