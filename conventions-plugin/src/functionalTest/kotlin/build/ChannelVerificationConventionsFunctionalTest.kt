package build

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertTrue

/**
 * GBL-014 / S-052 — `verifyChannel` is registered by **every** convention, so a
 * build that only adopts `.cucumber` or `.library` (no `.lint`, no
 * `.publishing`) is no longer a gate hole.
 *
 * The transition rule is unchanged: the task exists on demand, and `check`
 * depends on it **only** when `-Pchannel` is explicit.
 */
class ChannelVerificationConventionsFunctionalTest {

    @TempDir
    lateinit var testProjectDir: File

    private val buildFile: File get() = testProjectDir.resolve("build.gradle.kts")
    private val settingsFile: File get() = testProjectDir.resolve("settings.gradle.kts")

    /** A `settings.gradle.kts` already projected onto the dual channel. */
    private fun writeMigratedSettings() {
        settingsFile.writeText(
            """
            pluginManagement {
                repositories {
                    mavenCentral()
                    gradlePluginPortal()
                }
            }
            val publicationChannel = providers.gradleProperty("channel").getOrElse("jitpack")
            if (publicationChannel == "jitpack") {
                pluginManagement.repositories.maven("https://jitpack.io")
                pluginManagement.resolutionStrategy.eachPlugin {
                    if (requested.id.id.startsWith("education.cccp.build.")) {
                        useModule("com.github.cccp-education.conventions-gradle:conventions-plugin:0.0.20")
                    }
                }
            }
            rootProject.name = "conventions-channel"
            if (publicationChannel == "jitpack") {
                dependencyResolutionManagement.repositories.maven("https://jitpack.io")
            }
            """.trimIndent(),
        )
    }

    private fun writeConsumer(pluginId: String, extraPlugins: String = "") {
        buildFile.writeText(
            """
            plugins {
                id("$pluginId")
                $extraPlugins
            }
            """.trimIndent(),
        )
    }

    private fun run(vararg arguments: String): String =
        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments(*arguments)
            .withPluginClasspath()
            .build()
            .output

    // ── `.cucumber` — the container-provision gate hole ────────────────────

    @Test
    fun `cucumber only build registers verifyChannel`() {
        writeMigratedSettings()
        writeConsumer("education.cccp.build.cucumber")

        val output = run("tasks", "--all")

        assertTrue(output.contains("verifyChannel"), output)
    }

    @Test
    fun `cucumber only build accepts the migrated settings on the jitpack channel`() {
        writeMigratedSettings()
        writeConsumer("education.cccp.build.cucumber")

        val output = run("verifyChannel", "-Pchannel=jitpack")

        assertTrue(output.contains("CHANNEL OK"), output)
    }

    @Test
    fun `cucumber only build accepts the migrated settings on the central channel`() {
        writeMigratedSettings()
        writeConsumer("education.cccp.build.cucumber")

        val output = run("verifyChannel", "-Pchannel=central")

        assertTrue(output.contains("CHANNEL OK"), output)
    }

    @Test
    fun `cucumber only build refuses an unguarded jitpack repository on the central channel`() {
        writeMigratedSettings()
        settingsFile.appendText(
            "\ndependencyResolutionManagement.repositories.maven(\"https://jitpack.io\")\n",
        )
        writeConsumer("education.cccp.build.cucumber")

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("verifyChannel", "-Pchannel=central")
            .withPluginClasspath()
            .buildAndFail()

        assertTrue(result.output.contains("FORBIDDEN_JITPACK_REPOSITORY"), result.output)
    }

    @Test
    fun `cucumber only build does not wire verifyChannel into check without an explicit channel`() {
        writeMigratedSettings()
        writeConsumer("education.cccp.build.cucumber")

        val output = run("check", "--dry-run")

        assertTrue(!output.contains("verifyChannel"), output)
    }

    @Test
    fun `cucumber only build wires verifyChannel into check when the channel is explicit`() {
        writeMigratedSettings()
        writeConsumer("education.cccp.build.cucumber")

        val output = run("check", "--dry-run", "-Pchannel=jitpack")

        assertTrue(output.contains("verifyChannel"), output)
    }

    // ── `.library` — the N0 contract module profile ─────────────────────────

    @Test
    fun `library only build registers verifyChannel and accepts the central channel`() {
        writeMigratedSettings()
        writeConsumer("education.cccp.build.library")

        val tasksOutput = run("tasks", "--all")
        assertTrue(tasksOutput.contains("verifyChannel"), tasksOutput)

        val gateOutput = run("verifyChannel", "-Pchannel=central")
        assertTrue(gateOutput.contains("CHANNEL OK"), gateOutput)
    }
}
