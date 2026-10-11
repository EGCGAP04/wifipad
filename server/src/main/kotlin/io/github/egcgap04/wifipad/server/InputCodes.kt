package io.github.egcgap04.wifipad.server

// Constants from <linux/input.h> and <linux/input-event-codes.h>, named as in C.

internal const val BUS_USB = 0x03

internal const val EV_SYN = 0x00
internal const val EV_KEY = 0x01
internal const val EV_ABS = 0x03

internal const val SYN_REPORT = 0

internal const val BTN_A = 0x130
internal const val BTN_B = 0x131
internal const val BTN_X = 0x133
internal const val BTN_Y = 0x134
internal const val BTN_TL = 0x136
internal const val BTN_TR = 0x137
internal const val BTN_SELECT = 0x13a
internal const val BTN_START = 0x13b
internal const val BTN_MODE = 0x13c
internal const val BTN_THUMBL = 0x13d
internal const val BTN_THUMBR = 0x13e

internal const val ABS_X = 0x00
internal const val ABS_Y = 0x01
internal const val ABS_Z = 0x02
internal const val ABS_RX = 0x03
internal const val ABS_RY = 0x04
internal const val ABS_RZ = 0x05
internal const val ABS_HAT0X = 0x10
internal const val ABS_HAT0Y = 0x11
