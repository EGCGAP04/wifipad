package io.github.egcgap04.wifipad.app

import io.github.egcgap04.wifipad.protocol.GamepadState

/**
 * Multitouch by pointer ID: each finger grabs the control where it went down and keeps it until it
 * goes up, even if it slides off. Only one finger at a time can hold each stick or the D-pad.
 *
 * A double tap on a stick presses its [Stick.click] (L3/R3) while the second finger stays down: the
 * second touch has to come within [DOUBLE_TAP_MS] of the first one. Measuring from the first touch
 * down, not up, means that quickly grabbing the stick again after moving it does not count.
 */
class TouchTracker(val layout: GamepadLayout) {
    private val grabbed = HashMap<Int, Control>()

    /** The axes of each held stick, as (x, y). */
    private val axes = HashMap<Stick, Pair<Int, Int>>()

    /** Held sticks that are pushed in. */
    private val clicked = HashSet<Stick>()

    /** When each stick was last touched, for double taps. */
    private val lastTouch = HashMap<Stick, Long>()

    /** The pressed directions of each held D-pad. */
    private val directions = HashMap<DPad, Int>()

    var state = GamepadState.NEUTRAL
        private set

    /** A finger went down at [time], in milliseconds (`MotionEvent.getEventTime()`). */
    fun down(pointerId: Int, x: Float, y: Float, time: Long) {
        val control = layout.controlAt(x, y) ?: return
        when (control) {
            is Stick -> {
                if (control in axes) return
                val last = lastTouch.remove(control)
                if (last != null && time - last <= DOUBLE_TAP_MS) {
                    clicked += control
                } else {
                    // A third tap starts over instead of clicking again.
                    lastTouch[control] = time
                }
                moveStick(control, x, y)
            }
            is DPad -> {
                if (control in directions) return
                directions[control] = control.directionsAt(x, y)
            }
            is Button, is Trigger -> Unit
        }
        grabbed[pointerId] = control
        update()
    }

    fun move(pointerId: Int, x: Float, y: Float) {
        when (val control = grabbed[pointerId]) {
            is Stick -> moveStick(control, x, y)
            is DPad -> directions[control] = control.directionsAt(x, y)
            is Button, is Trigger, null -> return
        }
        update()
    }

    fun up(pointerId: Int) {
        when (val control = grabbed.remove(pointerId) ?: return) {
            is Stick -> {
                axes -= control
                clicked -= control
            }
            is DPad -> directions -= control
            is Button, is Trigger -> Unit
        }
        update()
    }

    /** Releases everything, as for `ACTION_CANCEL`. */
    fun cancel() {
        grabbed.clear()
        axes.clear()
        clicked.clear()
        lastTouch.clear()
        directions.clear()
        update()
    }

    private fun moveStick(stick: Stick, x: Float, y: Float) {
        axes[stick] = stick.axisX(x, y) to stick.axisY(x, y)
    }

    private fun update() {
        var buttons = 0
        var lt = 0
        var rt = 0
        for (control in grabbed.values) {
            when (control) {
                is Button -> buttons = buttons or control.button
                is Trigger -> if (control.isLeft) lt = GamepadState.TRIGGER_MAX else rt = GamepadState.TRIGGER_MAX
                is Stick, is DPad -> Unit
            }
        }
        for (stick in clicked) buttons = buttons or stick.click
        for (pressed in directions.values) buttons = buttons or pressed
        val (lx, ly) = axes[layout.leftStick] ?: CENTER
        val (rx, ry) = axes[layout.rightStick] ?: CENTER
        state = GamepadState(buttons = buttons, lx = lx, ly = ly, rx = rx, ry = ry, lt = lt, rt = rt)
    }

    companion object {
        /** The same as Android's default (`ViewConfiguration.getDoubleTapTimeout()`). */
        const val DOUBLE_TAP_MS = 300L

        private val CENTER = 0 to 0
    }
}
