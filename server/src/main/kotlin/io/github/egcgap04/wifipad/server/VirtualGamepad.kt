package io.github.egcgap04.wifipad.server

import com.sun.jna.Pointer
import com.sun.jna.ptr.PointerByReference
import io.github.egcgap04.wifipad.protocol.GamepadState

/**
 * Virtual gamepad on uinput that games see as an Xbox 360 pad (`docs/protocol.md`).
 *
 * Pass full states to [update]: it emits only what changed. Not thread-safe. Closing it destroys
 * the device, and the kernel releases anything still pressed.
 */
class VirtualGamepad private constructor(private val uinput: Pointer, val devnode: String?) : AutoCloseable {
    private var state = GamepadState.NEUTRAL
    private var closed = false

    fun update(new: GamepadState) {
        check(!closed) { "VirtualGamepad is closed" }
        var changed = false
        forEachChange(state, new) { type, code, value ->
            write(type, code, value)
            changed = true
        }
        if (changed) write(EV_SYN, SYN_REPORT, 0)
        state = new
    }

    override fun close() {
        if (closed) return
        closed = true
        Evdev.lib.libevdev_uinput_destroy(uinput)
    }

    private fun write(type: Int, code: Int, value: Int) {
        val rc = Evdev.lib.libevdev_uinput_write_event(uinput, type, code, value)
        check(rc >= 0) { "Could not write to the uinput device (errno ${-rc})" }
    }

    companion object {
        const val NAME = "wifipad"

        // Xbox 360 pad IDs, as xpad reports them, so SDL and Steam map it without configuration.
        private const val VENDOR = 0x045e
        private const val PRODUCT = 0x028e
        private const val VERSION = 0x0110

        private const val EPERM = 1
        private const val ENOENT = 2
        private const val EACCES = 13
        private const val ENODEV = 19

        /** Creates the device. Its initial state is [GamepadState.NEUTRAL]. */
        fun create(): VirtualGamepad {
            val lib = Evdev.lib
            val dev = checkNotNull(lib.libevdev_new()) { "libevdev_new failed" }
            try {
                lib.libevdev_set_name(dev, NAME)
                lib.libevdev_set_id_bustype(dev, BUS_USB)
                lib.libevdev_set_id_vendor(dev, VENDOR)
                lib.libevdev_set_id_product(dev, PRODUCT)
                lib.libevdev_set_id_version(dev, VERSION)
                for (button in BUTTONS) enable(dev, EV_KEY, button.code, null)
                // fuzz and flat stay at 0: each game applies its own dead zone.
                for (axis in AXES) enable(dev, EV_ABS, axis.code, InputAbsinfo(axis.min, axis.max))

                val uinput = PointerByReference()
                val rc = lib.libevdev_uinput_create_from_device(dev, Evdev.LIBEVDEV_UINPUT_OPEN_MANAGED, uinput)
                if (rc < 0) throw IllegalStateException(createError(-rc))
                return VirtualGamepad(uinput.value, lib.libevdev_uinput_get_devnode(uinput.value))
            } finally {
                lib.libevdev_free(dev)
            }
        }

        private fun enable(dev: Pointer, type: Int, code: Int, absinfo: InputAbsinfo?) {
            val rc = Evdev.lib.libevdev_enable_event_code(dev, type, code, absinfo)
            check(rc == 0) { "Could not enable event type $type, code $code" }
        }

        private fun createError(errno: Int): String = when (errno) {
            EACCES, EPERM ->
                "No permission to use /dev/uinput. Install the udev rule (see README) instead of using sudo."
            ENOENT, ENODEV -> "/dev/uinput not found. Load the module with: sudo modprobe uinput"
            else -> "Could not create the uinput device (errno $errno)"
        }
    }
}
