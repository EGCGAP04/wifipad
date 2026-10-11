package io.github.egcgap04.wifipad.server

import io.github.egcgap04.wifipad.protocol.GamepadState

// Mapping from docs/protocol.md to evdev codes, as the xpad driver exposes an Xbox 360 pad.

internal class ButtonMapping(val button: Int, val code: Int)

internal class AxisMapping(val code: Int, val min: Int, val max: Int, val value: (GamepadState) -> Int)

/** Every button except the D-pad, which goes to the hat axes. */
internal val BUTTONS = listOf(
    ButtonMapping(GamepadState.A, BTN_A),
    ButtonMapping(GamepadState.B, BTN_B),
    ButtonMapping(GamepadState.X, BTN_X),
    ButtonMapping(GamepadState.Y, BTN_Y),
    ButtonMapping(GamepadState.LB, BTN_TL),
    ButtonMapping(GamepadState.RB, BTN_TR),
    ButtonMapping(GamepadState.BACK, BTN_SELECT),
    ButtonMapping(GamepadState.START, BTN_START),
    ButtonMapping(GamepadState.GUIDE, BTN_MODE),
    ButtonMapping(GamepadState.L3, BTN_THUMBL),
    ButtonMapping(GamepadState.R3, BTN_THUMBR),
)

internal val AXES = listOf(
    AxisMapping(ABS_X, GamepadState.AXIS_MIN, GamepadState.AXIS_MAX) { it.lx },
    AxisMapping(ABS_Y, GamepadState.AXIS_MIN, GamepadState.AXIS_MAX) { it.ly },
    AxisMapping(ABS_RX, GamepadState.AXIS_MIN, GamepadState.AXIS_MAX) { it.rx },
    AxisMapping(ABS_RY, GamepadState.AXIS_MIN, GamepadState.AXIS_MAX) { it.ry },
    AxisMapping(ABS_Z, 0, GamepadState.TRIGGER_MAX) { it.lt },
    AxisMapping(ABS_RZ, 0, GamepadState.TRIGGER_MAX) { it.rt },
    AxisMapping(ABS_HAT0X, -1, 1) { hat(it, GamepadState.DPAD_LEFT, GamepadState.DPAD_RIGHT) },
    AxisMapping(ABS_HAT0Y, -1, 1) { hat(it, GamepadState.DPAD_UP, GamepadState.DPAD_DOWN) },
)

/** -1, 0 or 1. Opposite directions pressed together cancel out. */
private fun hat(state: GamepadState, negative: Int, positive: Int): Int =
    (if (state.isPressed(positive)) 1 else 0) - (if (state.isPressed(negative)) 1 else 0)

/**
 * Calls [emit] with the evdev events (`type`, `code`, `value`) that take the device from [old] to
 * [new]: only what changed, without `SYN_REPORT`. Inline so the server loop does not allocate.
 */
internal inline fun forEachChange(
    old: GamepadState,
    new: GamepadState,
    emit: (type: Int, code: Int, value: Int) -> Unit,
) {
    for (mapping in BUTTONS) {
        val pressed = new.isPressed(mapping.button)
        if (pressed != old.isPressed(mapping.button)) emit(EV_KEY, mapping.code, if (pressed) 1 else 0)
    }
    for (axis in AXES) {
        val value = axis.value(new)
        if (value != axis.value(old)) emit(EV_ABS, axis.code, value)
    }
}
