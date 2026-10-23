package io.github.egcgap04.wifipad.app

import io.github.egcgap04.wifipad.protocol.GamepadState
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A round control on screen, in pixels. A touch within [hitRadius] of the center grabs it: a bit
 * more than what is drawn, because fingers are not precise.
 */
sealed class Control(val x: Float, val y: Float, val radius: Float) {
    val hitRadius get() = radius * HIT_SCALE

    fun hits(px: Float, py: Float) = hypot(px - x, py - y) <= hitRadius

    private companion object {
        const val HIT_SCALE = 1.3f
    }
}

/** A stick; [click] is the button it presses when pushed in (`L3` or `R3`). */
class Stick(x: Float, y: Float, radius: Float, val click: Int) : Control(x, y, radius) {
    /**
     * The axis value for a finger at [px] on the X axis (or [py] on Y), with the offset from the
     * center clamped to the unit circle. Up is negative, as on screen and in the protocol.
     */
    fun axisX(px: Float, py: Float) = axis(px - x, py - y)

    fun axisY(px: Float, py: Float) = axis(py - y, px - x)

    private fun axis(along: Float, across: Float): Int {
        val length = hypot(along, across)
        val scale = if (length > radius) length else radius
        // Truncate: rounding could push a diagonal just outside the circle (23170² · 2 > 32767²).
        return (along / scale * GamepadState.AXIS_MAX).toInt()
    }
}

/**
 * A D-pad. Unlike a button, it follows the finger: the direction comes from the angle to the center,
 * so sliding the finger changes it without lifting.
 */
class DPad(x: Float, y: Float, radius: Float) : Control(x, y, radius) {
    /**
     * The D-pad bits for a finger at ([px], [py]): one of 8 sectors of 45°, where diagonals press
     * two directions, or none within [DEAD_ZONE] of the center. Opposite directions never come
     * together.
     */
    fun directionsAt(px: Float, py: Float): Int {
        val dx = px - x
        val dy = py - y
        if (hypot(dx, dy) < radius * DEAD_ZONE) return 0
        // Screen Y grows downwards: flip it so the angle goes counterclockwise from the right.
        val sector = (atan2(-dy, dx) / (PI / 4)).roundToInt()
        return SECTORS[sector.mod(SECTORS.size)]
    }

    companion object {
        /** The size of the dead zone, as a fraction of the radius. */
        const val DEAD_ZONE = 0.25f

        private val SECTORS = intArrayOf(
            GamepadState.DPAD_RIGHT,
            GamepadState.DPAD_RIGHT or GamepadState.DPAD_UP,
            GamepadState.DPAD_UP,
            GamepadState.DPAD_UP or GamepadState.DPAD_LEFT,
            GamepadState.DPAD_LEFT,
            GamepadState.DPAD_LEFT or GamepadState.DPAD_DOWN,
            GamepadState.DPAD_DOWN,
            GamepadState.DPAD_DOWN or GamepadState.DPAD_RIGHT,
        )
    }
}

/** A button that sets [button] (a `GamepadState` bit) while pressed. */
class Button(x: Float, y: Float, radius: Float, val button: Int, val label: String) :
    Control(x, y, radius)

/**
 * A trigger: the screen does not measure pressure, so it is either released (0) or fully pressed
 * (`TRIGGER_MAX`).
 */
class Trigger(x: Float, y: Float, radius: Float, val isLeft: Boolean, val label: String) :
    Control(x, y, radius)

/** Where each control goes on a screen of [width] × [height] pixels. */
class GamepadLayout(val width: Float, val height: Float) {
    // Sizes depend on `unit`, the side of the square that fits in each half of the screen, and
    // positions on the screen size. It also works in portrait, just smaller. As on an Xbox gamepad,
    // the left stick sits above the D-pad and the right stick below the face buttons. The triggers
    // go in the top corners with the bumpers next to them, and Back, Guide and Start in the middle.
    private val unit = min(width / 2, height)
    private val shoulderY = height * 0.12f
    private val shoulderRadius = unit * 0.07f

    val leftStick = Stick(width * 0.17f, height * 0.5f, unit * 0.19f, GamepadState.L3)
    val rightStick = Stick(width * 0.66f, height * 0.78f, unit * 0.19f, GamepadState.R3)
    val dpad = DPad(width * 0.34f, height * 0.78f, unit * 0.14f)

    val buttons: List<Button> = run {
        val x = width * 0.83f
        val y = height * 0.5f
        val gap = unit * 0.15f
        val radius = unit * 0.07f
        listOf(
            // Xbox positions: Y on top, A at the bottom, X on the left, B on the right.
            Button(x, y - gap, radius, GamepadState.Y, "Y"),
            Button(x, y + gap, radius, GamepadState.A, "A"),
            Button(x - gap, y, radius, GamepadState.X, "X"),
            Button(x + gap, y, radius, GamepadState.B, "B"),
            Button(width * 0.2f, shoulderY, shoulderRadius, GamepadState.LB, "LB"),
            Button(width * 0.8f, shoulderY, shoulderRadius, GamepadState.RB, "RB"),
        ) + run {
            // Smaller and apart from each other, so they are not pressed by accident.
            val centerY = height * 0.2f
            val centerRadius = unit * 0.05f
            listOf(
                Button(width * 0.42f, centerY, centerRadius, GamepadState.BACK, "⧉"),
                Button(width * 0.5f, centerY, centerRadius, GamepadState.GUIDE, "⌂"),
                Button(width * 0.58f, centerY, centerRadius, GamepadState.START, "≡"),
            )
        }
    }

    val triggers = listOf(
        Trigger(width * 0.07f, shoulderY, shoulderRadius, isLeft = true, "LT"),
        Trigger(width * 0.93f, shoulderY, shoulderRadius, isLeft = false, "RT"),
    )

    val sticks = listOf(leftStick, rightStick)

    val controls: List<Control> = sticks + dpad + triggers + buttons

    /** The control under a touch at ([x], [y]), if any. Hit areas do not overlap. */
    fun controlAt(x: Float, y: Float): Control? = controls.firstOrNull { it.hits(x, y) }
}
