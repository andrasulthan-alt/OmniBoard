package io.github.andrasulthan.omniboard

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import kotlin.math.abs

object Codes {
    const val TEXT = 0
    const val SHIFT = -1
    const val DELETE = -2
    const val ENTER = -3
    const val SPACE = -4
    const val SYMBOLS = -5
    const val LETTERS = -6
    const val EMOJI = -7
}

enum class Shift { OFF, ONCE, LOCKED }

enum class EnterIcon { RETURN, SEARCH, GO, SEND, NEXT, PREVIOUS, DONE }

class Key(
    val label: String,
    val code: Int,
    val text: String = label,
    val weight: Float = 1f,
    val longPress: String? = null,
    val repeatable: Boolean = false,
) {
    val rect = RectF()
}

object Layouts {
    private fun row(chars: String, longs: String?): List<Key> =
        chars.mapIndexed { i, c ->
            Key(c.toString(), Codes.TEXT, c.toString(), 1f, longs?.getOrNull(i)?.toString())
        }

    private fun delete() = Key("del", Codes.DELETE, weight = 1.5f, repeatable = true)

    private fun bottom(label: String, code: Int): List<Key> = listOf(
        Key(label, code, weight = 1.5f),
        Key("emoji", Codes.EMOJI),
        Key(",", Codes.TEXT),
        Key("space", Codes.SPACE, text = " ", weight = 4f),
        Key(".", Codes.TEXT, longPress = "?"),
        Key("enter", Codes.ENTER, weight = 1.5f),
    )

    fun letters(): List<List<Key>> = listOf(
        row("qwertyuiop", "1234567890"),
        row("asdfghjkl", "@#\$%&-+()"),
        listOf(Key("shift", Codes.SHIFT, weight = 1.5f)) +
            row("zxcvbnm", "*\"':;!?") + delete(),
        bottom("?123", Codes.SYMBOLS),
    )

    fun symbols(): List<List<Key>> = listOf(
        row("1234567890", null),
        row("@#\$%&-+()/", null),
        listOf(Key("_", Codes.TEXT, weight = 1.5f)) + row("*\"':;!?", null) + delete(),
        bottom("abc", Codes.LETTERS),
    )
}

class KeyboardView(context: Context) : View(context) {

    interface Listener {
        fun onKey(key: Key)
        fun onLongPress(key: Key): Boolean
        /** Space-bar trackpad: move the text cursor one step (DPAD key code). */
        fun onCursorStep(keyCode: Int) {}
    }

    var listener: Listener? = null
    var hapticsEnabled = true

    var shift = Shift.OFF
        set(value) { field = value; invalidate() }

    var incognito = false
        set(value) { field = value; invalidate() }

    var isSymbols = false
        private set

