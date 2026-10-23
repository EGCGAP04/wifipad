package io.github.egcgap04.wifipad.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN &&
            checkSelfPermission(Manifest.permission.ACCESS_LOCAL_NETWORK) != PackageManager.PERMISSION_GRANTED
        ) {
            // Android 17 blocks traffic to the local network without it: sends fail with EPERM.
            requestPermissions(arrayOf(Manifest.permission.ACCESS_LOCAL_NETWORK), REQUEST_LOCAL_NETWORK)
        } else {
            openGamepad()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        // An empty result means the request was interrupted, not denied.
        if (requestCode != REQUEST_LOCAL_NETWORK || grantResults.isEmpty()) return
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openGamepad()
        } else {
            // After two denials Android stops asking, so point to the app settings.
            AlertDialog.Builder(this)
                .setMessage(R.string.local_network_denied)
                .setPositiveButton(R.string.open_settings) { _, _ ->
                    startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", packageName, null),
                        ),
                    )
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun openGamepad() {
        startActivity(Intent(this, GamepadActivity::class.java))
    }

    private companion object {
        const val REQUEST_LOCAL_NETWORK = 1
    }
}
