package io.github.egcgap04.wifipad.protocol

/**
 * Full gamepad state, as defined in `docs/protocol.md`.
 *
 * Sticks use Linux conventions: right is positive X and **up is negative Y**.
 */
data class GamepadState(
    /** Button bit mask; combine the constants in the companion object. */
    val buttons: Int = 0,
    val lx: Int = 0,
    val ly: Int = 0,
    val rx: Int = 0,
    val ry: Int = 0,
    val lt: Int = 0,
    val rt: Int = 0,
) {
    init {
        require(buttons in 0..0xFFFF) { "buttons out of range: $buttons" }
        requireAxis(lx, "lx")
        requireAxis(ly, "ly")
        requireAxis(rx, "rx")
        requireAxis(ry, "ry")
        requireTrigger(lt, "lt")
        requireTrigger(rt, "rt")
    }

    fun isPressed(button: Int): Boolean = buttons and button != 0

    companion object {
        const val AXIS_MIN = Short.MIN_VALUE.toInt()
        const val AXIS_MAX = Short.MAX_VALUE.toInt()
        const val TRIGGER_MAX = 255

        const val A = 1 shl 0
        const val B = 1 shl 1
        const val X = 1 shl 2
        const val Y = 1 shl 3
        const val LB = 1 shl 4
        const val RB = 1 shl 5
        const val BACK = 1 shl 6
        const val START = 1 shl 7
        const val GUIDE = 1 shl 8
        const val L3 = 1 shl 9
        const val R3 = 1 shl 10
        const val DPAD_UP = 1 shl 11
        const val DPAD_DOWN = 1 shl 12
        const val DPAD_LEFT = 1 shl 13
        const val DPAD_RIGHT = 1 shl 14

        /** Nothing pressed, sticks centered, triggers released. */
        val NEUTRAL = GamepadState()

        private fun requireAxis(value: Int, name: String) =
            require(value in AXIS_MIN..AXIS_MAX) { "$name out of range: $value" }

        private fun requireTrigger(value: Int, name: String) =
            require(value in 0..TRIGGER_MAX) { "$name out of range: $value" }
    }
}
