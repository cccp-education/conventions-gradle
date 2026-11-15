package build

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * GBL-014-4 — the semantic audit: a migrated consumer keeps **both**
 * projections in the same `settings.gradle.kts`, inside a channel-guarded
 * block. `central` must ignore those branches and still refuse any JitPack
 * reference that survives outside them.
 */
class ChannelConsistencySemanticTest {

    /** The shape delivered by GBL-014-5 (see `capsule-gradle/settings.gradle.kts`). */
    private val migratedSettings =
        """
        pluginManagement {
            repositories {
                mavenLocal()
                mavenCentral()
                gradlePluginPortal()
            }
        }
        // GBL-014 — the publication channel is a build property, never a branch.
        val publicationChannel = providers.gradleProperty("channel").getOrElse("jitpack")

        if (publicationChannel == "jitpack") {
            // CNV-16 — JitPack mirror for the education.cccp.build.* conventions
            pluginManagement.repositories.maven("https://jitpack.io")
            pluginManagement.resolutionStrategy.eachPlugin {
                if (requested.id.id.startsWith("education.cccp.build.")) {
                    useModule("com.github.cccp-education.conventions-gradle:conventions-plugin:0.0.18")
                }
            }
        }

        rootProject.name = "capsule-gradle"

        if (publicationChannel == "jitpack") {
            dependencyResolutionManagement.repositories.maven("https://jitpack.io")
        }
        """.trimIndent()

    private val jitpack = PublicationChannel.JITPACK
    private val central = PublicationChannel.CENTRAL

    private fun centralFindings(settings: String) =
        ChannelConsistency.auditSettings(settings, central).map { it.rule }

    private fun jitpackFindings(settings: String) =
        ChannelConsistency.auditSettings(settings, jitpack).map { it.rule }

    @Test
    fun `central accepts a settings whose jitpack references are channel-guarded`() {
        assertEquals(emptyList<ChannelConsistency.Finding.Rule>(), centralFindings(migratedSettings))
    }

    @Test
    fun `jitpack accepts the same channel-guarded settings`() {
        assertEquals(emptyList<ChannelConsistency.Finding.Rule>(), jitpackFindings(migratedSettings))
    }

    @Test
    fun `central refuses a jitpack repository outside the guard`() {
        val settings = migratedSettings.replace(
            "rootProject.name = \"capsule-gradle\"",
            """rootProject.name = "capsule-gradle"
            dependencyResolutionManagement.repositories.maven("https://jitpack.io")""",
        )
        assertEquals(
            listOf(ChannelConsistency.Finding.Rule.FORBIDDEN_JITPACK_REPOSITORY),
            centralFindings(settings),
        )
    }

    @Test
    fun `central refuses a shim call outside the guard`() {
        val settings = migratedSettings.replace(
            "rootProject.name = \"capsule-gradle\"",
            """rootProject.name = "capsule-gradle"
            pluginManagement.resolutionStrategy.eachPlugin {
                useModule("com.github.cccp-education.conventions-gradle:conventions-plugin:0.0.18")
            }""",
        )
        assertEquals(
            listOf(ChannelConsistency.Finding.Rule.FORBIDDEN_SHIM),
            centralFindings(settings),
        )
    }

    @Test
    fun `central refuses the jitpack projection placed in the else branch of a central guard`() {
        val settings =
            """
            val publicationChannel = providers.gradleProperty("channel").getOrElse("jitpack")
            if (publicationChannel == "central") {
                pluginManagement.repositories.mavenCentral()
            } else {
                pluginManagement.repositories.maven("https://jitpack.io")
            }
            rootProject.name = "inverted-guard"
            """.trimIndent()
        assertEquals(
            listOf(ChannelConsistency.Finding.Rule.FORBIDDEN_JITPACK_REPOSITORY),
            centralFindings(settings),
        )
    }

    @Test
    fun `central ignores the jitpack shim of the else branch of a central guard`() {
        val settings =
            """
            val publicationChannel = providers.gradleProperty("channel").getOrElse("jitpack")
            if (publicationChannel == "central") {
                pluginManagement.repositories.mavenCentral()
            } else {
                pluginManagement.resolutionStrategy.eachPlugin {
                    useModule("com.github.cccp-education.conventions-gradle:conventions-plugin:0.0.18")
                }
            }
            rootProject.name = "inverted-guard-shim"
            """.trimIndent()
        assertEquals(emptyList<ChannelConsistency.Finding.Rule>(), centralFindings(settings))
    }

