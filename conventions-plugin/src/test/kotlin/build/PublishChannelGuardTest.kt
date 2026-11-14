package build

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PublishChannelGuardTest {

    @Test
    fun `central-only tasks are allowed on the central channel`() {
        assertNull(PublishChannelGuard.violation(PublicationChannel.CENTRAL, "publishPlugins"))
        assertNull(
            PublishChannelGuard.violation(
                PublicationChannel.CENTRAL,
                "publishAggregationToCentralPortal",
            ),
        )
        assertNull(
            PublishChannelGuard.violation(
                PublicationChannel.CENTRAL,
                "publishAllPublicationsToCentralPortal",
            ),
        )
    }

    @Test
    fun `central-only tasks are refused on the jitpack channel`() {
        assertNotNull(PublishChannelGuard.violation(PublicationChannel.JITPACK, "publishPlugins"))
        assertNotNull(
            PublishChannelGuard.violation(
                PublicationChannel.JITPACK,
                "publishAggregationToCentralPortal",
            ),
        )
        assertNotNull(
            PublishChannelGuard.violation(
                PublicationChannel.JITPACK,
                "publishAllPublicationsToCentralPortal",
            ),
        )
    }

    @Test
    fun `local and jitpack publication tasks are allowed in both channels`() {
        for (channel in PublicationChannel.entries) {
            assertNull(PublishChannelGuard.violation(channel, "publishToMavenLocal"))
            assertNull(PublishChannelGuard.violation(channel, "publish"))
            assertNull(PublishChannelGuard.violation(channel, "generatePomFileForMavenPublication"))
        }
    }
}
