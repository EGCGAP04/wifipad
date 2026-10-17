package io.github.egcgap04.wifipad.app

import android.content.Context
import io.github.egcgap04.wifipad.protocol.Protocol

/** Where to send the packets: the PC running the wifipad server. */
data class ConnectionSettings(val host: String, val port: Int) {
    companion object {
        /** Returns the dotted IPv4 address in [text] without spaces, or null if it is not one. */
        fun parseHost(text: String): String? {
            val host = text.trim()
            val parts = host.split('.')
            val valid = parts.size == 4 && parts.all { part ->
                part.length in 1..3 && part.all { it in '0'..'9' } && part.toInt() <= 255
            }
            return host.takeIf { valid }
        }

        /** Returns the port in [text], or null if it is not in 1..65535. */
        fun parsePort(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it in 1..65535 }
    }
}

/** Keeps the last [ConnectionSettings] in [SharedPreferences][android.content.SharedPreferences]. */
class ConnectionStore(context: Context) {
    private val prefs = context.getSharedPreferences("connection", Context.MODE_PRIVATE)

    /** The saved settings; the host is empty if none were saved yet. */
    fun load() = ConnectionSettings(
        host = prefs.getString(KEY_HOST, null).orEmpty(),
        port = prefs.getInt(KEY_PORT, Protocol.DEFAULT_PORT),
    )

    fun save(settings: ConnectionSettings) {
        prefs.edit()
            .putString(KEY_HOST, settings.host)
            .putInt(KEY_PORT, settings.port)
            .apply()
    }

    private companion object {
        const val KEY_HOST = "host"
        const val KEY_PORT = "port"
    }
}
