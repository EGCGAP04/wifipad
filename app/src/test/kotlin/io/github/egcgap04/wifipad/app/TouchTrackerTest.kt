package io.github.egcgap04.wifipad.app

import io.github.egcgap04.wifipad.protocol.GamepadState
import kotlin.test.Test
import kotlin.test.assertEquals

class TouchTrackerTest {
    private val layout = GamepadLayout(2400f, 1080f)
    private val tracker = TouchTracker(layout)
    private val stick = layout.stick
    private fun button(bit: Int) = layout.buttons.single { it.button == bit }
    private val a = button(GamepadState.A)
    private val b = button(GamepadState.B)
    private val start = button(GamepadState.START)

    private fun TouchTracker.down(id: Int, control: Control) = down(id, control.x, control.y)

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
    fun `the stick follows its finger and centers when it goes up`() {
        tracker.down(0, stick.x + stick.radius / 2, stick.y)
        assertEquals(GamepadState.AXIS_MAX / 2, tracker.state.lx)
        tracker.move(0, stick.x, stick.y - stick.radius * 2)
        assertEquals(0, tracker.state.lx)
        assertEquals(-GamepadState.AXIS_MAX, tracker.state.ly, "clamped, up is negative")
        tracker.move(0, 0f, stick.y)
        assertEquals(-GamepadState.AXIS_MAX, tracker.state.lx, "keeps following outside the stick")
        tracker.up(0)
        assertEquals(GamepadState.NEUTRAL, tracker.state)
    }

    @Test
    fun `stick and buttons at the same time`() {
        tracker.down(0, stick.x + stick.radius, stick.y)
        tracker.down(1, a)
        tracker.move(1, a.x + 10f, a.y)
        assertEquals(GamepadState(buttons = GamepadState.A, lx = GamepadState.AXIS_MAX), tracker.state)
        tracker.move(0, stick.x - stick.radius, stick.y)
        tracker.up(1)
        assertEquals(GamepadState(lx = -GamepadState.AXIS_MAX), tracker.state)
    }

    @Test
    fun `only one finger holds the stick`() {
        tracker.down(0, stick.x + stick.radius, stick.y)
        tracker.down(1, stick.x - stick.radius, stick.y)
        tracker.move(1, stick.x, stick.y + stick.radius)
        assertEquals(GamepadState(lx = GamepadState.AXIS_MAX), tracker.state)
        tracker.up(1)
        assertEquals(GamepadState(lx = GamepadState.AXIS_MAX), tracker.state, "the second finger never held it")
    }

    @Test
    fun `cancel releases everything`() {
        tracker.down(0, stick.x + stick.radius, stick.y)
        tracker.down(1, a)
        tracker.cancel()
        assertEquals(GamepadState.NEUTRAL, tracker.state)
        tracker.move(0, stick.x, stick.y + stick.radius)
        assertEquals(GamepadState.NEUTRAL, tracker.state, "fingers from before the cancel are forgotten")
    }
}
