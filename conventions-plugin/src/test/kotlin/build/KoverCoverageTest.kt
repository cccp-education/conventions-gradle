package build

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KoverCoverageTest {

    private val report =
        """
        <report name="test">
        <package name="sample">
        <class name="sample/Covered">
        <counter type="INSTRUCTION" missed="0" covered="15"/>
        <counter type="INSTRUCTION" missed="18" covered="0"/>
        </class>
        <counter type="INSTRUCTION" missed="18" covered="15"/>
        </package>
        <counter type="INSTRUCTION" missed="18" covered="15"/>
        </report>
        """.trimIndent()

    @Test
    fun `summarize aggregates every instruction counter`() {
        val summary = KoverCoverage.summarize(report)

        assertEquals(54L, summary.missed)
        assertEquals(45L, summary.covered)
    }

    @Test
    fun `percent is the covered over total ratio`() {
        val summary = KoverCoverage.summarize(report)

        assertEquals(45.454, summary.percent, 0.001)
    }

    @Test
    fun `empty report yields zero summary without division error`() {
        val summary = KoverCoverage.summarize("<report></report>")

        assertEquals(0L, summary.total)
        assertEquals(0.0, summary.percent, 0.0)
    }

    @Test
    fun `meetsThreshold is inclusive at the boundary`() {
        val boundary = """<counter type="INSTRUCTION" missed="15" covered="85"/>"""

        assertTrue(KoverCoverage.meetsThreshold(boundary, 85.0))
        assertFalse(KoverCoverage.meetsThreshold(boundary, 85.01))
    }

    @Test
    fun `meetsThreshold fails when coverage is below the threshold`() {
        val low = """<counter type="INSTRUCTION" missed="84" covered="16"/>"""

        assertFalse(KoverCoverage.meetsThreshold(low, 85.0))
        assertTrue(KoverCoverage.meetsThreshold(low, 16.0))
    }

    @Test
    fun `ignores counters other than instruction`() {
        val mixed =
            """
            <counter type="INSTRUCTION" missed="1" covered="9"/>
            <counter type="BRANCH" missed="100" covered="0"/>
            <counter type="LINE" missed="100" covered="0"/>
            """.trimIndent()

        val summary = KoverCoverage.summarize(mixed)

        assertEquals(1L, summary.missed)
        assertEquals(9L, summary.covered)
    }
}
