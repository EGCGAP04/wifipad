package io.github.egcgap04.wifipad.app

import io.github.egcgap04.wifipad.protocol.GamepadState
import io.github.egcgap04.wifipad.protocol.GamepadState.Companion.A
import io.github.egcgap04.wifipad.protocol.GamepadState.Companion.B
import io.github.egcgap04.wifipad.protocol.GamepadState.Companion.DPAD_RIGHT
import io.github.egcgap04.wifipad.protocol.GamepadState.Companion.DPAD_UP
import io.github.egcgap04.wifipad.protocol.GamepadState.Companion.L3
import io.github.egcgap04.wifipad.protocol.GamepadState.Companion.TRIGGER_MAX
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HapticsTest {
    private val neutral = GamepadState.NEUTRAL

    @Test
    fun `pressing a button or L3 is a new press`() {
        assertTrue(GamepadState(buttons = A).hasNewPress(neutral))
        assertTrue(GamepadState(buttons = A or B).hasNewPress(GamepadState(buttons = A)), "a second button")
        assertTrue(GamepadState(buttons = L3, lx = 100).hasNewPress(GamepadState(lx = 100)))
    }

    @Test
    fun `releasing is not a new press`() {
        assertFalse(neutral.hasNewPress(GamepadState(buttons = A)))
        assertFalse(GamepadState(buttons = A).hasNewPress(GamepadState(buttons = A or B)))
        assertFalse(neutral.hasNewPress(GamepadState(lt = TRIGGER_MAX)))
    }

    @Test
    fun `nothing changing is not a new press`() {
        val held = GamepadState(buttons = A, rt = TRIGGER_MAX)
        assertFalse(held.hasNewPress(held))
    }

    @Test
    fun `moving a stick is not a new press`() {
        assertFalse(GamepadState(lx = 32767, ry = -500).hasNewPress(neutral))
        assertFalse(GamepadState(buttons = A, rx = 2000).hasNewPress(GamepadState(buttons = A, rx = 1000)))
    }

    @Test
    fun `pressing a trigger is a new press`() {
        assertTrue(GamepadState(lt = TRIGGER_MAX).hasNewPress(neutral))
        assertTrue(GamepadState(lt = TRIGGER_MAX, rt = TRIGGER_MAX).hasNewPress(GamepadState(lt = TRIGGER_MAX)))
    }

    @Test
    fun `the D-pad vibrates when it gains a direction`() {
        val up = GamepadState(buttons = DPAD_UP)
        val upRight = GamepadState(buttons = DPAD_UP or DPAD_RIGHT)
        val right = GamepadState(buttons = DPAD_RIGHT)
        assertTrue(up.hasNewPress(neutral))
        assertTrue(upRight.hasNewPress(up), "up to up-right")
        assertTrue(up.hasNewPress(right), "right to up")
        assertFalse(up.hasNewPress(upRight), "back from the diagonal")
    }
}
