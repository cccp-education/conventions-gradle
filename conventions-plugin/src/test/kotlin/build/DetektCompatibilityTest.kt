package build

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** CNV-15.2 — the detekt JVM policy is pure and unit-tested. */
class DetektCompatibilityTest {

    @Test
    fun `java 21 is supported`() {
        assertTrue(DetektCompatibility.supports("21.0.1"))
    }

    @Test
    fun `java 22 is the inclusive upper bound`() {
        assertTrue(DetektCompatibility.supports("22"))
        assertTrue(DetektCompatibility.supports("22.0.2"))
    }

    @Test
    fun `java 25 is not supported`() {
        assertFalse(DetektCompatibility.supports("25"))
        assertFalse(DetektCompatibility.supports("25.0.5"))
    }

    @Test
    fun `java 26 and above are not supported`() {
        assertFalse(DetektCompatibility.supports("26"))
        assertFalse(DetektCompatibility.supports("30.0.1"))
    }

    @Test
    fun `legacy java 1_8 is supported`() {
        assertTrue(DetektCompatibility.supports("1.8.0_402"))
    }

    @Test
    fun `an unparsable version is not supported`() {
        assertFalse(DetektCompatibility.supports("unknown"))
        assertFalse(DetektCompatibility.supports(""))
    }

    @Test
    fun `diagnostic names the supported bound and the current version`() {
        assertEquals(
            "detekt 1.23.8 supports JVM <= 22 (current: 25.0.5) — skipped.",
            DetektCompatibility.diagnostic("25.0.5")
        )
    }
}
