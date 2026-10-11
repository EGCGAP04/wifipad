package io.github.egcgap04.wifipad.protocol

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProtocolTest {
    // Test vector from docs/protocol.md.
    private val vectorState = GamepadState(
        buttons = GamepadState.A or GamepadState.START,
        lx = 32767,
        ly = -32768,
        rx = 0,
        ry = 1000,
        lt = 255,
        rt = 0,
    )
    private val vectorBytes = hex("47 50 01 00 01 00 00 00 81 00 FF 7F 00 80 00 00 E8 03 FF 00")

    @Test
    fun `encodes the test vector`() {
        assertContentEquals(vectorBytes, Protocol.encode(vectorState, seq = 1u))
    }

    @Test
    fun `decodes the test vector`() {
        assertEquals(Packet(1u, vectorState), Protocol.decode(vectorBytes))
    }

    @Test
    fun `round-trips extreme values`() {
        val states = listOf(
            GamepadState.NEUTRAL,
            GamepadState(0xFFFF, -32768, -32768, -32768, -32768, 0, 0),
            GamepadState(0xFFFF, 32767, 32767, 32767, 32767, 255, 255),
        )
        for (state in states) {
            for (seq in listOf(0u, 1u, Int.MAX_VALUE.toUInt(), UInt.MAX_VALUE)) {
                assertEquals(Packet(seq, state), Protocol.decode(Protocol.encode(state, seq)))
            }
        }
    }

    @Test
    fun `encodes the sequence number little-endian`() {
        val bytes = Protocol.encode(GamepadState.NEUTRAL, seq = 0xA1B2C3D4u)
        assertContentEquals(hex("D4 C3 B2 A1"), bytes.copyOfRange(4, 8))
    }

    @Test
    fun `decodes only the given length of a larger buffer`() {
        val buffer = vectorBytes.copyOf(64)
        assertEquals(Packet(1u, vectorState), Protocol.decode(buffer, length = 20))
        assertNull(Protocol.decode(buffer, length = 21))
        assertNull(Protocol.decode(buffer))
    }

    @Test
    fun `rejects packets with the wrong size`() {
        assertNull(Protocol.decode(vectorBytes.copyOf(19)))
        assertNull(Protocol.decode(vectorBytes.copyOf(21)))
        assertNull(Protocol.decode(ByteArray(0)))
        assertNull(Protocol.decode(vectorBytes.copyOf(19), length = 20))
    }

    @Test
    fun `rejects packets with the wrong magic`() {
        assertNull(Protocol.decode(vectorBytes.withByte(0, 'X')))
        assertNull(Protocol.decode(vectorBytes.withByte(1, 'X')))
        assertNull(Protocol.decode(vectorBytes.withByte(0, 'P').withByte(1, 'G')))
    }

    @Test
    fun `rejects unknown versions`() {
        assertNull(Protocol.decode(vectorBytes.withByte(2, 0)))
        assertNull(Protocol.decode(vectorBytes.withByte(2, 2)))
        assertNull(Protocol.decode(vectorBytes.withByte(2, 0xFF)))
    }

    @Test
    fun `ignores flags`() {
        assertEquals(Packet(1u, vectorState), Protocol.decode(vectorBytes.withByte(3, 0xFF)))
    }

    @Test
    fun `isNewer accepts the next sequence numbers`() {
        assertTrue(Protocol.isNewer(2u, 1u))
        assertTrue(Protocol.isNewer(1000u, 1u))
    }

    @Test
    fun `isNewer rejects equal and older sequence numbers`() {
        assertFalse(Protocol.isNewer(1u, 1u))
        assertFalse(Protocol.isNewer(1u, 2u))
        assertFalse(Protocol.isNewer(UInt.MAX_VALUE, 0u))
    }

    @Test
    fun `isNewer handles wrap-around`() {
        assertTrue(Protocol.isNewer(0u, UInt.MAX_VALUE))
        assertTrue(Protocol.isNewer(5u, UInt.MAX_VALUE - 5u))
        assertFalse(Protocol.isNewer(UInt.MAX_VALUE, 0u))
    }

    @Test
    fun `isNewer limits the distance to 2^31 - 1`() {
        val last = 0xFFFF_FF00u
        assertTrue(Protocol.isNewer(last + 0x7FFF_FFFFu, last))
        assertFalse(Protocol.isNewer(last + 0x8000_0000u, last))
        assertFalse(Protocol.isNewer(last + 0x8000_0001u, last))
    }

    private fun hex(text: String): ByteArray =
        text.split(' ').map { it.toInt(16).toByte() }.toByteArray()

    private fun ByteArray.withByte(index: Int, value: Int): ByteArray =
        copyOf().also { it[index] = value.toByte() }

    private fun ByteArray.withByte(index: Int, value: Char): ByteArray = withByte(index, value.code)
}
