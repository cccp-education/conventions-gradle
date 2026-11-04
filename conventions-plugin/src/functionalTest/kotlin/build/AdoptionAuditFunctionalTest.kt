package build

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertTrue

/**
 * Functional tests for the cross-borough adoption audit. The expected
 * conventions version is **injected by Gradle**
 * (`conventions.version` system property, from the build's `version`), so the
 * fixture can never drift from the plugin under test.
 */
class AdoptionAuditFunctionalTest {

    @TempDir
    lateinit var testProjectDir: File

    private val buildFile: File get() = testProjectDir.resolve("build.gradle.kts")
    private val settingsFile: File get() = testProjectDir.resolve("settings.gradle.kts")
    private val workspace: File get() = testProjectDir.resolve("ws")

    private val conventionsVersion: String
        get() = System.getProperty("conventions.version")
            ?: error("conventions.version system property is missing")

    private fun writeProject() {
        settingsFile.writeText("rootProject.name = \"test-project\"")
        buildFile.writeText("""
            plugins {
                id("education.cccp.build.gradle-plugin")
            }
        """)
    }

    private fun writeWorkspacePin(version: String) {
        val target = workspace.resolve("some-plugin")
        target.mkdirs()
        target.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("education.cccp.build.lint") version "$version"
            }
            """.trimIndent()
        )
    }

    private fun runner(vararg arguments: String): GradleRunner =
        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments(*arguments)
            .withPluginClasspath()

    @Test
    fun `audit reports an aligned workspace`() {
        writeProject()
        writeWorkspacePin(conventionsVersion)

        val result = runner("auditConventionsAdoption", "-PworkspaceRoot=${workspace.absolutePath}").build()

        assertTrue(result.output.contains("CONVENTIONS ADOPTION OK"), result.output)
    }

    @Test
    fun `audit reports an obsolete pin without failing by default`() {
        writeProject()
        writeWorkspacePin("0.0.7")

        val result = runner("auditConventionsAdoption", "-PworkspaceRoot=${workspace.absolutePath}").build()

        assertTrue(result.output.contains("CONVENTIONS ADOPTION:"), result.output)
        assertTrue(result.output.contains("0.0.7"), result.output)
    }

    @Test
    fun `audit fails on a finding when strict`() {
        writeProject()
        writeWorkspacePin("0.0.99")

        val result = runner(
            "auditConventionsAdoption",
            "-PworkspaceRoot=${workspace.absolutePath}",
            "-Pstrict=true",
        ).buildAndFail()

        assertTrue(result.output.contains("0.0.99"), result.output)
        assertTrue(result.output.contains("finding(s)"), result.output)
    }
}
