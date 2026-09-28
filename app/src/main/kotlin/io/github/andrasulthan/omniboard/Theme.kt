package io.github.andrasulthan.omniboard

import android.content.Context
import android.graphics.Typeface

class Palette(
    val background: Int,
    val key: Int,
    val special: Int,
    val pressedKey: Int,
    val text: Int,
    val hint: Int,
    val accent: Int,
) {
    companion object {
        val DARK = Palette(
            background = 0xFF000000.toInt(),
            key = 0xFF1C1C1C.toInt(),
            special = 0xFF2E2E2E.toInt(),
            pressedKey = 0xFF4A4A4A.toInt(),
            text = 0xFFFFFFFF.toInt(),
            hint = 0xFF8A8A8A.toInt(),
            accent = 0xFFD71921.toInt(),
        )
        val LIGHT = Palette(
            background = 0xFFEDEDED.toInt(),
            key = 0xFFFFFFFF.toInt(),
            special = 0xFFD6D6D6.toInt(),
            pressedKey = 0xFFBDBDBD.toInt(),
            text = 0xFF000000.toInt(),
            hint = 0xFF7A7A7A.toInt(),
            accent = 0xFFD71921.toInt(),
        )
    }
}

object DotFont {
    @Volatile
    private var cached: Typeface? = null

    /**
     * Doto (SIL OFL 1.1) is downloaded into assets/fonts/doto.ttf by the build workflow.
     * If it is missing, we fall back to the system monospace font.
     */
    fun get(context: Context): Typeface {
        cached?.let { return it }
        val tf = try {
            context.assets.open("fonts/doto.ttf").close()
            Typeface.Builder(context.assets, "fonts/doto.ttf")
                .setFontVariationSettings("'wght' 800, 'ROND' 100")
                .build() ?: Typeface.MONOSPACE
        } catch (e: Exception) {
            Typeface.MONOSPACE
        }
        cached = tf
        return tf
    }
}
