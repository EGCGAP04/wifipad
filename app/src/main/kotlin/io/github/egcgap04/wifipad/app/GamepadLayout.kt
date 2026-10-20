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

class Stick(x: Float, y: Float, radius: Float) : Control(x, y, radius) {
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
    // Each half of the screen gets a square of side `unit`: the stick on the left, the face
    // buttons on the right. It also works in portrait, just smaller.
    private val unit = min(width / 2, height)

    val stick = Stick(width * 0.25f, height * 0.55f, unit * 0.3f)

    val buttons: List<Button> = run {
        val x = width * 0.75f
        val y = height * 0.55f
        val gap = unit * 0.22f
        val radius = unit * 0.11f
        listOf(
            // Xbox positions: Y on top, A at the bottom, X on the left, B on the right.
            Button(x, y - gap, radius, GamepadState.Y, "Y"),
            Button(x, y + gap, radius, GamepadState.A, "A"),
            Button(x - gap, y, radius, GamepadState.X, "X"),
            Button(x + gap, y, radius, GamepadState.B, "B"),
            Button(width * 0.5f, height * 0.2f, unit * 0.08f, GamepadState.START, "≡"),
        )
    }

    val controls: List<Control> = listOf(stick) + buttons

    /** The control under a touch at ([x], [y]), if any. Hit areas do not overlap. */
    fun controlAt(x: Float, y: Float): Control? = controls.firstOrNull { it.hits(x, y) }
}
