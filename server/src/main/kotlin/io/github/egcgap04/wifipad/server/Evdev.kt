package io.github.egcgap04.wifipad.server

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.ptr.PointerByReference

/**
 * Minimal JNA bindings for libevdev: only the functions wifipad uses.
 *
 * libevdev builds `uinput_setup` and `input_event` itself, so the only struct mapped here is
 * `input_absinfo`. Functions that can fail return a negative errno.
 */
@Suppress("FunctionName")
internal interface Evdev : Library {
    fun libevdev_new(): Pointer?
    fun libevdev_free(dev: Pointer)

    /** Copies [name]. */
    fun libevdev_set_name(dev: Pointer, name: String)
    fun libevdev_set_id_bustype(dev: Pointer, bustype: Int)
    fun libevdev_set_id_vendor(dev: Pointer, vendor: Int)
    fun libevdev_set_id_product(dev: Pointer, product: Int)
    fun libevdev_set_id_version(dev: Pointer, version: Int)

    /** Also enables [type]. [data] is the `input_absinfo` for `EV_ABS` and `null` for `EV_KEY`. */
    fun libevdev_enable_event_code(dev: Pointer, type: Int, code: Int, data: InputAbsinfo?): Int

    /** Copies the setup of [dev], which can be freed afterwards. */
    fun libevdev_uinput_create_from_device(dev: Pointer, uinputFd: Int, uinputDev: PointerByReference): Int
    fun libevdev_uinput_destroy(uinputDev: Pointer)
    fun libevdev_uinput_get_devnode(uinputDev: Pointer): String?
    fun libevdev_uinput_write_event(uinputDev: Pointer, type: Int, code: Int, value: Int): Int

    companion object {
        /** libevdev opens `/dev/uinput` and closes it on destroy. */
        const val LIBEVDEV_UINPUT_OPEN_MANAGED = -2

        val lib: Evdev by lazy { load() }

        // "evdev" resolves to libevdev.so, which only the -dev package installs, so fall back to
        // the versioned name from libevdev2.
        private fun load(): Evdev = try {
            Native.load("evdev", Evdev::class.java)
        } catch (e: UnsatisfiedLinkError) {
            try {
                Native.load("libevdev.so.2", Evdev::class.java)
            } catch (e2: UnsatisfiedLinkError) {
                e2.addSuppressed(e)
                throw IllegalStateException("libevdev not found. Install it with: sudo apt install libevdev2", e2)
            }
        }
    }
}

/** `struct input_absinfo` from <linux/input.h>: six `__s32`. */
@Structure.FieldOrder("value", "minimum", "maximum", "fuzz", "flat", "resolution")
internal class InputAbsinfo(minimum: Int = 0, maximum: Int = 0) : Structure() {
    @JvmField var value: Int = 0
    @JvmField var minimum: Int = minimum
    @JvmField var maximum: Int = maximum
    @JvmField var fuzz: Int = 0
    @JvmField var flat: Int = 0
    @JvmField var resolution: Int = 0
}
