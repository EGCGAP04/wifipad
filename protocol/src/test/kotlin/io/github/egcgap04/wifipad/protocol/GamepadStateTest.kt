package io.github.egcgap04.wifipad.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GamepadStateTest {
    @Test
    fun `button bits match the protocol table`() {
        val bits = listOf(
            GamepadState.A, GamepadState.B, GamepadState.X, GamepadState.Y,
            GamepadState.LB, GamepadState.RB, GamepadState.BACK, GamepadState.START,
            GamepadState.GUIDE, GamepadState.L3, GamepadState.R3, GamepadState.DPAD_UP,
            GamepadState.DPAD_DOWN, GamepadState.DPAD_LEFT, GamepadState.DPAD_RIGHT,
        )
        assertEquals((0..14).map { 1 shl it }, bits)
    }

    @Test
    fun `neutral state has everything released and centered`() {
        assertEquals(GamepadState(0, 0, 0, 0, 0, 0, 0), GamepadState.NEUTRAL)
    }

    @Test
    fun `isPressed checks individual buttons`() {
        val state = GamepadState(buttons = GamepadState.A or GamepadState.DPAD_LEFT)
        assertTrue(state.isPressed(GamepadState.A))
        assertTrue(state.isPressed(GamepadState.DPAD_LEFT))
        assertFalse(state.isPressed(GamepadState.B))
    }

    @Test
    fun `rejects values out of range`() {
        assertFailsWith<IllegalArgumentException> { GamepadState(buttons = -1) }
        assertFailsWith<IllegalArgumentException> { GamepadState(buttons = 0x10000) }
        assertFailsWith<IllegalArgumentException> { GamepadState(lx = 32768) }
        assertFailsWith<IllegalArgumentException> { GamepadState(ly = -32769) }
        assertFailsWith<IllegalArgumentException> { GamepadState(rx = 40000) }
        assertFailsWith<IllegalArgumentException> { GamepadState(ry = -40000) }
        assertFailsWith<IllegalArgumentException> { GamepadState(lt = -1) }
        assertFailsWith<IllegalArgumentException> { GamepadState(rt = 256) }
    }
}
