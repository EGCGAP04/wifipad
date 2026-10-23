package io.github.egcgap04.wifipad.app

import io.github.egcgap04.wifipad.protocol.GamepadState
import kotlin.test.Test
import kotlin.test.assertEquals

class TouchTrackerTest {
    private val layout = GamepadLayout(2400f, 1080f)
    private val tracker = TouchTracker(layout)
    private val left = layout.leftStick
    private val right = layout.rightStick
    private fun button(bit: Int) = layout.buttons.single { it.button == bit }
    private val a = button(GamepadState.A)
    private val b = button(GamepadState.B)
    private val start = button(GamepadState.START)

    /** Touches are a second apart unless a test says otherwise, so they are never double taps. */
    private var time = 0L

    private fun TouchTracker.down(id: Int, x: Float, y: Float) = down(id, x, y, time.also { time += 1000 })

    private fun TouchTracker.down(id: Int, control: Control) = down(id, control.x, control.y)

    /** A short tap on [stick] at [time]. */
    private fun tap(stick: Stick, time: Long) {
        tracker.down(9, stick.x, stick.y, time)
        tracker.up(9)
    }

    @Test
    fun `starts neutral`() {
        assertEquals(GamepadState.NEUTRAL, tracker.state)
    }

    @Test
    fun `each finger presses its own button`() {
        tracker.down(0, a)
        assertEquals(GamepadState.A, tracker.state.buttons)
        tracker.down(1, b)
        tracker.down(2, start)
        assertEquals(GamepadState.A or GamepadState.B or GamepadState.START, tracker.state.buttons)
        tracker.up(0)
        assertEquals(GamepadState.B or GamepadState.START, tracker.state.buttons)
        tracker.up(2)
        tracker.up(1)
        assertEquals(GamepadState.NEUTRAL, tracker.state)
    }

    @Test
    fun `a button stays pressed until its finger goes up`() {
        tracker.down(0, a)
        tracker.move(0, 0f, 0f)
        tracker.move(0, b.x, b.y)
        assertEquals(GamepadState.A, tracker.state.buttons, "sliding off or onto another button")
        tracker.up(0)
        assertEquals(0, tracker.state.buttons)
    }

    @Test
    fun `two fingers on the same button`() {
        tracker.down(0, a)
        tracker.down(1, a)
        tracker.up(0)
        assertEquals(GamepadState.A, tracker.state.buttons)
        tracker.up(1)
        assertEquals(0, tracker.state.buttons)
    }

    @Test
    fun `touches outside the controls are ignored`() {
        tracker.down(0, 0f, 0f)
        tracker.move(0, a.x, a.y)
        tracker.up(0)
        tracker.down(0, a)
        tracker.up(5)
        assertEquals(GamepadState(buttons = GamepadState.A), tracker.state)
    }

    @Test
    fun `the left stick follows its finger and centers when it goes up`() {
        tracker.down(0, left.x + left.radius / 2, left.y)
        assertEquals(GamepadState.AXIS_MAX / 2, tracker.state.lx)
        tracker.move(0, left.x, left.y - left.radius * 2)
        assertEquals(0, tracker.state.lx)
        assertEquals(-GamepadState.AXIS_MAX, tracker.state.ly, "clamped, up is negative")
        tracker.move(0, 0f, left.y)
        assertEquals(-GamepadState.AXIS_MAX, tracker.state.lx, "keeps following outside the stick")
        tracker.up(0)
        assertEquals(GamepadState.NEUTRAL, tracker.state)
    }

    @Test
    fun `the right stick moves the right axes`() {
        tracker.down(0, right.x, right.y + right.radius)
        tracker.move(0, right.x - right.radius / 2, right.y)
        assertEquals(GamepadState(rx = -GamepadState.AXIS_MAX / 2), tracker.state)
        tracker.up(0)
        assertEquals(GamepadState.NEUTRAL, tracker.state)
    }

    @Test
    fun `both sticks and a button at the same time`() {
        tracker.down(0, left.x + left.radius, left.y)
        tracker.down(1, right.x, right.y - right.radius)
        tracker.down(2, a)
        assertEquals(
            GamepadState(buttons = GamepadState.A, lx = GamepadState.AXIS_MAX, ry = -GamepadState.AXIS_MAX),
            tracker.state,
        )
        tracker.move(0, left.x - left.radius, left.y)
        tracker.up(1)
        tracker.up(2)
        assertEquals(GamepadState(lx = -GamepadState.AXIS_MAX), tracker.state)
    }

    @Test
    fun `only one finger holds each stick`() {
        tracker.down(0, left.x + left.radius, left.y)
        tracker.down(1, left.x - left.radius, left.y)
        tracker.move(1, left.x, left.y + left.radius)
        assertEquals(GamepadState(lx = GamepadState.AXIS_MAX), tracker.state)
        tracker.up(1)
        assertEquals(GamepadState(lx = GamepadState.AXIS_MAX), tracker.state, "the second finger never held it")
    }

    @Test
    fun `a double tap pushes the stick in while the finger stays down`() {
        tap(left, 0)
        tracker.down(0, left.x, left.y, TouchTracker.DOUBLE_TAP_MS)
        assertEquals(GamepadState(buttons = GamepadState.L3), tracker.state)
        tracker.move(0, left.x + left.radius, left.y)
        assertEquals(GamepadState(buttons = GamepadState.L3, lx = GamepadState.AXIS_MAX), tracker.state, "still moves")
        tracker.up(0)
        assertEquals(GamepadState.NEUTRAL, tracker.state)
    }

    @Test
    fun `a double tap on the right stick presses R3`() {
        tap(right, 1000)
        tracker.down(0, right.x, right.y, 1100)
        tracker.down(1, a)
        assertEquals(GamepadState(buttons = GamepadState.R3 or GamepadState.A), tracker.state)
    }

    @Test
    fun `touches too far apart are not a double tap`() {
        tap(left, 0)
        tracker.down(0, left.x, left.y, TouchTracker.DOUBLE_TAP_MS + 1)
        assertEquals(GamepadState.NEUTRAL, tracker.state)
    }

    @Test
    fun `grabbing the stick again right after moving it is not a double tap`() {
        // Held for two seconds, then grabbed again 50 ms after letting go.
        tracker.down(0, left.x, left.y, 0)
        tracker.move(0, left.x - left.radius, left.y)
        tracker.up(0)
        tracker.down(0, left.x + left.radius, left.y, 2050)
        assertEquals(GamepadState(lx = GamepadState.AXIS_MAX), tracker.state)
    }

    @Test
    fun `taps on different sticks are not a double tap`() {
        tap(left, 0)
        tracker.down(0, right.x, right.y, 100)
        assertEquals(GamepadState.NEUTRAL, tracker.state)
    }

    @Test
    fun `a third quick tap does not push the stick in again`() {
        tap(left, 0)
        tap(left, 100)
        tracker.down(0, left.x, left.y, 200)
        assertEquals(GamepadState.NEUTRAL, tracker.state)
    }

    @Test
    fun `cancel releases everything`() {
        tracker.down(0, left.x + left.radius, left.y)
        tracker.down(1, a)
        tracker.cancel()
        assertEquals(GamepadState.NEUTRAL, tracker.state)
        tracker.move(0, left.x, left.y + left.radius)
        assertEquals(GamepadState.NEUTRAL, tracker.state, "fingers from before the cancel are forgotten")
    }

    @Test
    fun `cancel forgets the first tap`() {
        tap(left, 0)
        tracker.cancel()
        tracker.down(0, left.x, left.y, 100)
        assertEquals(GamepadState.NEUTRAL, tracker.state)
    }
}
