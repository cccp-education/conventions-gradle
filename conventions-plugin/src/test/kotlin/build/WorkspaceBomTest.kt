package build

import java.io.ByteArrayInputStream
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals

class WorkspaceBomTest {

    @Test
    fun `coordinates compose group artifact and version`() {
        assertEquals(
            "education.cccp:workspace-bom:1.2.3",
            WorkspaceBom.coordinates("1.2.3")
        )
    }

    @Test
    fun `version reads the generated resource`() {
        val loader = loaderWith(mapOf("version" to "9.9.9"))

        assertEquals("9.9.9", WorkspaceBom.version(loader))
    }

    @Test
    fun `version falls back when the resource is absent`() {
        val loader = object : ClassLoader() {}

        assertEquals(WorkspaceBom.FALLBACK_VERSION, WorkspaceBom.version(loader))
    }

    @Test
    fun `version falls back when the version key is blank`() {
        val loader = loaderWith(mapOf("version" to "   "))

        assertEquals(WorkspaceBom.FALLBACK_VERSION, WorkspaceBom.version(loader))
    }

    @Test
    fun `coordinates from loader uses the resource version`() {
        val loader = loaderWith(mapOf("version" to "4.5.6"))

        assertEquals("education.cccp:workspace-bom:4.5.6", WorkspaceBom.coordinates(loader))
    }

    @Test
    fun `fallback version matches the published catalog version`() {
        // The fallback is a safety net, not the source of truth; it must still
        // be the current published BOM so a missing resource degrades cleanly.
        assertEquals("0.0.58", WorkspaceBom.FALLBACK_VERSION)
    }

    @Test
    fun `generated resource matches the fallback and never the obsolete drift`() {
        // Anti-drift guard: the resource is generated from
        // gradle/libs.versions.toml; the hardcoded fallback must track it.
        // This test fails the moment one is bumped without the other.
        val generated = WorkspaceBom.version(WorkspaceBom::class.java.classLoader)

        assertEquals(WorkspaceBom.FALLBACK_VERSION, generated)
        assert(generated != "0.0.13") {
            "workspace-bom drifted back to the obsolete 0.0.13 (S-019 P2-A)"
        }
    }

    private fun loaderWith(values: Map<String, String>): ClassLoader {
        val bytes = Properties().also { properties -> values.forEach(properties::setProperty) }
            .let { properties ->
                java.io.ByteArrayOutputStream().also { properties.store(it, null) }.toByteArray()
            }
        return object : ClassLoader() {
            override fun getResourceAsStream(name: String) =
                if (name == WorkspaceBom.RESOURCE_PATH) ByteArrayInputStream(bytes) else null
        }
    }
}
