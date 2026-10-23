package io.github.egcgap04.wifipad.app

import io.github.egcgap04.wifipad.protocol.GamepadState
import io.github.egcgap04.wifipad.protocol.Protocol
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * Sends the latest [state] to the server every [PERIOD_MS] until cancelled, and then a neutral
 * state so nothing stays pressed. States are never queued: each packet carries the current one.
 */
class UdpSender(
    /** A dotted IPv4 address, as returned by [ConnectionSettings.parseHost]. */
    private val host: String,
    private val port: Int,
    private val state: StateFlow<GamepadState>,
) {
    /**
     * Each run uses a new socket, so the server sees a new client: it accepts any `seq` once the
     * previous client times out (250 ms), and `seq` can start over from 0.
     */
    suspend fun run(): Unit = withContext(Dispatchers.IO) {
        // An IPv4 literal is parsed, not looked up: no DNS.
        val address = InetAddress.getByName(host)
        var seq = 0u
        DatagramSocket().use { socket ->
            fun send(state: GamepadState) {
                val bytes = Protocol.encode(state, seq++)
                try {
                    socket.send(DatagramPacket(bytes, bytes.size, address, port))
                } catch (e: IOException) {
                    // No network right now (Wi-Fi off or switching): the next packet tries again.
                }
            }
            try {
                while (true) {
                    send(state.value)
                    delay(PERIOD_MS)
                }
            } finally {
                // send() does not suspend, so it still runs after cancellation.
                send(GamepadState.NEUTRAL)
            }
        }
    }

    companion object {
        /** ~60 Hz, as `docs/protocol.md` asks. */
        const val PERIOD_MS = 16L
    }
}
