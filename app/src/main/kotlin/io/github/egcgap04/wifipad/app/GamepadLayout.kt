package io.github.egcgap04.wifipad.app

import io.github.egcgap04.wifipad.protocol.GamepadState
import kotlin.math.hypot
import kotlin.math.min

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

/** A button that sets [button] (a `GamepadState` bit) while pressed. */
class Button(x: Float, y: Float, radius: Float, val button: Int, val label: String) :
    Control(x, y, radius)

/** Where each control goes on a screen of [width] × [height] pixels. */
class GamepadLayout(val width: Float, val height: Float) {
    // Sizes depend on `unit`, the side of the square that fits in each half of the screen, and
    // positions on the screen size. It also works in portrait, just smaller. As on an Xbox gamepad,
    // the left stick sits above the D-pad and the right stick below the face buttons. Free space
    // left for later: the D-pad at about (0.34, 0.78), and the bumpers and triggers in the top
    // corners.
    private val unit = min(width / 2, height)

    val leftStick = Stick(width * 0.17f, height * 0.5f, unit * 0.19f, GamepadState.L3)
    val rightStick = Stick(width * 0.66f, height * 0.78f, unit * 0.19f, GamepadState.R3)

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
            Button(width * 0.5f, height * 0.2f, unit * 0.08f, GamepadState.START, "≡"),
        )
    }

    val sticks = listOf(leftStick, rightStick)

    val controls: List<Control> = sticks + buttons

    /** The control under a touch at ([x], [y]), if any. Hit areas do not overlap. */
    fun controlAt(x: Float, y: Float): Control? = controls.firstOrNull { it.hits(x, y) }
}
