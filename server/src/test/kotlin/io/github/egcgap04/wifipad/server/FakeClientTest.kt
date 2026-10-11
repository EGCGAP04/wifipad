package io.github.egcgap04.wifipad.server

import io.github.egcgap04.wifipad.protocol.GamepadState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FakeClientTest {
    private val ms = 1_000_000L

    @Test
    fun `starts with A, sticks to the right and the left trigger released`() {
        assertEquals(GamepadState(GamepadState.A, 32767, 0, 32767, 0, 0, 255), fakeState(0))
    }

    @Test
    fun `the left stick points down after a quarter turn`() {
        val state = fakeState(500 * ms)
        assertEquals(0, state.lx)
        assertEquals(32767, state.ly)
    }

    @Test
    fun `the triggers peak in opposite phase`() {
        val state = fakeState(1000 * ms)
        assertEquals(255, state.lt)
        assertEquals(0, state.rt)
    }

    @Test
    fun `buttons take turns through every bit and wrap around`() {
        val pressed = (0 until 15).map { fakeState(it * 500 * ms + 1).buttons }
        assertEquals((0..14).map { 1 shl it }, pressed)
        assertEquals(GamepadState.A, fakeState(7500 * ms).buttons)
    }

    @Test
    fun `stays in range for a whole cycle`() {
        // GamepadState throws if a value is out of range.
        for (t in 0L..10_000L) fakeState(t * ms)
    }

    @Test
    fun `parses host and port`() {
        assertEquals(FakeClientOptions(), parseFakeClientArgs(emptyArray()))
        assertEquals(
            FakeClientOptions(host = "192.168.0.5", port = 9000),
            parseFakeClientArgs(arrayOf("--host", "192.168.0.5", "--port", "9000")),
        )
        assertEquals(true, parseFakeClientArgs(arrayOf("-h")).help)
        for (args in listOf(arrayOf("--host"), arrayOf("--port", "0"), arrayOf("--foo"))) {
            assertFailsWith<IllegalArgumentException>(args.joinToString(" ")) { parseFakeClientArgs(args) }
        }
    }
}
