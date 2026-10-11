package io.github.egcgap04.wifipad.server

import io.github.egcgap04.wifipad.protocol.GamepadState
import kotlin.test.Test
import kotlin.test.assertEquals

class GamepadEventsTest {
    private data class Event(val type: Int, val code: Int, val value: Int)

    private fun changes(old: GamepadState, new: GamepadState): List<Event> =
        buildList { forEachChange(old, new) { type, code, value -> add(Event(type, code, value)) } }

    private fun changesFromNeutral(new: GamepadState) = changes(GamepadState.NEUTRAL, new)

    @Test
    fun `no changes emit no events`() {
        assertEquals(emptyList(), changesFromNeutral(GamepadState.NEUTRAL))
        val state = GamepadState(GamepadState.A or GamepadState.DPAD_UP, 1, 2, 3, 4, 5, 6)
        assertEquals(emptyList(), changes(state, state))
    }

    @Test
    fun `buttons map to the xpad key codes`() {
        val expected = mapOf(
            GamepadState.A to BTN_A,
            GamepadState.B to BTN_B,
            GamepadState.X to BTN_X,
            GamepadState.Y to BTN_Y,
            GamepadState.LB to BTN_TL,
            GamepadState.RB to BTN_TR,
            GamepadState.BACK to BTN_SELECT,
            GamepadState.START to BTN_START,
            GamepadState.GUIDE to BTN_MODE,
            GamepadState.L3 to BTN_THUMBL,
            GamepadState.R3 to BTN_THUMBR,
        )
        for ((button, code) in expected) {
            val pressed = GamepadState(buttons = button)
            assertEquals(listOf(Event(EV_KEY, code, 1)), changesFromNeutral(pressed))
            assertEquals(listOf(Event(EV_KEY, code, 0)), changes(pressed, GamepadState.NEUTRAL))
        }
    }

    @Test
    fun `sticks and triggers map to the absolute axes`() {
        val state = GamepadState(lx = 32767, ly = -32768, rx = -1, ry = 1000, lt = 255, rt = 1)
        assertEquals(
            listOf(
                Event(EV_ABS, ABS_X, 32767),
                Event(EV_ABS, ABS_Y, -32768),
                Event(EV_ABS, ABS_RX, -1),
                Event(EV_ABS, ABS_RY, 1000),
                Event(EV_ABS, ABS_Z, 255),
                Event(EV_ABS, ABS_RZ, 1),
            ),
            changesFromNeutral(state),
        )
    }

    @Test
    fun `only changed values are emitted`() {
        val old = GamepadState(buttons = GamepadState.A or GamepadState.B, lx = 100, lt = 10)
        val new = GamepadState(buttons = GamepadState.A or GamepadState.X, lx = 100, lt = 20)
        assertEquals(
            listOf(Event(EV_KEY, BTN_B, 0), Event(EV_KEY, BTN_X, 1), Event(EV_ABS, ABS_Z, 20)),
            changes(old, new),
        )
    }

    @Test
    fun `D-pad maps to the hat axes with up and left negative`() {
        assertEquals(listOf(Event(EV_ABS, ABS_HAT0Y, -1)), changesFromNeutral(GamepadState(GamepadState.DPAD_UP)))
        assertEquals(listOf(Event(EV_ABS, ABS_HAT0Y, 1)), changesFromNeutral(GamepadState(GamepadState.DPAD_DOWN)))
        assertEquals(listOf(Event(EV_ABS, ABS_HAT0X, -1)), changesFromNeutral(GamepadState(GamepadState.DPAD_LEFT)))
        assertEquals(listOf(Event(EV_ABS, ABS_HAT0X, 1)), changesFromNeutral(GamepadState(GamepadState.DPAD_RIGHT)))
    }

    @Test
    fun `opposite D-pad directions cancel out`() {
        val upDown = GamepadState(GamepadState.DPAD_UP or GamepadState.DPAD_DOWN)
        val leftRight = GamepadState(GamepadState.DPAD_LEFT or GamepadState.DPAD_RIGHT)
        assertEquals(emptyList(), changesFromNeutral(upDown))
        assertEquals(emptyList(), changesFromNeutral(leftRight))
        assertEquals(listOf(Event(EV_ABS, ABS_HAT0Y, 0)), changes(GamepadState(GamepadState.DPAD_UP), upDown))
    }

    @Test
    fun `axis ranges match the protocol`() {
        val ranges = AXES.associate { it.code to (it.min..it.max) }
        val stick = -32768..32767
        assertEquals(
            mapOf(
                ABS_X to stick, ABS_Y to stick, ABS_RX to stick, ABS_RY to stick,
                ABS_Z to 0..255, ABS_RZ to 0..255, ABS_HAT0X to -1..1, ABS_HAT0Y to -1..1,
            ),
            ranges,
        )
    }

    @Test
    fun `input_absinfo has the kernel layout`() {
        assertEquals(6 * Int.SIZE_BYTES, InputAbsinfo().size())
    }
}
