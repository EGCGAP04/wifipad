package io.github.egcgap04.wifipad.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import io.github.egcgap04.wifipad.protocol.GamepadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Draws the controls and turns touches into a [GamepadState]. The logic lives in [GamepadLayout]
 * and [TouchTracker]; this class only draws and forwards `MotionEvent`s.
 */
class GamepadView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    View(context, attrs) {

    private val mutableState = MutableStateFlow(GamepadState.NEUTRAL)

    /** The current state, updated on every touch that changes it. */
    val state: StateFlow<GamepadState> = mutableState.asStateFlow()

    private var tracker: TouchTracker? = null

    private val background = context.getColor(R.color.gamepad_background)
    private val density = resources.displayMetrics.density
    private val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3 * density
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    /** The outline of the D-pad cross, built with the layout instead of on every frame. */
    private val dpadOutline = Path()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        // Fingers on the old layout do not match the new one: start over.
        val layout = GamepadLayout(w.toFloat(), h.toFloat())
        tracker = TouchTracker(layout)
        buildCross(layout.dpad)
        publish()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val tracker = tracker ?: return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = event.actionIndex
                tracker.down(event.getPointerId(i), event.getX(i), event.getY(i), event.eventTime)
                if (event.actionMasked == MotionEvent.ACTION_DOWN) performClick()
            }
            // A move event carries every pointer, not just the one that moved.
            MotionEvent.ACTION_MOVE -> for (i in 0 until event.pointerCount) {
                tracker.move(event.getPointerId(i), event.getX(i), event.getY(i))
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP ->
                tracker.up(event.getPointerId(event.actionIndex))
            MotionEvent.ACTION_CANCEL -> tracker.cancel()
        }
        publish()
        return true
    }

    // Touches are not clicks, but accessibility services expect performClick() on ACTION_DOWN.
    override fun performClick(): Boolean = super.performClick()

    private fun publish() {
        val new = tracker?.state ?: GamepadState.NEUTRAL
        if (new != mutableState.value) {
            mutableState.value = new
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(background)
        val layout = tracker?.layout ?: return
        val state = mutableState.value
        drawStick(canvas, layout.leftStick, state.lx, state.ly, state.isPressed(GamepadState.L3))
        drawStick(canvas, layout.rightStick, state.rx, state.ry, state.isPressed(GamepadState.R3))
        drawDPad(canvas, layout.dpad, state)
        for (button in layout.buttons) {
            drawRound(canvas, button, button.label, colorOf(button.button), state.isPressed(button.button))
        }
        for (trigger in layout.triggers) {
            val pressed = (if (trigger.isLeft) state.lt else state.rt) > 0
            drawRound(canvas, trigger, trigger.label, NEUTRAL_COLOR, pressed)
        }
    }

    private fun drawStick(canvas: Canvas, stick: Stick, axisX: Int, axisY: Int, clicked: Boolean) {
        if (clicked) {
            fill.color = CLICKED_STICK_COLOR
            canvas.drawCircle(stick.x, stick.y, stick.radius, fill)
        }
        outline.color = NEUTRAL_COLOR
        canvas.drawCircle(stick.x, stick.y, stick.radius, outline)
        // The knob center travels within the base circle, like the axes within the unit circle.
        val knobX = stick.x + axisX.toFloat() / GamepadState.AXIS_MAX * stick.radius
        val knobY = stick.y + axisY.toFloat() / GamepadState.AXIS_MAX * stick.radius
        fill.color = NEUTRAL_COLOR
        canvas.drawCircle(knobX, knobY, stick.radius * 0.4f, fill)
    }

    /**
     * A plus sign that fits in the D-pad circle: each arm is [ARM_WIDTH] of the radius wide and
     * reaches [ARM_LENGTH] of it from the center.
     */
    private fun buildCross(dpad: DPad) {
        val a = dpad.radius * ARM_WIDTH / 2
        val l = dpad.radius * ARM_LENGTH
        dpadOutline.reset()
        // (x, y) pairs, clockwise from the top left corner of the up arm.
        val corners = floatArrayOf(
            -a, -l, a, -l, a, -a, // up
            l, -a, l, a, a, a, // right
            a, l, -a, l, -a, a, // down
            -l, a, -l, -a, -a, -a, // left
        )
        dpadOutline.moveTo(dpad.x + corners[0], dpad.y + corners[1])
        for (i in 2 until corners.size step 2) {
            dpadOutline.lineTo(dpad.x + corners[i], dpad.y + corners[i + 1])
        }
        dpadOutline.close()
    }

    private fun drawDPad(canvas: Canvas, dpad: DPad, state: GamepadState) {
        val a = dpad.radius * ARM_WIDTH / 2
        val l = dpad.radius * ARM_LENGTH
        val x = dpad.x
        val y = dpad.y
        fill.color = NEUTRAL_COLOR
        if (state.isPressed(GamepadState.DPAD_UP)) canvas.drawRect(x - a, y - l, x + a, y - a, fill)
        if (state.isPressed(GamepadState.DPAD_DOWN)) canvas.drawRect(x - a, y + a, x + a, y + l, fill)
        if (state.isPressed(GamepadState.DPAD_LEFT)) canvas.drawRect(x - l, y - a, x - a, y + a, fill)
        if (state.isPressed(GamepadState.DPAD_RIGHT)) canvas.drawRect(x + a, y - a, x + l, y + a, fill)
        outline.color = NEUTRAL_COLOR
        canvas.drawPath(dpadOutline, outline)
    }

    /** A button or a trigger: a circle with a label, filled while pressed. */
    private fun drawRound(canvas: Canvas, control: Control, label: String, color: Int, pressed: Boolean) {
        if (pressed) {
            fill.color = color
            canvas.drawCircle(control.x, control.y, control.radius, fill)
        }
        outline.color = color
        canvas.drawCircle(control.x, control.y, control.radius, outline)

        text.color = if (pressed) background else color
        // Two letters (LB, RT...) need a smaller font to fit.
        text.textSize = control.radius * if (label.length > 1) 0.75f else 1f
        // drawText takes the baseline: shift it so the text is centered vertically.
        val baseline = control.y - (text.ascent() + text.descent()) / 2
        canvas.drawText(label, control.x, baseline, text)
    }

    private companion object {
        val NEUTRAL_COLOR = Color.rgb(0x9e, 0x9e, 0x9e)
        val CLICKED_STICK_COLOR = Color.argb(0x60, 0x9e, 0x9e, 0x9e)

        // The corner of an arm is at (ARM_LENGTH, ARM_WIDTH / 2): √(0.95² + 0.3²) < 1, inside the circle.
        const val ARM_WIDTH = 0.6f
        const val ARM_LENGTH = 0.95f

        // Xbox colors.
        fun colorOf(button: Int) = when (button) {
            GamepadState.A -> Color.rgb(0x4c, 0xaf, 0x50)
            GamepadState.B -> Color.rgb(0xf4, 0x43, 0x36)
            GamepadState.X -> Color.rgb(0x21, 0x96, 0xf3)
            GamepadState.Y -> Color.rgb(0xff, 0xc1, 0x07)
            else -> NEUTRAL_COLOR
        }
    }
}
