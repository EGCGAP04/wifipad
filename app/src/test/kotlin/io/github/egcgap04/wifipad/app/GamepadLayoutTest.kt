package io.github.egcgap04.wifipad.app

import io.github.egcgap04.wifipad.protocol.GamepadState
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class GamepadLayoutTest {
    private val sizes = listOf(2400f to 1080f, 2340f to 1080f, 1080f to 2400f, 1920f to 1080f, 3200f to 1000f, 1280f to 800f)

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
    }

    @Test
    fun `sticks are placed like on an Xbox gamepad`() {
        val layout = GamepadLayout(2400f, 1080f)
        val left = layout.leftStick
        val right = layout.rightStick
        val x = layout.buttons.single { it.button == GamepadState.X }
        assertTrue(left.x < right.x && right.x < x.x, "left stick, right stick, face buttons")
        assertTrue(left.y < right.y, "the right stick is lower")
        assertTrue(right.x > layout.width / 2, "the right stick is on the right half")
        assertEquals(GamepadState.L3, left.click)
        assertEquals(GamepadState.R3, right.click)
        assertEquals(left.radius, right.radius)
    }

    @Test
    fun `the D-pad is below the left stick, left of the right stick`() {
        val layout = GamepadLayout(2400f, 1080f)
        val dpad = layout.dpad
        assertTrue(layout.leftStick.x < dpad.x && dpad.x < layout.rightStick.x)
        assertTrue(dpad.y > layout.leftStick.y)
        assertEquals(dpad.y, layout.rightStick.y)
    }

    @Test
    fun `triggers are in the top corners with the bumpers next to them`() {
        val layout = GamepadLayout(2400f, 1080f)
        val (lt, rt) = layout.triggers
        val lb = layout.buttons.single { it.button == GamepadState.LB }
        val rb = layout.buttons.single { it.button == GamepadState.RB }
        assertTrue(lt.isLeft && !rt.isLeft)
        assertTrue(lt.x < lb.x && lb.x < layout.width / 2 && layout.width / 2 < rb.x && rb.x < rt.x)
        for (control in listOf(lt, lb, rb, rt)) {
            assertTrue(layout.sticks.all { control.y < it.y - it.radius }, "above the sticks")
            assertTrue(layout.buttons.filter { it.label.length == 1 }.all { control.y < it.y }, "above the other buttons")
        }
    }

    @Test
    fun `Back, Guide and Start are in the middle, smaller than the face buttons`() {
        val layout = GamepadLayout(2400f, 1080f)
        val (back, guide, start) = listOf(GamepadState.BACK, GamepadState.GUIDE, GamepadState.START)
            .map { bit -> layout.buttons.single { it.button == bit } }
        val a = layout.buttons.single { it.button == GamepadState.A }
        assertTrue(back.x < guide.x && guide.x < start.x)
        assertEquals(layout.width / 2, guide.x)
        assertTrue(back.y == guide.y && guide.y == start.y)
        assertTrue(listOf(back, guide, start).all { it.radius < a.radius })
    }

    @Test
    fun `every protocol button has a control`() {
        val layout = GamepadLayout(2400f, 1080f)
        val sticks = layout.sticks.fold(0) { bits, stick -> bits or stick.click }
        val dpad = GamepadState.DPAD_UP or GamepadState.DPAD_DOWN or GamepadState.DPAD_LEFT or GamepadState.DPAD_RIGHT
        val all = layout.buttons.fold(sticks or dpad) { bits, button -> bits or button.button }
        assertEquals(0x7fff, all, "bits 0 to 14; bit 15 is reserved")
        assertEquals(layout.buttons.size, layout.buttons.map { it.button }.toSet().size, "no repeats")
    }

    @Test
    fun `finds the control under a touch`() {
        val layout = GamepadLayout(2400f, 1080f)
        val a = layout.buttons.single { it.button == GamepadState.A }
        assertSame(layout.leftStick, layout.controlAt(layout.leftStick.x, layout.leftStick.y))
        assertSame(layout.rightStick, layout.controlAt(layout.rightStick.x, layout.rightStick.y))
        assertSame(layout.dpad, layout.controlAt(layout.dpad.x, layout.dpad.y))
        assertSame(a, layout.controlAt(a.x + a.radius, a.y))
        assertSame(a, layout.controlAt(a.x, a.y + a.hitRadius * 0.99f), "a bit outside what is drawn")
        assertNull(layout.controlAt(0f, 0f))
    }

    @Test
    fun `stick axes follow the finger with up negative`() {
        val stick = Stick(500f, 500f, 100f, GamepadState.L3)
        assertEquals(0 to 0, stick.axes(500f, 500f))
        assertEquals(GamepadState.AXIS_MAX to 0, stick.axes(600f, 500f))
        assertEquals(-GamepadState.AXIS_MAX to 0, stick.axes(400f, 500f))
        assertEquals(0 to -GamepadState.AXIS_MAX, stick.axes(500f, 400f), "up")
        assertEquals(0 to GamepadState.AXIS_MAX / 2, stick.axes(500f, 550f), "truncated")
    }

    @Test
    fun `stick axes are clamped to the unit circle`() {
        val stick = Stick(500f, 500f, 100f, GamepadState.L3)
        assertEquals(GamepadState.AXIS_MAX to 0, stick.axes(900f, 500f))
        val (x, y) = stick.axes(800f, 200f)
        assertEquals(23169, x)
        assertEquals(-23169, y)
        assertTrue(hypot(x.toDouble(), y.toDouble()) <= GamepadState.AXIS_MAX)
    }

    @Test
    fun `the D-pad picks a direction by the angle`() {
        val dpad = DPad(500f, 500f, 100f)
        assertEquals(GamepadState.DPAD_UP, dpad.directionsAt(500f, 420f))
        assertEquals(GamepadState.DPAD_DOWN, dpad.directionsAt(500f, 580f))
        assertEquals(GamepadState.DPAD_LEFT, dpad.directionsAt(420f, 500f))
        assertEquals(GamepadState.DPAD_RIGHT, dpad.directionsAt(580f, 500f))
        assertEquals(GamepadState.DPAD_UP or GamepadState.DPAD_RIGHT, dpad.directionsAt(560f, 440f))
        assertEquals(GamepadState.DPAD_DOWN or GamepadState.DPAD_LEFT, dpad.directionsAt(440f, 560f))
        assertEquals(GamepadState.DPAD_RIGHT, dpad.directionsAt(580f, 470f), "20° up is still right")
        assertEquals(GamepadState.DPAD_RIGHT or GamepadState.DPAD_UP, dpad.directionsAt(580f, 460f), "26° up is diagonal")
        assertEquals(GamepadState.DPAD_LEFT, dpad.directionsAt(0f, 500f), "beyond the D-pad, as the finger slides off")
    }

    @Test
    fun `the D-pad has a dead zone in the center`() {
        val dpad = DPad(500f, 500f, 100f)
        assertEquals(0, dpad.directionsAt(500f, 500f))
        assertEquals(0, dpad.directionsAt(524f, 500f))
        assertEquals(GamepadState.DPAD_RIGHT, dpad.directionsAt(526f, 500f))
    }

    @Test
    fun `the D-pad never presses opposite directions`() {
        val dpad = DPad(500f, 500f, 100f)
        val seen = HashSet<Int>()
        for (degrees in 0 until 360) {
            val angle = Math.toRadians(degrees.toDouble())
            val pressed = dpad.directionsAt(500f + 80f * cos(angle).toFloat(), 500f - 80f * sin(angle).toFloat())
            assertTrue(pressed != 0, "$degrees°")
            assertTrue(pressed and UP_DOWN != UP_DOWN && pressed and LEFT_RIGHT != LEFT_RIGHT, "$degrees°")
            seen += pressed
        }
        assertEquals(8, seen.size, "4 directions and 4 diagonals")
    }

    private fun Stick.axes(x: Float, y: Float) = axisX(x, y) to axisY(x, y)

    private companion object {
        const val UP_DOWN = GamepadState.DPAD_UP or GamepadState.DPAD_DOWN
        const val LEFT_RIGHT = GamepadState.DPAD_LEFT or GamepadState.DPAD_RIGHT
    }
}