    private val dm = resources.displayMetrics
    private fun dp(v: Float) = v * dm.density
    private fun sp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, dm)

    private val rowHeight = dp(54f)
    private val gap = dp(5f)
    private val radius = dp(12f)

    private var rows: List<List<Key>> = Layouts.letters()
    private var enterIcon = EnterIcon.RETURN
    private var palette = Palette.DARK

    private val dotFont = DotFont.get(context)
    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = dotFont
    }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.RIGHT
        textSize = sp(10f)
        typeface = dotFont
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.8f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val arcRect = RectF()

    private val timer = Handler(Looper.getMainLooper())
    private var downKey: Key? = null
    private var activeId = -1
    private var longPressFired = false

    // Finger positions, used by the space-bar trackpad.
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f

    // Space-bar trackpad state.
    private var trackpad = false
    private var trackX = 0f
    private var trackY = 0f

    // Labels fade out while the trackpad is active and fade back in afterwards.
    private var labelAlpha = 1f
    private var fadeAnim: ValueAnimator? = null

    private val repeatRunnable = object : Runnable {
        override fun run() {
            val k = downKey ?: return
            listener?.onKey(k)
            timer.postDelayed(this, 50)
        }
    }

    private val longPressRunnable = Runnable {
        val k = downKey ?: return@Runnable
        if (k.code == Codes.SPACE) {
            enterTrackpad(lastX, lastY)
            return@Runnable
        }
        if (listener?.onLongPress(k) == true) {
            longPressFired = true
            if (hapticsEnabled) performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            invalidate()
        }
    }

    init {
        // Keep keys above the gesture / navigation bar.
        setOnApplyWindowInsetsListener { _, insets ->
            val bottom = if (Build.VERSION.SDK_INT >= 30) {
                insets.getInsets(WindowInsets.Type.navigationBars()).bottom
            } else {
                @Suppress("DEPRECATION")
                insets.systemWindowInsetBottom
            }
            if (paddingBottom != bottom) {
                setPadding(0, 0, 0, bottom)
                requestLayout()
            }
            insets
        }
    }

    fun showLetters() { rows = Layouts.letters(); isSymbols = false; layoutKeys(); invalidate() }
    fun showSymbols() { rows = Layouts.symbols(); isSymbols = true; layoutKeys(); invalidate() }
    fun setEnterIcon(icon: EnterIcon) { enterIcon = icon; invalidate() }
    fun setDark(dark: Boolean) { palette = if (dark) Palette.DARK else Palette.LIGHT; invalidate() }

    fun reset() {
        timer.removeCallbacksAndMessages(null)
        fadeAnim?.cancel()
        labelAlpha = 1f
        downKey = null
        activeId = -1
        trackpad = false
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = (rows.size * rowHeight + gap).toInt() + paddingBottom
        setMeasuredDimension(w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        layoutKeys()
    }

    private fun layoutKeys() {
        if (width == 0) return
        val unit = width / 10f
        rows.forEachIndexed { r, row ->
            val total = row.fold(0f) { acc, k -> acc + k.weight }
            var x = (width - total * unit) / 2f
            val top = gap / 2f + r * rowHeight
            for (k in row) {
                val w = k.weight * unit
                k.rect.set(x + gap / 2f, top + gap / 2f, x + w - gap / 2f, top + rowHeight - gap / 2f)
                x += w
            }
        }
    }

    // ---------- drawing ----------

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(palette.background)
        for (row in rows) for (k in row) {
            val pill = k.code == Codes.SPACE || k.code == Codes.ENTER
            val r = if (pill) k.rect.height() / 2f else radius
            val isDown = k === downKey && !trackpad

            canvas.save()
            if (isDown) canvas.scale(0.94f, 0.94f, k.rect.centerX(), k.rect.centerY())

            // Keys stay visible as shapes; in trackpad mode they all turn the same grey.
            keyPaint.color = when {
                trackpad -> palette.key
                k.code == Codes.ENTER -> palette.accent
                isDown -> palette.pressedKey
                k.code == Codes.TEXT || k.code == Codes.SPACE -> palette.key
                else -> palette.special
            }
            canvas.drawRoundRect(k.rect, r, r, keyPaint)

            if (labelAlpha > 0.01f) {
                val fading = labelAlpha < 0.99f
                if (fading) canvas.saveLayerAlpha(k.rect, (labelAlpha * 255).toInt())

                when (k.code) {
                    Codes.SHIFT -> drawShift(canvas, k)
                    Codes.DELETE -> drawBackspace(canvas, k)
                    Codes.ENTER -> drawEnter(canvas, k)
                    Codes.SPACE -> drawSpace(canvas, k)
                    Codes.EMOJI -> drawEmojiKey(canvas, k)
                    else -> drawLabel(canvas, k)
                }
                k.longPress?.let {
                    hintPaint.color = palette.hint
                    canvas.drawText(it, k.rect.right - dp(6f), k.rect.top + dp(13f), hintPaint)
                }

                if (fading) canvas.restore()
            }
            canvas.restore()
        }
    }

    private fun drawLabel(canvas: Canvas, k: Key) {
        val label = if (k.code == Codes.TEXT && shift != Shift.OFF) k.label.uppercase() else k.label
        textPaint.color = palette.text
        textPaint.textSize = if (label.length > 1) sp(14f) else sp(22f)
        val y = k.rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(label, k.rect.centerX(), y, textPaint)
    }

    private fun iconSize(k: Key) = k.rect.height() * 0.42f

    private fun drawShift(canvas: Canvas, k: Key) {
        val cx = k.rect.centerX()
        val cy = k.rect.centerY()
        val s = iconSize(k)
        path.reset()
        path.moveTo(cx, cy - s / 2f)
        path.lineTo(cx + s / 2f, cy)
        path.lineTo(cx + s / 4f, cy)
        path.lineTo(cx + s / 4f, cy + s / 2f)
        path.lineTo(cx - s / 4f, cy + s / 2f)
        path.lineTo(cx - s / 4f, cy)
        path.lineTo(cx - s / 2f, cy)
        path.close()
        iconPaint.color = palette.text
        iconPaint.style = if (shift == Shift.OFF) Paint.Style.STROKE else Paint.Style.FILL_AND_STROKE
        canvas.drawPath(path, iconPaint)
        iconPaint.style = Paint.Style.STROKE
        if (shift == Shift.LOCKED) {
            dotPaint.color = palette.accent
            dotPaint.style = Paint.Style.FILL
            canvas.drawCircle(cx, cy + s / 2f + dp(6f), dp(2.5f), dotPaint)
        }
    }

    private fun drawBackspace(canvas: Canvas, k: Key) {
        val cx = k.rect.centerX()
        val cy = k.rect.centerY()
        val s = iconSize(k)
        path.reset()
        path.moveTo(cx - s * 0.6f, cy)
        path.lineTo(cx - s * 0.25f, cy - s * 0.4f)
        path.lineTo(cx + s * 0.6f, cy - s * 0.4f)
        path.lineTo(cx + s * 0.6f, cy + s * 0.4f)
        path.lineTo(cx - s * 0.25f, cy + s * 0.4f)
        path.close()
        path.moveTo(cx - s * 0.02f, cy - s * 0.15f)
        path.lineTo(cx + s * 0.3f, cy + s * 0.15f)
        path.moveTo(cx + s * 0.3f, cy - s * 0.15f)
        path.lineTo(cx - s * 0.02f, cy + s * 0.15f)
        iconPaint.color = palette.text
        canvas.drawPath(path, iconPaint)
    }

    private fun drawEmojiKey(canvas: Canvas, k: Key) {
        val cx = k.rect.centerX()
        val cy = k.rect.centerY()
        val s = iconSize(k)
        iconPaint.color = palette.text
        canvas.drawCircle(cx, cy, s * 0.5f, iconPaint)
        dotPaint.color = palette.text
        dotPaint.style = Paint.Style.FILL
        canvas.drawCircle(cx - s * 0.17f, cy - s * 0.1f, s * 0.06f, dotPaint)
        canvas.drawCircle(cx + s * 0.17f, cy - s * 0.1f, s * 0.06f, dotPaint)
        arcRect.set(cx - s * 0.25f, cy - s * 0.18f, cx + s * 0.25f, cy + s * 0.28f)
        canvas.drawArc(arcRect, 20f, 140f, false, iconPaint)
    }

    private fun drawEnter(canvas: Canvas, k: Key) {
        val cx = k.rect.centerX()
        val cy = k.rect.centerY()
        val s = iconSize(k)
        iconPaint.color = Color.WHITE
        path.reset()
        when (enterIcon) {
            EnterIcon.SEARCH -> {
                canvas.drawCircle(cx - s * 0.1f, cy - s * 0.1f, s * 0.3f, iconPaint)
                path.moveTo(cx + s * 0.12f, cy + s * 0.12f)
                path.lineTo(cx + s * 0.42f, cy + s * 0.42f)
            }
            EnterIcon.GO, EnterIcon.SEND, EnterIcon.NEXT -> {
                path.moveTo(cx - s * 0.45f, cy)
                path.lineTo(cx + s * 0.45f, cy)
                path.moveTo(cx + s * 0.15f, cy - s * 0.3f)
                path.lineTo(cx + s * 0.45f, cy)
                path.lineTo(cx + s * 0.15f, cy + s * 0.3f)
            }
            EnterIcon.PREVIOUS -> {
                path.moveTo(cx + s * 0.45f, cy)
                path.lineTo(cx - s * 0.45f, cy)
                path.moveTo(cx - s * 0.15f, cy - s * 0.3f)
                path.lineTo(cx - s * 0.45f, cy)
                path.lineTo(cx - s * 0.15f, cy + s * 0.3f)
            }
            EnterIcon.DONE -> {
                path.moveTo(cx - s * 0.4f, cy)
                path.lineTo(cx - s * 0.1f, cy + s * 0.3f)
                path.lineTo(cx + s * 0.45f, cy - s * 0.3f)
            }
            EnterIcon.RETURN -> {
                path.moveTo(cx + s * 0.45f, cy - s * 0.4f)
                path.lineTo(cx + s * 0.45f, cy + s * 0.1f)
                path.lineTo(cx - s * 0.45f, cy + s * 0.1f)
                path.moveTo(cx - s * 0.2f, cy - s * 0.15f)
                path.lineTo(cx - s * 0.45f, cy + s * 0.1f)
                path.lineTo(cx - s * 0.2f, cy + s * 0.35f)
            }
        }
        canvas.drawPath(path, iconPaint)
    }

    private fun drawSpace(canvas: Canvas, k: Key) {
        val label = "omniboard"
        textPaint.color = palette.hint
        textPaint.textSize = sp(11f)
        val textW = textPaint.measureText(label)
        val r = dp(3.5f)
        val between = dp(7f)
        val total = 2 * r + between + textW
        val startX = k.rect.centerX() - total / 2f
        val cy = k.rect.centerY()

        dotPaint.color = palette.accent
        dotPaint.style = if (incognito) Paint.Style.STROKE else Paint.Style.FILL
        dotPaint.strokeWidth = dp(1.5f)
        canvas.drawCircle(startX + r, cy, r, dotPaint)

        val y = cy - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(label, startX + 2 * r + between + textW / 2f, y, textPaint)
    }

    // ---------- space-bar trackpad ----------

    private fun fadeLabels(to: Float) {
        fadeAnim?.cancel()
        fadeAnim = ValueAnimator.ofFloat(labelAlpha, to).apply {
            duration = 160
            addUpdateListener {
                labelAlpha = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun enterTrackpad(x: Float, y: Float) {
        timer.removeCallbacks(longPressRunnable)
        trackpad = true
        longPressFired = true
        trackX = x
        trackY = y
        if (hapticsEnabled) performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        fadeLabels(0f)
        invalidate()
    }

    private fun exitTrackpad() {
        trackpad = false
        fadeLabels(1f)
        invalidate()
    }

    private fun moveTrackpad(x: Float, y: Float) {
        val stepX = dp(9f)
        val stepY = dp(24f)
        while (x - trackX >= stepX) { trackX += stepX; cursorStep(KeyEvent.KEYCODE_DPAD_RIGHT) }
        while (trackX - x >= stepX) { trackX -= stepX; cursorStep(KeyEvent.KEYCODE_DPAD_LEFT) }
        while (y - trackY >= stepY) { trackY += stepY; cursorStep(KeyEvent.KEYCODE_DPAD_DOWN) }
        while (trackY - y >= stepY) { trackY -= stepY; cursorStep(KeyEvent.KEYCODE_DPAD_UP) }
    }

    private fun cursorStep(keyCode: Int) {
        listener?.onCursorStep(keyCode)
        if (hapticsEnabled) performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    // ---------- touch ----------

    private fun findKey(x: Float, y: Float): Key? {
        var best: Key? = null
        var bestD = Float.MAX_VALUE
        for (row in rows) for (k in row) {
            val dx = when {
                x < k.rect.left -> k.rect.left - x
                x > k.rect.right -> x - k.rect.right
                else -> 0f
            }
            val dy = when {
                y < k.rect.top -> k.rect.top - y
                y > k.rect.bottom -> y - k.rect.bottom
                else -> 0f
            }
            val d = dx * dx + dy * dy
            if (d < bestD) { bestD = d; best = k }
        }
        return best
    }

    private fun press(id: Int, x: Float, y: Float) {
        val k = findKey(x, y) ?: return
        downKey = k
        activeId = id
        longPressFired = false
        downX = x
        downY = y
        lastX = x
        lastY = y
        if (hapticsEnabled) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        if (k.repeatable) {
            listener?.onKey(k)
            timer.postDelayed(repeatRunnable, 400)
        } else {
            timer.postDelayed(longPressRunnable, 350)
        }
        invalidate()
    }

    private fun slide(x: Float, y: Float) {
        lastX = x
        lastY = y
        if (trackpad) {
            moveTrackpad(x, y)
            return
        }
        val current = downKey ?: return
        // Swipe sideways on the space bar: start the trackpad right away.
        if (current.code == Codes.SPACE && !longPressFired && abs(x - downX) > dp(16f)) {
            enterTrackpad(x, y)
            return
        }
        if (current.repeatable || longPressFired) return
        val k = findKey(x, y)
        if (k !== current) {
            timer.removeCallbacks(longPressRunnable)
            downKey = k
            if (k != null) timer.postDelayed(longPressRunnable, 350)
            invalidate()
        }
    }

    private fun release(commit: Boolean) {
        timer.removeCallbacks(repeatRunnable)
        timer.removeCallbacks(longPressRunnable)
        if (trackpad) {
            downKey = null
            activeId = -1
            exitTrackpad()
            return
        }
        val k = downKey
        if (commit && k != null && !k.repeatable && !longPressFired) listener?.onKey(k)
        downKey = null
        activeId = -1
        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> press(e.getPointerId(0), e.x, e.y)
            MotionEvent.ACTION_POINTER_DOWN -> {
                release(true)
                val i = e.actionIndex
                press(e.getPointerId(i), e.getX(i), e.getY(i))
            }
            MotionEvent.ACTION_MOVE -> {
                val i = e.findPointerIndex(activeId)
                if (i >= 0) slide(e.getX(i), e.getY(i))
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (e.getPointerId(e.actionIndex) == activeId) release(true)
            }
            MotionEvent.ACTION_UP -> release(true)
            MotionEvent.ACTION_CANCEL -> release(false)
        }
        return true
    }
}
