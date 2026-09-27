package build

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Functional coverage for the workspace-bom fallback (S-019 P2-A): a consumer
 * without a local `libs` catalog must still receive the **current** BOM version,
 * generated from this plugin's own version catalog — never the obsolete
 * hardcoded `0.0.13` that had drifted 45 versions behind.
 */
class WorkspaceBomFunctionalTest {

    @TempDir
    lateinit var testProjectDir: File

    private val buildFile: File get() = testProjectDir.resolve("build.gradle.kts")
    private val settingsFile: File get() = testProjectDir.resolve("settings.gradle.kts")

    private fun writeConsumerWithoutCatalog() {
        settingsFile.writeText("rootProject.name = \"bom-consumer\"")
        buildFile.writeText(
            """
            plugins {
                id("education.cccp.build.gradle-plugin")
            }
            """.trimIndent()
        )
    }

    private fun dependencies(): String =
        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("dependencies", "--configuration", "testImplementation")
            .withPluginClasspath()
            .build()
            .output

    @Test
    fun `fallback injects the plugin's own workspace-bom version`() {
        writeConsumerWithoutCatalog()

        val version = System.getProperty("conventions.workspaceBom.version")
            ?: error("conventions.workspaceBom.version system property is missing")
        val expected = "education.cccp:workspace-bom:$version"
        val output = dependencies()

        assertTrue(
            output.contains(expected),
            "Expected the generated workspace-bom coordinates '$expected' but got\n$output"
        )
    }

    @Test
    fun `fallback never injects the obsolete drifted version`() {
        writeConsumerWithoutCatalog()

        val output = dependencies()

        assertTrue(
            !output.contains("education.cccp:workspace-bom:0.0.13"),
            "The obsolete 0.0.13 BOM drifted back into the fallback\n$output"
        )
    }
}
