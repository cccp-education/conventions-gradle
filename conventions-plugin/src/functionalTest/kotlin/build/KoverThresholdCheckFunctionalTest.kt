package build

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class KoverThresholdCheckFunctionalTest {

    @TempDir
    lateinit var testProjectDir: File

    private val buildFile: File get() = testProjectDir.resolve("build.gradle.kts")
    private val settingsFile: File get() = testProjectDir.resolve("settings.gradle.kts")

    private fun writeSettings() {
        settingsFile.writeText(
            """
            rootProject.name = "kover-threshold-test"
            pluginManagement {
                repositories {
                    mavenLocal()
                    gradlePluginPortal()
                    mavenCentral()
                }
            }
            """.trimIndent()
        )
    }

    private fun writeProject(koverBlock: String = "") {
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

            dependencies {
                testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
                testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.14.3")
            }

            tasks.test {
                useJUnitPlatform()
            }

            koverConventions {
                enabled = true
                threshold = 0.0
            }

            $koverBlock
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

    private fun runThresholdCheck(): org.gradle.testkit.runner.BuildResult =
        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("koverThresholdCheck")
            .withPluginClasspath()
            .build()

    @Test
    fun `threshold check reads the kover default xml report location`() {
        writeSettings()
        writeProject()

        val result = runThresholdCheck()

        org.junit.jupiter.api.Assertions.assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":koverThresholdCheck")?.outcome
        )
        org.junit.jupiter.api.Assertions.assertTrue(
            result.output.contains("Instruction coverage"),
            "Expected the threshold check to report instruction coverage\n${result.output}"
        )
    }

    @Test
    fun `threshold check honours a custom xml report location`() {
        writeSettings()
        writeProject(
            koverBlock =
                """
                kover {
                    reports {
                        total {
                            xml {
                                xmlFile.set(layout.buildDirectory.file("reports/kover/custom/coverage.xml"))
                            }
                        }
                    }
                }
                """.trimIndent()
        )

        val result = runThresholdCheck()

        org.junit.jupiter.api.Assertions.assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":koverThresholdCheck")?.outcome
        )
        org.junit.jupiter.api.Assertions.assertTrue(
            testProjectDir.resolve("build/reports/kover/custom/coverage.xml").exists(),
            "Expected the custom xml report to be generated"
        )
    }
}
