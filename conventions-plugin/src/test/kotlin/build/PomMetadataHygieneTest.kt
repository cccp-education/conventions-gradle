package build

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PomMetadataHygieneTest {
    @Test
    fun `the organization root is invalid`() {
        assertTrue(PomMetadataHygiene.isInvalid("https://github.com/cccp-education"))
        assertTrue(PomMetadataHygiene.isInvalid("https://github.com/cccp-education/"))
    }

    @Test
    fun `a non-existent personal path is invalid`() {
        assertTrue(PomMetadataHygiene.isInvalid("https://github.com/cheroliv/jhipster-gradle-plugins"))
    }

    @Test
    fun `an exact repository url is valid`() {
        assertFalse(PomMetadataHygiene.isInvalid("https://github.com/cccp-education/graphify-gradle"))
        assertFalse(PomMetadataHygiene.isInvalid("https://github.com/cccp-education/jhipster-gradle-plugins"))
        assertFalse(PomMetadataHygiene.isInvalid(null))
    }
}
