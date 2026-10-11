package io.github.egcgap04.wifipad.server

import io.github.egcgap04.wifipad.protocol.GamepadState
import io.github.egcgap04.wifipad.protocol.Protocol
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketException
import java.net.SocketTimeoutException
import kotlin.system.exitProcess

private const val USAGE = """Usage: server [--port PORT]

Receives wifipad packets over UDP and exposes them as a virtual gamepad.

Options:
  --port PORT   UDP port to listen on (default: 7654)
  -h, --help    Show this help"""

// How often receive() wakes up to check the client timeout.
private const val POLL_MILLIS = 50

// Larger than a packet, so oversized datagrams are not truncated to a valid size.
private const val RECEIVE_BUFFER_SIZE = 64

internal data class Options(val port: Int = Protocol.DEFAULT_PORT, val help: Boolean = false)

/** Throws [IllegalArgumentException] with a message for the user if [args] are invalid. */
internal fun parseArgs(args: Array<String>): Options {
    var options = Options()
    var i = 0
    while (i < args.size) {
        when (val arg = args[i++]) {
            "-h", "--help" -> options = options.copy(help = true)
            "--port" -> {
                val value = requireNotNull(args.getOrNull(i++)) { "--port needs a value" }
                val port = value.toIntOrNull()
                require(port != null && port in 1..65535) { "invalid port: $value" }
                options = options.copy(port = port)
            }
            else -> throw IllegalArgumentException("unknown option: $arg")
        }
    }
    return options
}

@Volatile
private var running = true

fun main(args: Array<String>) {
    val options = try {
        parseArgs(args)
    } catch (e: IllegalArgumentException) {
        fail("${e.message}\n\n$USAGE", status = 2)
    }
    if (options.help) {
        println(USAGE)
        return
    }

    val socket = try {
        DatagramSocket(options.port)
    } catch (e: SocketException) {
        fail("Could not listen on UDP port ${options.port}: ${e.message}")
    }
    socket.use {
        val gamepad = try {
            VirtualGamepad.create()
        } catch (e: IllegalStateException) {
            fail(e.message ?: "Could not create the virtual gamepad")
        }
        gamepad.use {
            println("Listening on UDP port ${options.port}. Virtual gamepad: ${gamepad.devnode ?: "?"}")
            stopOnShutdown(socket)
            serve(socket, gamepad)
        }
    }
    println("Stopped")
}

private fun fail(message: String, status: Int = 1): Nothing {
    System.err.println("Error: $message")
    exitProcess(status)
}

/**
 * On Ctrl+C, closes the socket so receive() fails and the main thread leaves [serve], and waits
 * for it to destroy the device. The device is never touched from two threads.
 */
private fun stopOnShutdown(socket: DatagramSocket) {
    val mainThread = Thread.currentThread()
    Runtime.getRuntime().addShutdownHook(
        Thread {
            running = false
            socket.close()
            mainThread.join(1000)
        },
    )
}

private fun serve(socket: DatagramSocket, gamepad: VirtualGamepad) {
    val buffer = ByteArray(RECEIVE_BUFFER_SIZE)
    val datagram = DatagramPacket(buffer, buffer.size)
    val session = ClientSession()
    socket.soTimeout = POLL_MILLIS

    while (running) {
        datagram.length = buffer.size
        val received = try {
            socket.receive(datagram)
            true
        } catch (e: SocketTimeoutException) {
            false
        } catch (e: SocketException) {
            if (!running) break
            throw e
        }

        // Expire before accepting: a packet after the timeout re-engages with any seq.
        val now = System.nanoTime()
        if (session.expire(now)) {
            gamepad.update(GamepadState.NEUTRAL)
            println("Client timed out")
        }
        if (!received) continue

        val packet = Protocol.decode(buffer, datagram.length) ?: continue
        val wasIdle = session.client == null
        val state = session.accept(datagram.socketAddress, packet, now) ?: continue
        if (wasIdle) println("Client connected: ${session.client}")
        gamepad.update(state)
    }
}
