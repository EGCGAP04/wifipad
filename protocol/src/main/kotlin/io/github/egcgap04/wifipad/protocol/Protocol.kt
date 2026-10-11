package io.github.egcgap04.wifipad.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class Packet(val seq: UInt, val state: GamepadState)

/**
 * Packet encoding and decoding for protocol v1 (`docs/protocol.md`).
 *
 * Only absolute `ByteBuffer` accessors are used: the `Buffer` methods (`position`, `flip`, ...)
 * compiled against JDK 9+ resolve to overloads that older Android versions may not have.
 */
object Protocol {
    const val VERSION = 1
    const val PACKET_SIZE = 20
    const val DEFAULT_PORT = 7654

    private const val MAGIC_0 = 'G'.code.toByte()
    private const val MAGIC_1 = 'P'.code.toByte()

    private const val OFFSET_MAGIC = 0
    private const val OFFSET_VERSION = 2
    private const val OFFSET_FLAGS = 3
    private const val OFFSET_SEQ = 4
    private const val OFFSET_BUTTONS = 8
    private const val OFFSET_LX = 10
    private const val OFFSET_LY = 12
    private const val OFFSET_RX = 14
    private const val OFFSET_RY = 16
    private const val OFFSET_LT = 18
    private const val OFFSET_RT = 19

    fun encode(state: GamepadState, seq: UInt): ByteArray {
        val bytes = ByteArray(PACKET_SIZE)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            .put(OFFSET_MAGIC, MAGIC_0)
            .put(OFFSET_MAGIC + 1, MAGIC_1)
            .put(OFFSET_VERSION, VERSION.toByte())
            .put(OFFSET_FLAGS, 0)
            .putInt(OFFSET_SEQ, seq.toInt())
            .putShort(OFFSET_BUTTONS, state.buttons.toShort())
            .putShort(OFFSET_LX, state.lx.toShort())
            .putShort(OFFSET_LY, state.ly.toShort())
            .putShort(OFFSET_RX, state.rx.toShort())
            .putShort(OFFSET_RY, state.ry.toShort())
            .put(OFFSET_LT, state.lt.toByte())
            .put(OFFSET_RT, state.rt.toByte())
        return bytes
    }

    /**
     * Decodes the first [length] bytes of [bytes], so a receive buffer can be passed without
     * copying it. Returns `null` if the size, magic or version is invalid. `flags` is ignored.
     */
    fun decode(bytes: ByteArray, length: Int = bytes.size): Packet? {
        if (length != PACKET_SIZE || bytes.size < PACKET_SIZE) return null
        if (bytes[OFFSET_MAGIC] != MAGIC_0 || bytes[OFFSET_MAGIC + 1] != MAGIC_1) return null
        if (bytes[OFFSET_VERSION].toInt() != VERSION) return null

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val state = GamepadState(
            buttons = buffer.getShort(OFFSET_BUTTONS).toInt() and 0xFFFF,
            lx = buffer.getShort(OFFSET_LX).toInt(),
            ly = buffer.getShort(OFFSET_LY).toInt(),
            rx = buffer.getShort(OFFSET_RX).toInt(),
            ry = buffer.getShort(OFFSET_RY).toInt(),
            lt = buffer.get(OFFSET_LT).toInt() and 0xFF,
            rt = buffer.get(OFFSET_RT).toInt() and 0xFF,
        )
        return Packet(buffer.getInt(OFFSET_SEQ).toUInt(), state)
    }

    /**
     * Serial number arithmetic (RFC 1982): `seq` is newer than `last` if
     * `(seq - last) mod 2^32` is in `1..2^31-1`. Never compare sequence numbers with `>`.
     */
    fun isNewer(seq: UInt, last: UInt): Boolean = (seq - last) in 1u..Int.MAX_VALUE.toUInt()
}
