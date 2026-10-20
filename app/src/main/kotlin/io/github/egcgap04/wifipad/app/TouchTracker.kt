package io.github.egcgap04.wifipad.app

import io.github.egcgap04.wifipad.protocol.GamepadState

/**
 * Multitouch by pointer ID: each finger grabs the control where it went down and keeps it until it
 * goes up, even if it slides off. Only one finger at a time can hold the stick.
 */
class TouchTracker(val layout: GamepadLayout) {
    private val grabbed = HashMap<Int, Control>()
    private var lx = 0
    private var ly = 0

    var state = GamepadState.NEUTRAL
        private set

    fun down(pointerId: Int, x: Float, y: Float) {
        val control = layout.controlAt(x, y) ?: return
        if (control is Stick && control in grabbed.values) return
        grabbed[pointerId] = control
        if (control is Stick) moveStick(control, x, y)
        update()
    }

    fun move(pointerId: Int, x: Float, y: Float) {
        val control = grabbed[pointerId]
        if (control is Stick) {
            moveStick(control, x, y)
            update()
        }
    }

    fun up(pointerId: Int) {
        val control = grabbed.remove(pointerId) ?: return
        if (control is Stick) moveStick(control, control.x, control.y)
        update()
    }

    /** Releases everything, as for `ACTION_CANCEL`. */
    fun cancel() {
        grabbed.clear()
        lx = 0
        ly = 0
        update()
    }

    private fun moveStick(stick: Stick, x: Float, y: Float) {
        lx = stick.axisX(x, y)
        ly = stick.axisY(x, y)
    }

    private fun update() {
        var buttons = 0
        for (control in grabbed.values) {
            if (control is Button) buttons = buttons or control.button
        }
        state = GamepadState(buttons = buttons, lx = lx, ly = ly)
    }
}
