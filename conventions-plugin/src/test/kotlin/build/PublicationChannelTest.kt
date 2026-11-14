package build

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PublicationChannelTest {

    @Test
    fun `an absent or blank property defaults to jitpack`() {
        assertEquals(PublicationChannel.JITPACK, PublicationChannel.parse(null))
        assertEquals(PublicationChannel.JITPACK, PublicationChannel.parse(""))
        assertEquals(PublicationChannel.JITPACK, PublicationChannel.parse("   "))
    }

    @Test
    fun `the channel is parsed case-insensitively and trimmed`() {
        assertEquals(PublicationChannel.CENTRAL, PublicationChannel.parse("central"))
        assertEquals(PublicationChannel.CENTRAL, PublicationChannel.parse(" CENTRAL "))
        assertEquals(PublicationChannel.JITPACK, PublicationChannel.parse("JitPack"))
    }

    @Test
    fun `an unknown channel is refused`() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            PublicationChannel.parse("sonatype")
        }
        assertEquals(
            true,
            failure.message!!.contains("-Pchannel=central|jitpack"),
            failure.message,
        )
    }
}
