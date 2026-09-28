package io.github.andrasulthan.omniboard

import android.content.Context
import android.content.res.Configuration
import java.io.File

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("omniboard", Context.MODE_PRIVATE)

    var haptics: Boolean
        get() = sp.getBoolean("haptics", true)
        set(value) = sp.edit().putBoolean("haptics", value).apply()

    var autoCap: Boolean
        get() = sp.getBoolean("auto_cap", true)
        set(value) = sp.edit().putBoolean("auto_cap", value).apply()

    var theme: String
        get() = sp.getString("theme", "system") ?: "system"
        set(value) = sp.edit().putString("theme", value).apply()

    /** Recently used emoji, newest first. Stored only on this phone (backup is disabled). */
    var recentEmojis: List<String>
        get() = (sp.getString("recent_emojis", "") ?: "").split("\n").filter { it.isNotEmpty() }
        set(value) = sp.edit().putString("recent_emojis", value.joinToString("\n")).apply()

    fun addRecentEmoji(emoji: String) {
        recentEmojis = (listOf(emoji) + recentEmojis.filter { it != emoji }).take(32)
    }

    fun isDark(context: Context): Boolean = when (theme) {
        "dark" -> true
        "light" -> false
        else -> (context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }
}

/** Saves the last crash (stack trace only, never typed text) to a private file. */
object CrashLog {
    private const val FILE_NAME = "last_crash.txt"

    @Volatile
    private var installed = false

    fun install(context: Context) {
        if (installed) return
        installed = true
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                File(appContext.filesDir, FILE_NAME).writeText(error.stackTraceToString())
            } catch (e: Exception) {
                // ignore
            }
            previous?.uncaughtException(thread, error)
        }
    }

    fun read(context: Context): String? {
        val f = File(context.filesDir, FILE_NAME)
        return if (f.exists()) f.readText() else null
    }

    fun clear(context: Context) {
        File(context.filesDir, FILE_NAME).delete()
    }
}
