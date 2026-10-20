package io.github.egcgap04.wifipad.app

import android.app.Activity
import android.os.Bundle

/** The gamepad screen. */
class GamepadActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(GamepadView(this))
    }
}
