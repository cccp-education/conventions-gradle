package build

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KoverReportSourcesTest {

    @Test
    fun `main sources are measured`() {
        assertTrue(KoverReportSources.isIncluded(KoverReportSources.MAIN))
    }

    @Test
    fun `functional test sources are measured`() {
        assertTrue(KoverReportSources.isIncluded(KoverReportSources.FUNCTIONAL_TEST))
    }

    @Test
    fun `e2e test sources are excluded from the report`() {
        assertFalse(KoverReportSources.isIncluded("e2eTest"))
    }

    @Test
    fun `unit test sources are excluded from the report`() {
        assertFalse(KoverReportSources.isIncluded("test"))
    }

    @Test
    fun `included set lists main then functionalTest`() {
        assertEquals(listOf("main", "functionalTest"), KoverReportSources.included)
    }
}
