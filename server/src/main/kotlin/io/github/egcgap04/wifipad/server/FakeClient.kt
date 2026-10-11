package io.github.egcgap04.wifipad.server

import io.github.egcgap04.wifipad.protocol.GamepadState
import io.github.egcgap04.wifipad.protocol.Protocol
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.util.concurrent.locks.LockSupport
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// Test client without a phone: sends an animated pattern at 60 Hz (`./gradlew :server:runFakeClient`).

private const val FAKE_CLIENT_USAGE = """Usage: runFakeClient [--host HOST] [--port PORT]

Sends an animated test pattern to a wifipad server at 60 Hz, like the app would.

Options:
  --host HOST   Server address (default: 127.0.0.1)
  --port PORT   Server UDP port (default: 7654)
  -h, --help    Show this help"""

private const val PERIOD_NANOS = 1_000_000_000L / 60
private const val BUTTON_NANOS = 500_000_000L
private const val BUTTON_COUNT = 15 // Bits 0..14: every button, D-pad included.

internal data class FakeClientOptions(
    val host: String = "127.0.0.1",
    val port: Int = Protocol.DEFAULT_PORT,
    val help: Boolean = false,
)

/** Throws [IllegalArgumentException] with a message for the user if [args] are invalid. */
internal fun parseFakeClientArgs(args: Array<String>): FakeClientOptions {
    var options = FakeClientOptions()
    var i = 0
    while (i < args.size) {
        when (val arg = args[i++]) {
            "-h", "--help" -> options = options.copy(help = true)
            "--host" -> options = options.copy(host = requireNotNull(args.getOrNull(i++)) { "--host needs a value" })
            "--port" -> options = options.copy(port = parsePort(args.getOrNull(i++)))
            else -> throw IllegalArgumentException("unknown option: $arg")
        }
    }
    return options
}

/**
 * The pattern at [elapsedNanos]: the left stick turns once every 2 s and the right one the other
 * way every 3 s, the triggers go up and down in opposite phase, and the buttons take turns every
 * 0.5 s.
 */
internal fun fakeState(elapsedNanos: Long): GamepadState {
    val seconds = elapsedNanos / 1e9
    val left = 2 * PI * seconds / 2
    val right = -2 * PI * seconds / 3
    val lt = triangle(seconds / 2)
    return GamepadState(
        buttons = 1 shl (elapsedNanos / BUTTON_NANOS % BUTTON_COUNT).toInt(),
        lx = (cos(left) * GamepadState.AXIS_MAX).roundToInt(),
        ly = (sin(left) * GamepadState.AXIS_MAX).roundToInt(),
        rx = (cos(right) * GamepadState.AXIS_MAX).roundToInt(),
        ry = (sin(right) * GamepadState.AXIS_MAX).roundToInt(),
        lt = lt,
        rt = GamepadState.TRIGGER_MAX - lt,
    )
}

/** 0 → [GamepadState.TRIGGER_MAX] → 0 once per unit of [cycles]. */
private fun triangle(cycles: Double): Int {
    val phase = cycles % 1
    val up = if (phase < 0.5) phase * 2 else 2 - phase * 2
    return (up * GamepadState.TRIGGER_MAX).roundToInt()
}

fun main(args: Array<String>) {
    val options = try {
        parseFakeClientArgs(args)
    } catch (e: IllegalArgumentException) {
        fail("${e.message}\n\n$FAKE_CLIENT_USAGE", status = 2)
    }
    if (options.help) {
        println(FAKE_CLIENT_USAGE)
        return
    }
    val server = InetSocketAddress(options.host, options.port)
    if (server.isUnresolved) fail("Unknown host: ${options.host}")

    println("Sending a test pattern to ${options.host}:${options.port} at 60 Hz. Press Ctrl+C to stop.")
    DatagramSocket().use { socket ->
        val start = System.nanoTime()
        var next = start
        var seq = 0u
        while (true) {
            val bytes = Protocol.encode(fakeState(System.nanoTime() - start), seq++)
            socket.send(DatagramPacket(bytes, bytes.size, server))

            // Fixed rate without drift; after a long stall, continue from now instead of catching up.
            val now = System.nanoTime()
            next = maxOf(next + PERIOD_NANOS, now)
            LockSupport.parkNanos(next - now)
        }
    }
}
