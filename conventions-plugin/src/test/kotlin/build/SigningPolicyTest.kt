package build

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SigningPolicyTest {

    @Test
    fun `signs when not on CI and version is not a snapshot`() {
        assertTrue(SigningPolicy.shouldSign(isCi = false, version = "1.0.0"))
    }

    @Test
    fun `does not sign on CI`() {
        assertFalse(SigningPolicy.shouldSign(isCi = true, version = "1.0.0"))
    }

    @Test
    fun `does not sign a snapshot version`() {
        assertFalse(SigningPolicy.shouldSign(isCi = false, version = "1.0.0-SNAPSHOT"))
    }

    @Test
    fun `does not sign a snapshot on CI`() {
        assertFalse(SigningPolicy.shouldSign(isCi = true, version = "1.0.0-SNAPSHOT"))
    }
}
