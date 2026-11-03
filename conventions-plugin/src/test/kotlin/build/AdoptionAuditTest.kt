package build

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdoptionAuditTest {

    private fun kts(plugin: String, version: String) =
        """plugins { id("education.cccp.build.$plugin") version "$version" }"""

    @Test
    fun `pins parses every kotlin dsl pin of a file`() {
        val content = """
            plugins {
                id("education.cccp.build.gradle-plugin") version "0.0.11"
                id("education.cccp.build.lint") version "0.0.11"
            }
        """.trimIndent()

        val pins = AdoptionAudit.pins("a/build.gradle.kts", content)

        assertEquals(listOf("gradle-plugin", "lint"), pins.map { it.plugin })
        assertEquals(listOf("0.0.11", "0.0.11"), pins.map { it.version })
    }

    @Test
    fun `pins also reads the groovy dsl form`() {
        val pins = AdoptionAudit.pins("b/build.gradle", "id 'education.cccp.build.lint' version '0.0.9'")

        assertEquals(1, pins.size)
        assertEquals("lint", pins.single().plugin)
        assertEquals("0.0.9", pins.single().version)
    }

    @Test
    fun `audit is empty when every pin matches`() {
        val findings = AdoptionAudit.audit(
            mapOf("a/build.gradle.kts" to kts("gradle-plugin", "0.0.11")),
            "0.0.11",
        )

        assertTrue(findings.isEmpty())
    }

    @Test
    fun `audit flags an ahead pin`() {
        val findings = AdoptionAudit.audit(
            mapOf("a/build.gradle.kts" to kts("lint", "0.0.12")),
            "0.0.11",
        )

        assertTrue(findings.single() is AdoptionAudit.Finding.Ahead)
        assertTrue(AdoptionAudit.describe(findings.single()).contains("0.0.12"))
    }

    @Test
    fun `audit flags an obsolete pin`() {
        val findings = AdoptionAudit.audit(
            mapOf("a/build.gradle.kts" to kts("lint", "0.0.9")),
            "0.0.11",
        )

        assertTrue(findings.single() is AdoptionAudit.Finding.Obsolete)
    }

    @Test
    fun `audit flags snapshot and invalid pins`() {
        val findings = AdoptionAudit.audit(
            mapOf(
                "a/build.gradle.kts" to kts("lint", "0.0.11-SNAPSHOT"),
                "b/build.gradle.kts" to kts("kover", "latest.release"),
            ),
            "0.0.11",
        )

        assertTrue(findings.any { it is AdoptionAudit.Finding.Snapshot })
        assertTrue(findings.any { it is AdoptionAudit.Finding.Invalid })
    }

    @Test
    fun `audit ignores files without conventions pins`() {
        val findings = AdoptionAudit.audit(
            mapOf("a/build.gradle.kts" to "plugins { id(\"java-library\") }"),
            "0.0.11",
        )

        assertTrue(findings.isEmpty())
    }

    @Test
    fun `compare is numeric on major minor patch`() {
        assertEquals(0, AdoptionAudit.compare("0.0.11", "0.0.11"))
        assertTrue(AdoptionAudit.compare("0.0.9", "0.0.11")!! < 0)
        assertTrue(AdoptionAudit.compare("0.1.0", "0.0.99")!! > 0)
        assertEquals(null, AdoptionAudit.compare("0.0", "0.0.11"))
    }
}
