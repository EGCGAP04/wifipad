package io.github.egcgap04.wifipad.app

import io.github.egcgap04.wifipad.protocol.GamepadState

/**
 * Whether going from [previous] to this state presses something new: a button, L3/R3, a trigger or
 * a D-pad direction. That is when the phone vibrates. Releasing, moving a stick, or sliding the
 * D-pad from a diagonal back to one of its directions (up-right to up) do not count.
 */
fun GamepadState.hasNewPress(previous: GamepadState): Boolean =
    buttons and previous.buttons.inv() != 0 ||
        (lt > 0 && previous.lt == 0) ||
        (rt > 0 && previous.rt == 0)
