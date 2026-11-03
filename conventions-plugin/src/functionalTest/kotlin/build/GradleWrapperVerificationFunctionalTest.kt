package build

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertTrue

/**
 * Functional tests stay independent from the plugin's own classes (the
 * functionalTest classpath does not include `main`): they assert the observed
 * Gradle behaviour through TestKit, using the published contract values.
 */
class GradleWrapperVerificationFunctionalTest {

    @TempDir
    lateinit var testProjectDir: File

    private val buildFile: File get() = testProjectDir.resolve("build.gradle.kts")
    private val settingsFile: File get() = testProjectDir.resolve("settings.gradle.kts")
    private val wrapperFile: File get() = testProjectDir.resolve("gradle/wrapper/gradle-wrapper.properties")

    private fun writeProject(wrapperVersion: String) {
        settingsFile.writeText("rootProject.name = \"test-project\"")
        buildFile.writeText("""
            plugins {
                id("education.cccp.build.gradle-plugin")
            }
        """)
        wrapperFile.parentFile.mkdirs()
        wrapperFile.writeText(
            """
            distributionBase=GRADLE_USER_HOME
            distributionPath=wrapper/dists
            distributionUrl=https\://services.gradle.org/distributions/gradle-$wrapperVersion-bin.zip
            zipStoreBase=GRADLE_USER_HOME
            zipStorePath=wrapper/dists
            """.trimIndent()
        )
    }

    private fun runner(vararg arguments: String): GradleRunner =
        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments(*arguments)
            .withPluginClasspath()

    @Test
    fun `verifyGradleWrapper succeeds when the wrapper matches the workspace version`() {
        writeProject("9.8.2")

        val result = runner("verifyGradleWrapper").build()

        assertTrue(result.output.contains(":verifyGradleWrapper"), result.output)
    }

    @Test
    fun `verifyGradleWrapper fails when the wrapper drifts`() {
        writeProject("9.7.9")

        val result = runner("verifyGradleWrapper").buildAndFail()

        assertTrue(result.output.contains("9.7.9"), result.output)
        assertTrue(result.output.contains("9.8.2"), result.output)
    }

    @Test
    fun `check depends on verifyGradleWrapper`() {
        writeProject("9.8.2")

        val result = runner("check", "--dry-run").build()

        assertTrue(result.output.contains(":verifyGradleWrapper"), result.output)
    }
}
