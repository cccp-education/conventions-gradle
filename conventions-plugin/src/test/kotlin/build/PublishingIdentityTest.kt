package build

import kotlin.test.Test
import kotlin.test.assertEquals

class PublishingIdentityTest {

    @Test
    fun `resolve keeps the cccp defaults when every override is absent`() {
        assertEquals(PublishingIdentity.DEFAULT, PublishingIdentity.resolve())
    }

    @Test
    fun `resolve applies a full proprietary identity`() {
        val identity = PublishingIdentity.resolve(
            licenseName = "Proprietary — CSS",
            licenseUrl = "https://cheroliv.com/license",
            developerId = "cheroliv",
            developerName = "cheroliv",
            developerEmail = "cheroliv.developer@gmail.com",
        )

        assertEquals("Proprietary — CSS", identity.licenseName)
        assertEquals("https://cheroliv.com/license", identity.licenseUrl)
        assertEquals("cheroliv", identity.developerId)
        assertEquals("cheroliv", identity.developerName)
        assertEquals("cheroliv.developer@gmail.com", identity.developerEmail)
    }

    @Test
    fun `resolve falls back to the default for blank overrides`() {
        val identity = PublishingIdentity.resolve(
            licenseName = "  ",
            licenseUrl = "",
            developerId = "   ",
            developerName = "",
            developerEmail = "\t",
        )

        assertEquals(PublishingIdentity.DEFAULT, identity)
    }

    @Test
    fun `resolve trims the overrides`() {
        val identity = PublishingIdentity.resolve(
            licenseName = "  Proprietary — CSS  ",
            developerId = "  cheroliv  ",
        )

        assertEquals("Proprietary — CSS", identity.licenseName)
        assertEquals("cheroliv", identity.developerId)
    }

    @Test
    fun `resolve mixes custom and default values`() {
        val identity = PublishingIdentity.resolve(licenseName = "Proprietary — CSS")

        assertEquals("Proprietary — CSS", identity.licenseName)
        assertEquals(PublishingIdentity.DEFAULT_LICENSE_URL, identity.licenseUrl)
        assertEquals(PublishingIdentity.DEFAULT_DEVELOPER_ID, identity.developerId)
        assertEquals(PublishingIdentity.DEFAULT_DEVELOPER_NAME, identity.developerName)
        assertEquals(PublishingIdentity.DEFAULT_DEVELOPER_EMAIL, identity.developerEmail)
    }
}
