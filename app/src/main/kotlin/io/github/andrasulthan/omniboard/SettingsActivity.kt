package io.github.andrasulthan.omniboard

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class SettingsActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var palette: Palette
    private lateinit var dotFont: Typeface
    private lateinit var status: TextView
    private var dark = true

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        dark = prefs.isDark(this)
        palette = if (dark) Palette.DARK else Palette.LIGHT
        dotFont = DotFont.get(this)

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        // ---- header ----
        root.addView(text(getString(R.string.app_name), 40f, palette.text, dotFont))
        root.addView(text(getString(R.string.privacy_note), 14f, palette.hint), lp(6))

        // ---- setup ----
        root.addView(section(getString(R.string.section_setup)), lp(32))
        status = text("", 15f, palette.text)
        root.addView(status, lp(10))
        root.addView(pill(getString(R.string.step_enable), accent = true) {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }, lp(12))
        root.addView(pill(getString(R.string.step_choose), accent = false) {
            imm().showInputMethodPicker()
        }, lp(10))
        root.addView(EditText(this).apply {
            hint = getString(R.string.test_hint)
            setTextColor(palette.text)
            setHintTextColor(palette.hint)
            background = pillBg(palette.key)
            setPadding(dp(20), dp(14), dp(20), dp(14))
        }, lp(10))

        // ---- look ----
        root.addView(section(getString(R.string.section_look)), lp(32))
        val themes = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val options = listOf(
            "system" to R.string.theme_system,
            "dark" to R.string.theme_dark,
            "light" to R.string.theme_light,
        )
        options.forEachIndexed { i, (value, label) ->
            val selected = prefs.theme == value
            val chip = TextView(this).apply {
                text = getString(label)
                gravity = Gravity.CENTER
                textSize = 14f
                setTextColor(if (selected) palette.background else palette.text)
                background = pillBg(if (selected) palette.text else palette.key)
                setPadding(0, dp(12), 0, dp(12))
                setOnClickListener {
                    prefs.theme = value
                    recreate()
                }
            }
            val p = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            if (i > 0) p.marginStart = dp(8)
            themes.addView(chip, p)
        }
        root.addView(themes, lp(12))

        // ---- feel ----
        root.addView(section(getString(R.string.section_feel)), lp(32))
        val hapticsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = pillBg(palette.key)
            setPadding(dp(20), dp(8), dp(8), dp(8))
        }
        hapticsRow.addView(
            text(getString(R.string.haptics), 15f, palette.text),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        val toggle = TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = 13f
            typeface = dotFont
            setPadding(0, dp(8), 0, dp(8))
        }
        fun paintToggle() {
            val on = prefs.haptics
            toggle.text = if (on) "on" else "off"
            toggle.setTextColor(if (on) Color.WHITE else palette.hint)
            toggle.background = pillBg(if (on) palette.accent else palette.special)
        }
        paintToggle()
        hapticsRow.setOnClickListener {
            prefs.haptics = !prefs.haptics
            paintToggle()
        }
        hapticsRow.addView(toggle, LinearLayout.LayoutParams(dp(72), ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(hapticsRow, lp(12))

        // ---- privacy ----
        root.addView(section(getString(R.string.section_privacy)), lp(32))
        root.addView(text(getString(R.string.privacy_details), 14f, palette.hint), lp(10))

        // ---- last crash (only if there is one) ----
        CrashLog.read(this)?.let { crash ->
            root.addView(text(getString(R.string.last_crash), 13f, palette.accent), lp(32))
            root.addView(text(crash.take(3000), 11f, palette.hint, Typeface.MONOSPACE).apply {
                setTextIsSelectable(true)
            }, lp(8))
            root.addView(pill(getString(R.string.clear), accent = false) {
                CrashLog.clear(this)
                recreate()
            }, lp(10))
        }

        val scroll = ScrollView(this).apply {
            setBackgroundColor(palette.background)
            isFillViewport = true
            addView(root)
        }
        // Edge-to-edge: keep content clear of the status bar and navigation bar.
        scroll.setOnApplyWindowInsetsListener { _, insets ->
            val top: Int
            val bottom: Int
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                top = bars.top
                bottom = bars.bottom
            } else {
                @Suppress("DEPRECATION")
                top = insets.systemWindowInsetTop
                @Suppress("DEPRECATION")
                bottom = insets.systemWindowInsetBottom
            }
            root.setPadding(dp(24), top + dp(24), dp(24), bottom + dp(32))
            insets
        }
        setContentView(scroll)
        window.decorView.setBackgroundColor(palette.background)

        if (Build.VERSION.SDK_INT >= 30) {
            val lightBars = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            window.insetsController?.setSystemBarsAppearance(if (dark) 0 else lightBars, lightBars)
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) refresh()
    }

    // ---------- helpers ----------

    private fun imm() = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

    private fun lp(topDp: Int) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(topDp) }

    private fun pillBg(color: Int) = GradientDrawable().apply {
        cornerRadius = dp(100).toFloat()
        setColor(color)
    }

    private fun text(value: String, sizeSp: Float, color: Int, font: Typeface? = null) =
        TextView(this).apply {
            text = value
            textSize = sizeSp
            setTextColor(color)
            if (font != null) typeface = font
        }

    private fun section(title: String): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val dot = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(palette.accent)
            }
        }
        row.addView(dot, LinearLayout.LayoutParams(dp(7), dp(7)))
        row.addView(text(title, 15f, palette.hint, dotFont), LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { marginStart = dp(10) })
        return row
    }

    private fun pill(label: String, accent: Boolean, onClick: () -> Unit) =
        TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = 15f
            setTextColor(if (accent) Color.WHITE else palette.text)
            background = pillBg(if (accent) palette.accent else palette.key)
            setPadding(dp(20), dp(15), dp(20), dp(15))
            setOnClickListener { onClick() }
        }

    private fun refresh() {
        val enabled = imm().enabledInputMethodList.any { it.packageName == packageName }
        val current = try {
            Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: ""
        } catch (e: Exception) {
            ""
        }
        val active = current.startsWith("$packageName/")
        status.text = getString(
            when {
                active -> R.string.status_active
                enabled -> R.string.status_enabled
                else -> R.string.status_disabled
            }
        )
    }
}
