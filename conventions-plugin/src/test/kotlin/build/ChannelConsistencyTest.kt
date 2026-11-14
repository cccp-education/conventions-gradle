package build

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ChannelConsistencyTest {

    private val jitpackSettings = """
        pluginManagement.repositories.maven("https://jitpack.io")
        pluginManagement.resolutionStrategy.eachPlugin {
            if (requested.id.id.startsWith("education.cccp.build.")) {
                useModule("com.github.cccp-education.conventions-gradle:conventions-plugin:0.0.18")
            }
        }
        dependencyResolutionManagement.repositories.maven("https://jitpack.io")
    """.trimIndent()

    private val centralSettings = """
        pluginManagement {
            repositories {
                mavenCentral()
                gradlePluginPortal()
            }
        }
        dependencyResolutionManagement.repositories.mavenCentral()
    """.trimIndent()

    @Test
    fun `jitpack settings with the repository and the shim are consistent`() {
        assertEquals(
            emptyList<ChannelConsistency.Finding>(),
            ChannelConsistency.auditSettings(jitpackSettings, PublicationChannel.JITPACK),
        )
    }

    @Test
    fun `jitpack settings without the repository are refused`() {
        val settings = jitpackSettings
            .replace("""pluginManagement.repositories.maven("https://jitpack.io")""", "")
            .replace("""dependencyResolutionManagement.repositories.maven("https://jitpack.io")""", "")
        val findings = ChannelConsistency.auditSettings(settings, PublicationChannel.JITPACK)
        assertEquals(
            listOf(ChannelConsistency.Finding.Rule.MISSING_JITPACK_REPOSITORY),
            findings.map { it.rule },
        )
    }

    @Test
    fun `jitpack settings without the shim are refused`() {
        val settings = jitpackSettings.replace("useModule(", "// useModule(")
        val findings = ChannelConsistency.auditSettings(settings, PublicationChannel.JITPACK)
        assertEquals(
            listOf(ChannelConsistency.Finding.Rule.MISSING_SHIM),
            findings.map { it.rule },
        )
    }

    @Test
    fun `central settings without any jitpack reference are consistent`() {
        assertEquals(
            emptyList<ChannelConsistency.Finding>(),
            ChannelConsistency.auditSettings(centralSettings, PublicationChannel.CENTRAL),
        )
    }

    @Test
    fun `central settings with the jitpack repository are refused`() {
        val findings = ChannelConsistency.auditSettings(jitpackSettings, PublicationChannel.CENTRAL)
        assertEquals(
            listOf(
                ChannelConsistency.Finding.Rule.FORBIDDEN_JITPACK_REPOSITORY,
                ChannelConsistency.Finding.Rule.FORBIDDEN_SHIM,
            ),
            findings.map { it.rule },
        )
    }

    @Test
    fun `comment lines are ignored in central mode`() {
        val settings = centralSettings + "\n" + """
            // CNV-16 — historical note: maven("https://jitpack.io") and useModule(...)
        """.trimIndent()
        assertEquals(
            emptyList<ChannelConsistency.Finding>(),
            ChannelConsistency.auditSettings(settings, PublicationChannel.CENTRAL),
        )
    }
}
