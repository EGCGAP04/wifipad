package io.github.egcgap04.wifipad.app

import io.github.egcgap04.wifipad.protocol.GamepadState
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class GamepadLayoutTest {
    private val sizes = listOf(2400f to 1080f, 1080f to 2400f, 1920f to 1080f, 3200f to 1000f, 1280f to 800f)

    @Test
    fun `controls are drawn inside the view`() {
        for ((width, height) in sizes) {
            for (c in GamepadLayout(width, height).controls) {
                assertTrue(
                    c.x - c.radius >= 0 && c.x + c.radius <= width && c.y - c.radius >= 0 && c.y + c.radius <= height,
                    "${c::class.simpleName} at (${c.x}, ${c.y}) in $width x $height",
                )
            }
        }
    }

    @Test
    fun `hit areas do not overlap`() {
        for ((width, height) in sizes) {
            val controls = GamepadLayout(width, height).controls
            for ((i, a) in controls.withIndex()) {
                for (b in controls.drop(i + 1)) {
                    assertTrue(hypot(a.x - b.x, a.y - b.y) > a.hitRadius + b.hitRadius, "$width x $height")
                }
            }
        }
    }

    @Test
    fun `face buttons are placed like on an Xbox gamepad`() {
        val layout = GamepadLayout(2400f, 1080f)
        val (y, a, x, b) = listOf(GamepadState.Y, GamepadState.A, GamepadState.X, GamepadState.B)
            .map { bit -> layout.buttons.single { it.button == bit } }
        assertTrue(y.y < x.y && x.y < a.y && x.x < y.x && y.x < b.x)
        assertTrue(layout.stick.x < x.x)
    }

    @Test
    fun `finds the control under a touch`() {
        val layout = GamepadLayout(2400f, 1080f)
        val a = layout.buttons.single { it.button == GamepadState.A }
        assertSame(layout.stick, layout.controlAt(layout.stick.x, layout.stick.y))
        assertSame(a, layout.controlAt(a.x + a.radius, a.y))
        assertSame(a, layout.controlAt(a.x, a.y + a.hitRadius * 0.99f), "a bit outside what is drawn")
        assertNull(layout.controlAt(0f, 0f))
    }

    @Test
    fun `stick axes follow the finger with up negative`() {
        val stick = Stick(500f, 500f, 100f)
        assertEquals(0 to 0, stick.axes(500f, 500f))
        assertEquals(GamepadState.AXIS_MAX to 0, stick.axes(600f, 500f))
        assertEquals(-GamepadState.AXIS_MAX to 0, stick.axes(400f, 500f))
        assertEquals(0 to -GamepadState.AXIS_MAX, stick.axes(500f, 400f), "up")
        assertEquals(0 to GamepadState.AXIS_MAX / 2, stick.axes(500f, 550f), "truncated")
    }

    @Test
    fun `stick axes are clamped to the unit circle`() {
        val stick = Stick(500f, 500f, 100f)
        assertEquals(GamepadState.AXIS_MAX to 0, stick.axes(900f, 500f))
        val (x, y) = stick.axes(800f, 200f)
        assertEquals(23169, x)
        assertEquals(-23169, y)
        assertTrue(hypot(x.toDouble(), y.toDouble()) <= GamepadState.AXIS_MAX)
    }

    private fun Stick.axes(x: Float, y: Float) = axisX(x, y) to axisY(x, y)
}
