package build

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertTrue

/**
 * Functional coverage for the `library` convention (CNV-14.1): an N0 contract
 * module must get the JVM library build without applying `java-gradle-plugin`,
 * and must receive the centralised test dependencies and workspace guards.
 */
class LibraryConventionsFunctionalTest {

    @TempDir
    lateinit var testProjectDir: File

    private val buildFile: File get() = testProjectDir.resolve("build.gradle.kts")
    private val settingsFile: File get() = testProjectDir.resolve("settings.gradle.kts")

    private fun writeConsumer() {
        settingsFile.writeText("rootProject.name = \"library-consumer\"")
        buildFile.writeText(
            """
            plugins {
                id("education.cccp.build.library")
            }
            """.trimIndent()
        )
    }

    private fun run(vararg arguments: String): String =
        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments(*arguments)
            .withPluginClasspath()
            .build()
            .output

    @Test
    fun `plugin applies without error`() {
        writeConsumer()

        val output = run("tasks", "--all")

        assertTrue(output.contains("compileKotlin"), "Expected kotlin-jvm\n$output")
        assertTrue(output.contains("compileJava"), "Expected java-library\n$output")
    }

    @Test
    fun `plugin registers sources and javadoc jars`() {
        writeConsumer()

        val output = run("tasks", "--all")

        assertTrue(output.contains("sourcesJar"), "Expected sourcesJar\n$output")
        assertTrue(output.contains("javadocJar"), "Expected javadocJar\n$output")
    }

    @Test
    fun `plugin injects the workspace bom and junit test dependencies`() {
        writeConsumer()

        val output = run("dependencies", "--configuration", "testImplementation")

        val bomVersion = System.getProperty("conventions.workspaceBom.version")
            ?: error("conventions.workspaceBom.version system property is missing")
        assertTrue(
            output.contains("education.cccp:workspace-bom:$bomVersion"),
            "Expected the workspace-bom platform\n$output"
        )
        assertTrue(output.contains("org.junit.jupiter:junit-jupiter"), "Expected junit-jupiter\n$output")
        assertTrue(output.contains("org.jetbrains.kotlin:kotlin-test-junit5"), "Expected kotlin-test\n$output")
        assertTrue(output.contains("org.assertj:assertj-core"), "Expected assertj-core\n$output")
    }

    @Test
    fun `plugin registers the workspace guards`() {
        writeConsumer()

        val output = run("tasks", "--all")

        assertTrue(output.contains("verifyGradleWrapper"), "Expected verifyGradleWrapper\n$output")
        assertTrue(output.contains("auditConventionsAdoption"), "Expected auditConventionsAdoption\n$output")
    }

    @Test
    fun `plugin does not require a Gradle wrapper to keep check green`() {
        writeConsumer()

        val output = run("check")

        assertTrue(output.contains("BUILD SUCCESSFUL"), "Expected check to succeed\n$output")
    }
}
