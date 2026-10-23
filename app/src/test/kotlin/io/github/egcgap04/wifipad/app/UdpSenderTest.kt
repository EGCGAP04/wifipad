package io.github.egcgap04.wifipad.app

import io.github.egcgap04.wifipad.protocol.GamepadState
import io.github.egcgap04.wifipad.protocol.Packet
import io.github.egcgap04.wifipad.protocol.Protocol
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class UdpSenderTest {
    private val receiver = DatagramSocket(0, InetAddress.getLoopbackAddress()).apply {
        soTimeout = 1000
    }
    private val state = MutableStateFlow(GamepadState.NEUTRAL)
    private val received = mutableListOf<Packet>()

    @AfterTest
    fun closeReceiver() = receiver.close()

    private fun sender() = UdpSender("127.0.0.1", receiver.localPort, state)

    private fun receive(): Packet {
        val buffer = ByteArray(Protocol.PACKET_SIZE + 1)
        val datagram = DatagramPacket(buffer, buffer.size)
        receiver.receive(datagram)
        return assertNotNull(Protocol.decode(buffer, datagram.length)).also { received += it }
    }

    private fun receiveUntil(expected: GamepadState) {
        while (receive().state != expected) Unit
    }

    /** Reads what is left after the sender stopped and returns the last packet. */
    private fun drain(): Packet {
        receiver.soTimeout = 200
        try {
            while (true) receive()
        } catch (e: SocketTimeoutException) {
            return received.last()
        }
    }

    private fun assertSeqFromZero() {
        received.forEachIndexed { i, packet -> assertEquals(i.toUInt(), packet.seq) }
    }

    @Test
    fun `sends the latest state and a neutral one when cancelled`() = runBlocking {
        // Not on runBlocking's thread: receive() blocks it.
        val job = launch(Dispatchers.IO) { sender().run() }
        receiveUntil(GamepadState.NEUTRAL)
        val pressed = GamepadState(buttons = GamepadState.A, lx = 16383, ly = -32767)
        state.value = pressed
        receiveUntil(pressed)
        repeat(3) { assertEquals(pressed, receive().state) }

        job.cancelAndJoin()
        assertEquals(GamepadState.NEUTRAL, drain().state)
        assertSeqFromZero()
    }
}
