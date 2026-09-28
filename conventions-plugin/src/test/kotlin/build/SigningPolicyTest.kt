package build

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SigningPolicyTest {

    @Test
    fun `signs locally when version is not a snapshot`() {
        assertTrue(
            SigningPolicy.shouldSign(isCi = false, isPublishing = false, version = "1.0.0"),
        )
    }

    @Test
    fun `does not sign a plain CI test build`() {
        // A test job has no GPG key imported: signing would fail the build.
        assertFalse(
            SigningPolicy.shouldSign(isCi = true, isPublishing = false, version = "1.0.0"),
        )
    }

    @Test
    fun `signs a release job even on CI`() {
        // The publish job imports a GPG key and sets SigningPolicy.PUBLISH_ENV_VAR.
        // Central rejects unsigned artefacts, so CI release must sign.
        assertTrue(
            SigningPolicy.shouldSign(isCi = true, isPublishing = true, version = "1.0.0"),
        )
    }

    @Test
    fun `does not sign a snapshot version`() {
        assertFalse(
            SigningPolicy.shouldSign(isCi = false, isPublishing = false, version = "1.0.0-SNAPSHOT"),
        )
    }

    @Test
    fun `does not sign a snapshot even on a publish job`() {
        assertFalse(
            SigningPolicy.shouldSign(isCi = true, isPublishing = true, version = "1.0.0-SNAPSHOT"),
        )
    }
}
