package io.github.egcgap04.wifipad.server

import io.github.egcgap04.wifipad.protocol.GamepadState
import io.github.egcgap04.wifipad.protocol.Packet
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClientSessionTest {
    private val phone = InetSocketAddress("192.168.0.10", 40000)
    private val other = InetSocketAddress("192.168.0.11", 40000)
    private val pressed = GamepadState(buttons = GamepadState.A)
    private val ms = 1_000_000L

    private fun packet(seq: UInt, state: GamepadState = pressed) = Packet(seq, state)

    @Test
    fun `the first sender gets engaged with any seq`() {
        val session = ClientSession()
        assertEquals(pressed, session.accept(phone, packet(UInt.MAX_VALUE - 1u), now = 0))
        assertEquals(phone, session.client)
    }

    @Test
    fun `accepts only newer seq from the engaged client`() {
        val session = ClientSession()
        session.accept(phone, packet(10u), now = 0)
        assertNull(session.accept(phone, packet(10u), now = 1))
        assertNull(session.accept(phone, packet(9u), now = 2))
        assertEquals(GamepadState.NEUTRAL, session.accept(phone, packet(12u, GamepadState.NEUTRAL), now = 3))
        assertNull(session.accept(phone, packet(11u), now = 4))
    }

    @Test
    fun `seq wraps around`() {
        val session = ClientSession()
        session.accept(phone, packet(UInt.MAX_VALUE), now = 0)
        assertEquals(pressed, session.accept(phone, packet(0u), now = 1))
    }

    @Test
    fun `ignores other senders while a client is engaged`() {
        val session = ClientSession()
        session.accept(phone, packet(1u), now = 0)
        assertNull(session.accept(other, packet(100u), now = 1))
        assertEquals(phone, session.client)
    }

    @Test
    fun `expires after 250 ms without accepted packets`() {
        val session = ClientSession()
        assertFalse(session.expire(now = 1000 * ms), "nothing to expire without a client")

        session.accept(phone, packet(1u), now = 0)
        session.accept(phone, packet(2u), now = 100 * ms)
        assertFalse(session.expire(now = 349 * ms))
        assertTrue(session.expire(now = 350 * ms))
        assertNull(session.client)
        assertFalse(session.expire(now = 1000 * ms), "expires only once")
    }

    @Test
    fun `ignored packets do not keep the client engaged`() {
        val session = ClientSession()
        session.accept(phone, packet(5u), now = 0)
        session.accept(phone, packet(4u), now = 200 * ms)
        session.accept(other, packet(1u), now = 200 * ms)
        assertTrue(session.expire(now = 250 * ms))
    }

    @Test
    fun `after a timeout any sender gets engaged with any seq`() {
        val session = ClientSession()
        session.accept(phone, packet(1000u), now = 0)
        session.expire(now = 250 * ms)
        assertEquals(pressed, session.accept(other, packet(1u), now = 300 * ms))
        assertEquals(other, session.client)
    }
}
