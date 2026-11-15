package build

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertTrue

/**
 * GBL-014-4 — `verifyChannel` must also exist in builds that apply the lint
 * convention without `.publishing` (`academy`, `dashboard`, `jhipster` are
 * plugin-publish builds with lint only). The task is useful on demand and is
 * wired to `check` **only** when `-Pchannel` is explicit.
 */
class ChannelVerificationLintFunctionalTest {

    @TempDir
    lateinit var testProjectDir: File

    private val buildFile: File get() = testProjectDir.resolve("build.gradle.kts")
    private val settingsFile: File get() = testProjectDir.resolve("settings.gradle.kts")

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
                        useModule("com.github.cccp-education.conventions-gradle:conventions-plugin:0.0.18")
                    }
                }
            }
            rootProject.name = "lint-channel"
            if (publicationChannel == "jitpack") {
                dependencyResolutionManagement.repositories.maven("https://jitpack.io")
            }
            """.trimIndent(),
        )
    }

    private fun writeLintOnlyBuild() {
        buildFile.writeText(
            """
            plugins {
                id("education.cccp.build.lint")
            }
            """.trimIndent(),
        )
    }

    @Test
    fun `lint only build registers verifyChannel`() {
        writeMigratedSettings()
        writeLintOnlyBuild()

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("tasks", "--all")
            .withPluginClasspath()
            .build()

        assertTrue(result.output.contains("verifyChannel"), result.output)
    }

    @Test
    fun `lint only build accepts the migrated settings on the central channel`() {
        writeMigratedSettings()
        writeLintOnlyBuild()

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("verifyChannel", "-Pchannel=central")
            .withPluginClasspath()
            .build()

        assertTrue(result.output.contains("CHANNEL OK"), result.output)
    }

    @Test
    fun `lint only build refuses an unconditional jitpack repository on the central channel`() {
        writeMigratedSettings()
        settingsFile.appendText(
            "\ndependencyResolutionManagement.repositories.maven(\"https://jitpack.io\")\n",
        )
        writeLintOnlyBuild()

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("verifyChannel", "-Pchannel=central")
            .withPluginClasspath()
            .buildAndFail()

        assertTrue(result.output.contains("FORBIDDEN_JITPACK_REPOSITORY"), result.output)
    }

    // GBL-014-4 — the transition rule is preserved: no explicit `-Pchannel`,
    // no gate on `check` (the ~20 not-yet-migrated consumers keep their CI).
    @Test
    fun `check does not depend on verifyChannel without an explicit channel`() {
        writeMigratedSettings()
        buildFile.writeText(
            """
            plugins {
                id("java")
                id("education.cccp.build.lint")
            }
            """.trimIndent(),
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("check", "--dry-run")
            .withPluginClasspath()
            .build()

        assertTrue(!result.output.contains("verifyChannel"), result.output)
    }

    @Test
    fun `check depends on verifyChannel when the channel is explicit`() {
        writeMigratedSettings()
        buildFile.writeText(
            """
            plugins {
                id("java")
                id("education.cccp.build.lint")
            }
            """.trimIndent(),
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("check", "--dry-run", "-Pchannel=jitpack")
            .withPluginClasspath()
            .build()

        assertTrue(result.output.contains("verifyChannel"), result.output)
    }
}