    @Test
    fun `braces and quotes inside literals do not unbalance the guard scan`() {
        val settings =
            """
            val publicationChannel = providers.gradleProperty("channel").getOrElse("jitpack")
            if (publicationChannel == "jitpack") {
                println("}")
                pluginManagement.repositories.maven("https://jitpack.io")
            }
            rootProject.name = "literal-braces"
            """.trimIndent()
        // The guarded block is fully ignored in central: the `}` inside the
        // string must not close it early and leak the repository into the audit.
        assertEquals(emptyList<ChannelConsistency.Finding.Rule>(), centralFindings(settings))
    }

    @Test
    fun `a jitpack reference after a literal brace guard is still refused`() {
        val settings =
            """
            val publicationChannel = providers.gradleProperty("channel").getOrElse("jitpack")
            if (publicationChannel == "jitpack") {
                println("{")
                pluginManagement.repositories.maven("https://jitpack.io")
            }
            dependencyResolutionManagement.repositories.maven("https://jitpack.io")
            rootProject.name = "leak-after-guard"
            """.trimIndent()
        assertEquals(
            listOf(ChannelConsistency.Finding.Rule.FORBIDDEN_JITPACK_REPOSITORY),
            centralFindings(settings),
        )
    }

    @Test
    fun `a commented-out guard does not hide an unconditional violation`() {
        val settings =
            """
            // if (publicationChannel == "jitpack") {
            //     pluginManagement.repositories.maven("https://jitpack.io")
            // }
            pluginManagement.repositories.maven("https://jitpack.io")
            rootProject.name = "commented-guard"
            """.trimIndent()
        assertEquals(
            listOf(ChannelConsistency.Finding.Rule.FORBIDDEN_JITPACK_REPOSITORY),
            centralFindings(settings),
        )
    }

    @Test
    fun `nested guards are entirely ignored in central mode`() {
        val settings =
            """
            val publicationChannel = providers.gradleProperty("channel").getOrElse("jitpack")
            if (publicationChannel == "jitpack") {
                if (requested.id.id.startsWith("education.cccp.build.")) {
                    useModule("com.github.cccp-education.conventions-gradle:conventions-plugin:0.0.18")
                }
                pluginManagement.repositories.maven("https://jitpack.io")
            }
            rootProject.name = "nested-guard"
            """.trimIndent()
        assertEquals(emptyList<ChannelConsistency.Finding.Rule>(), centralFindings(settings))
    }

    @Test
    fun `a block comment inside the guard is not a violation`() {
        val settings =
            """
            val publicationChannel = providers.gradleProperty("channel").getOrElse("jitpack")
            if (publicationChannel == "jitpack") {
                /* maven("https://jitpack.io") was the historical fallback */
                pluginManagement.repositories.maven("https://jitpack.io")
                pluginManagement.resolutionStrategy.eachPlugin {
                    useModule("com.github.cccp-education.conventions-gradle:conventions-plugin:0.0.18")
                }
            }
            rootProject.name = "block-comment"
            """.trimIndent()
        assertEquals(emptyList<ChannelConsistency.Finding.Rule>(), centralFindings(settings))
    }

    @Test
    fun `jitpack still requires the shim when it only survives in a comment`() {
        val settings = migratedSettings.replace("useModule(", "// useModule(")
        assertEquals(
            listOf(ChannelConsistency.Finding.Rule.MISSING_SHIM),
            jitpackFindings(settings),
        )
    }

    @Test
    fun `jitpack accepts an unconditional repository and shim`() {
        val settings =
            """
            pluginManagement.repositories.maven("https://jitpack.io")
            pluginManagement.resolutionStrategy.eachPlugin {
                useModule("com.github.cccp-education.conventions-gradle:conventions-plugin:0.0.18")
            }
            dependencyResolutionManagement.repositories.maven("https://jitpack.io")
            """.trimIndent()
        assertEquals(emptyList<ChannelConsistency.Finding.Rule>(), jitpackFindings(settings))
    }

    @Test
    fun `findings carry an actionable message`() {
        val findings = ChannelConsistency.auditSettings(migratedSettings, central)
        assertTrue(findings.isEmpty(), "the migrated form must be consistent in central")

        val leaked = ChannelConsistency.auditSettings(
            migratedSettings + "\ndependencyResolutionManagement.repositories.maven(\"https://jitpack.io\")",
            central,
        )
        assertEquals(1, leaked.size)
        assertTrue(ChannelConsistency.describe(leaked.single()).contains("channel-guarded block"))
    }
}
