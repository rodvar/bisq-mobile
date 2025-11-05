package network.bisq.mobile.domain.utils

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ByteArrayUtilRoundTripTest {
    @Test
    fun hex_round_trip() {
        val input = byteArrayOf(0x00, 0x01, 0x7f, 0x80.toByte(), 0xff.toByte())
        val hex = input.toHex()
        val back = hex.hexToByteArray()
        assertContentEquals(input, back)
        assertEquals("00017f80ff", hex)
    }
}

