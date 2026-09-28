package io.github.andrasulthan.omniboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.LinearLayout
import android.widget.TextView

class EmojiView(context: Context, private val listener: Listener) : LinearLayout(context) {

    interface Listener {
        fun onEmoji(emoji: String)
        fun onEmojiDelete()
        fun onEmojiSpace()
        fun onEmojiBack()
    }

    var hapticsEnabled = true

    private val match = ViewGroup.LayoutParams.MATCH_PARENT
    private val wrap = ViewGroup.LayoutParams.WRAP_CONTENT
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private var palette = Palette.DARK
    private val dotFont = DotFont.get(context)

    private val tabRow = LinearLayout(context)
    private val label = TextView(context)
    private val grid = GridView(context)
    private val bottomRow = LinearLayout(context)
    private val abcKey = TextView(context)
    private val spaceKey = LinearLayout(context)
    private val spaceDot = View(context)
    private val spaceText = TextView(context)
    private val deleteKey = TextView(context)

    private val tabTexts = mutableListOf<TextView>()
    private val tabDots = mutableListOf<View>()

    private var recent: List<String> = emptyList()
    private var items: List<String> = emptyList()

    private val grayscale = Paint().apply {
        colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
    }

    private val handler = Handler(Looper.getMainLooper())
    private val repeatDelete = object : Runnable {
        override fun run() {
            listener.onEmojiDelete()
            handler.postDelayed(this, 50)
        }
    }

    private val adapter = object : BaseAdapter() {
        override fun getCount() = items.size
        override fun getItem(position: Int): Any = items[position]
        override fun getItemId(position: Int) = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val tv = (convertView as? TextView) ?: TextView(context).apply {
                gravity = Gravity.CENTER
                textSize = 26f
                layoutParams = AbsListView.LayoutParams(match, dp(44))
            }
            tv.setTextColor(palette.text)
            tv.text = items[position]
            return tv
        }
    }

    init {
        orientation = LinearLayout.VERTICAL

        // ---- category tabs ----
        tabRow.orientation = LinearLayout.HORIZONTAL
        tabRow.gravity = Gravity.CENTER_VERTICAL
        val icons = listOf("🕘") + EmojiData.categories.map { it.icon }
        icons.forEachIndexed { i, icon ->
            val cell = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(0, dp(4), 0, dp(4))
            }
            val tv = TextView(context).apply {
                text = icon
                textSize = 18f
                gravity = Gravity.CENTER
            }
            val dot = View(context).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(0xFFD71921.toInt())
                }
            }
            cell.addView(tv, LinearLayout.LayoutParams(wrap, wrap))
            cell.addView(dot, LinearLayout.LayoutParams(dp(5), dp(5)).apply { topMargin = dp(3) })
            cell.setOnClickListener { tap(); select(i) }
            tabRow.addView(cell, LinearLayout.LayoutParams(0, wrap, 1f))
            tabTexts.add(tv)
            tabDots.add(dot)
        }
        addView(tabRow, LinearLayout.LayoutParams(match, wrap).apply { topMargin = dp(4) })

        // ---- category name ----
        label.textSize = 12f
        label.typeface = dotFont
        label.setPadding(dp(12), dp(2), dp(12), dp(2))
        addView(label, LinearLayout.LayoutParams(match, wrap))

        // ---- emoji grid ----
        grid.numColumns = 8
        grid.adapter = adapter
        grid.isVerticalScrollBarEnabled = false
        grid.overScrollMode = View.OVER_SCROLL_NEVER
        grid.setSelector(ColorDrawable(Color.TRANSPARENT))
        grid.setPadding(dp(4), 0, dp(4), 0)
        grid.setOnItemClickListener { _, _, position, _ ->
            tap()
            listener.onEmoji(items[position])
        }
        addView(grid, LinearLayout.LayoutParams(match, 0, 1f))

        // ---- bottom row: abc | space | delete ----
        bottomRow.orientation = LinearLayout.HORIZONTAL
        bottomRow.setPadding(dp(3), dp(3), dp(3), dp(3))

        abcKey.text = "abc"
        abcKey.typeface = dotFont
        abcKey.textSize = 14f
        abcKey.gravity = Gravity.CENTER
        abcKey.setOnClickListener { tap(); listener.onEmojiBack() }

        spaceKey.orientation = LinearLayout.HORIZONTAL
        spaceKey.gravity = Gravity.CENTER
        spaceText.text = "omniboard"
        spaceText.typeface = dotFont
        spaceText.textSize = 11f
        spaceKey.addView(spaceDot, LinearLayout.LayoutParams(dp(7), dp(7)))
        spaceKey.addView(spaceText, LinearLayout.LayoutParams(wrap, wrap).apply { marginStart = dp(7) })
        spaceKey.setOnClickListener { tap(); listener.onEmojiSpace() }

        deleteKey.text = "⌫"
        deleteKey.textSize = 20f
        deleteKey.gravity = Gravity.CENTER
        setupDeleteRepeat()

        val half = dp(2)
        bottomRow.addView(abcKey, LinearLayout.LayoutParams(0, match, 1.5f).apply { setMargins(half, half, half, half) })
        bottomRow.addView(spaceKey, LinearLayout.LayoutParams(0, match, 7f).apply { setMargins(half, half, half, half) })
        bottomRow.addView(deleteKey, LinearLayout.LayoutParams(0, match, 1.5f).apply { setMargins(half, half, half, half) })
        addView(bottomRow, LinearLayout.LayoutParams(match, dp(54)))

        setPalette(Palette.DARK)
    }

    fun setPalette(p: Palette) {
        palette = p
        setBackgroundColor(p.background)
        label.setTextColor(p.hint)
        abcKey.setTextColor(p.text)
        abcKey.background = keyBg(p.special, dp(12))
        spaceKey.background = keyBg(p.key, dp(100))
        spaceText.setTextColor(p.hint)
        spaceDot.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(p.accent)
        }
        deleteKey.setTextColor(p.text)
        deleteKey.background = keyBg(p.special, dp(12))
        adapter.notifyDataSetChanged()
    }

    /** Called every time the panel opens. Starts on "recent" if there is any history. */
    fun show(recentEmojis: List<String>) {
        recent = recentEmojis
        select(if (recent.isEmpty()) 1 else 0)
    }

    fun reset() {
        handler.removeCallbacksAndMessages(null)
    }

    private fun select(index: Int) {
        items = if (index == 0) recent else EmojiData.supported(index - 1)
        label.text = if (index == 0) "recent" else EmojiData.categories[index - 1].name
        tabTexts.forEachIndexed { i, tv ->
            val on = i == index
            tv.alpha = if (on) 1f else 0.55f
            if (on) tv.setLayerType(View.LAYER_TYPE_NONE, null)
            else tv.setLayerType(View.LAYER_TYPE_HARDWARE, grayscale)
            tabDots[i].visibility = if (on) View.VISIBLE else View.INVISIBLE
        }
        adapter.notifyDataSetChanged()
        grid.setSelection(0)
    }

    private fun tap() {
        if (hapticsEnabled) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    private fun keyBg(color: Int, radius: Int) = GradientDrawable().apply {
        cornerRadius = radius.toFloat()
        setColor(color)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDeleteRepeat() {
        deleteKey.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    tap()
                    v.alpha = 0.6f
                    listener.onEmojiDelete()
                    handler.postDelayed(repeatDelete, 400)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.alpha = 1f
                    handler.removeCallbacks(repeatDelete)
                }
            }
            true
        }
    }
}
