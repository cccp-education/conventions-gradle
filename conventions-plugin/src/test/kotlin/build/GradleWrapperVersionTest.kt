package build

import java.io.ByteArrayInputStream
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GradleWrapperVersionTest {

    private val bin = """
        distributionBase=GRADLE_USER_HOME
        distributionUrl=https\://services.gradle.org/distributions/gradle-9.8.2-bin.zip
        validateDistributionUrl=true
    """.trimIndent()

    private val all = "distributionUrl=https\\://services.gradle.org/distributions/gradle-9.8.2-all.zip"

    @Test
    fun `version reads the generated resource`() {
        assertEquals("9.9.9", GradleWrapperVersion.version(loaderWith("9.9.9")))
    }

    @Test
    fun `version falls back when the resource is absent`() {
        assertEquals(GradleWrapperVersion.FALLBACK_VERSION, GradleWrapperVersion.version(object : ClassLoader(null) {}))
    }

    @Test
    fun `version falls back when the version key is blank`() {
        assertEquals(GradleWrapperVersion.FALLBACK_VERSION, GradleWrapperVersion.version(loaderWith("  ")))
    }

    @Test
    fun `distribution version parses bin and all distributions`() {
        assertEquals("9.8.2", GradleWrapperVersion.distributionVersion(bin))
        assertEquals("9.8.2", GradleWrapperVersion.distributionVersion(all))
    }

    @Test
    fun `distribution version is null without a distribution url`() {
        assertNull(GradleWrapperVersion.distributionVersion("distributionBase=GRADLE_USER_HOME"))
    }

    @Test
    fun `isAligned compares the wrapper against the expected version`() {
        assertTrue(GradleWrapperVersion.isAligned(bin, "9.8.2"))
        assertFalse(GradleWrapperVersion.isAligned(bin, "9.8.1"))
    }

    @Test
    fun `drift is null when aligned and names both versions when not`() {
        assertNull(GradleWrapperVersion.drift(bin, "9.8.2"))

        val drift = GradleWrapperVersion.drift(bin, "9.8.1")
        assertTrue(drift!!.contains("9.8.2"))
        assertTrue(drift.contains("9.8.1"))
    }

    @Test
    fun `drift reports a missing distribution url`() {
        val drift = GradleWrapperVersion.drift("distributionBase=GRADLE_USER_HOME", "9.8.2")
        assertTrue(drift!!.contains("no `distributionUrl`"))
    }

    private fun loaderWith(version: String): ClassLoader {
        val bytes = Properties()
            .also { it.setProperty("version", version) }
            .let { properties ->
                java.io.ByteArrayOutputStream().also { properties.store(it, null) }.toByteArray()
            }
        return object : ClassLoader() {
            override fun getResourceAsStream(name: String) =
                if (name == GradleWrapperVersion.RESOURCE_PATH) ByteArrayInputStream(bytes) else null
        }
    }
}
