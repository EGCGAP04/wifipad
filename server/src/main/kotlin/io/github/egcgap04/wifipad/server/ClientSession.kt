package io.github.egcgap04.wifipad.server

import io.github.egcgap04.wifipad.protocol.GamepadState
import io.github.egcgap04.wifipad.protocol.Packet
import io.github.egcgap04.wifipad.protocol.Protocol
import java.net.SocketAddress

/**
 * Server rules from `docs/protocol.md`: one client at a time, only newer `seq`, and a timeout.
 *
 * Times are `System.nanoTime()` values passed by the caller, so the logic can be tested without a
 * clock or a socket.
 */
internal class ClientSession(private val timeoutNanos: Long = TIMEOUT_NANOS) {
    /** The engaged client, or `null` if none. */
    var client: SocketAddress? = null
        private set

    private var lastSeq = 0u
    private var lastAcceptedAt = 0L

    /**
     * Returns the state of [packet] if it is accepted, or `null` if it has to be ignored. The first
     * sender gets engaged with any `seq`; after that, only its packets with a newer `seq` count.
     */
    fun accept(sender: SocketAddress, packet: Packet, now: Long): GamepadState? {
        val current = client
        if (current == null) {
            client = sender
        } else if (current != sender || !Protocol.isNewer(packet.seq, lastSeq)) {
            return null
        }
        lastSeq = packet.seq
        lastAcceptedAt = now
        return packet.state
    }

    /**
     * Releases the client if no packet was accepted in the last [timeoutNanos]. Returns `true` when
     * it does: the caller has to set the gamepad to [GamepadState.NEUTRAL].
     */
    fun expire(now: Long): Boolean {
        if (client == null || now - lastAcceptedAt < timeoutNanos) return false
        client = null
        return true
    }

    companion object {
        const val TIMEOUT_NANOS = 250_000_000L
    }
}
