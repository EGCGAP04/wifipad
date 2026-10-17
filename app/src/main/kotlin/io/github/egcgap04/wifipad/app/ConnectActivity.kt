package io.github.egcgap04.wifipad.app

import android.app.Activity
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import java.util.Locale

/** First screen: the address of the PC running the server. */
class ConnectActivity : Activity() {
    private lateinit var store: ConnectionStore
    private lateinit var hostInput: EditText
    private lateinit var portInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect)
        store = ConnectionStore(this)
        hostInput = findViewById(R.id.host)
        portInput = findViewById(R.id.port)

        // On recreation (rotation), the views restore what the user typed.
        if (savedInstanceState == null) {
            val saved = store.load()
            hostInput.setText(saved.host)
            portInput.setText(String.format(Locale.ROOT, "%d", saved.port))
        }
        portInput.setOnEditorActionListener { _, actionId, _ ->
            (actionId == EditorInfo.IME_ACTION_GO).also { if (it) connect() }
        }
        findViewById<Button>(R.id.connect).setOnClickListener { connect() }
    }

    private fun connect() {
        val host = ConnectionSettings.parseHost(hostInput.text.toString())
        val port = ConnectionSettings.parsePort(portInput.text.toString())
        hostInput.error = if (host == null) getString(R.string.error_host) else null
        portInput.error = if (port == null) getString(R.string.error_port) else null
        if (host == null || port == null) return

        store.save(ConnectionSettings(host, port))
        // TODO(#22): open the gamepad screen instead.
        Toast.makeText(this, getString(R.string.saved, host, port), Toast.LENGTH_SHORT).show()
    }
}
