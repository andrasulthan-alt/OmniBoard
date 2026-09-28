package io.github.andrasulthan.omniboard

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager

class OmniImeService : InputMethodService(), KeyboardView.Listener {

    private var keyboard: KeyboardView? = null
    private var lastShiftTap = 0L

    /** True for passwords and fields that ask us not to learn. No learning, no suggestions here. */
    private var privateField = false

    /** User setting: capitalize the first letter of a sentence automatically. */
    private var autoCapEnabled = true

    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
    }

    override fun onCreateInputView(): View {
        val view = KeyboardView(this)
        view.listener = this
        keyboard = view
        return view
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        val view = keyboard ?: return
        val prefs = Prefs(this)
        view.hapticsEnabled = prefs.haptics
        view.setDark(prefs.isDark(this))
        autoCapEnabled = prefs.autoCap

        privateField = isPrivateField(info)
        view.incognito = privateField

        val cls = info.inputType and InputType.TYPE_MASK_CLASS
        if (cls == InputType.TYPE_CLASS_NUMBER ||
            cls == InputType.TYPE_CLASS_PHONE ||
            cls == InputType.TYPE_CLASS_DATETIME
        ) view.showSymbols() else view.showLetters()

        view.setEnterIcon(enterIcon(info))
        view.shift = Shift.OFF
        updateAutoCap()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        keyboard?.reset()
    }

    private fun isPrivateField(info: EditorInfo): Boolean {
        val cls = info.inputType and InputType.TYPE_MASK_CLASS
        val variation = info.inputType and InputType.TYPE_MASK_VARIATION
        val password = (cls == InputType.TYPE_CLASS_TEXT && (
            variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)) ||
            (cls == InputType.TYPE_CLASS_NUMBER &&
                variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD)
        val noLearning = (info.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0
        return password || noLearning
    }

    private fun enterAction(info: EditorInfo): Int? {
        if ((info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) return null
        val action = info.imeOptions and EditorInfo.IME_MASK_ACTION
        return when (action) {
            EditorInfo.IME_ACTION_NONE, EditorInfo.IME_ACTION_UNSPECIFIED -> null
            else -> action
        }
    }

    private fun enterIcon(info: EditorInfo): EnterIcon = when (enterAction(info)) {
        EditorInfo.IME_ACTION_SEARCH -> EnterIcon.SEARCH
        EditorInfo.IME_ACTION_GO -> EnterIcon.GO
        EditorInfo.IME_ACTION_SEND -> EnterIcon.SEND
        EditorInfo.IME_ACTION_NEXT -> EnterIcon.NEXT
        EditorInfo.IME_ACTION_PREVIOUS -> EnterIcon.PREVIOUS
        EditorInfo.IME_ACTION_DONE -> EnterIcon.DONE
        else -> EnterIcon.RETURN
    }

    private fun updateAutoCap() {
        val view = keyboard ?: return
        if (view.shift == Shift.LOCKED) return
        val info = currentInputEditorInfo ?: return
        val caps = autoCapEnabled &&
            (currentInputConnection?.getCursorCapsMode(info.inputType) ?: 0) != 0
        view.shift = if (!view.isSymbols && caps) Shift.ONCE else Shift.OFF
    }

    private fun deleteOne(ic: InputConnection) {
        val selected = ic.getSelectedText(0)
        if (!selected.isNullOrEmpty()) ic.commitText("", 1)
        else sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
    }

    private fun handleEnter() {
        val info = currentInputEditorInfo ?: return
        val action = enterAction(info)
        if (action == null) sendKeyChar('\n')
        else currentInputConnection?.performEditorAction(action)
    }

    private fun toggleShift(view: KeyboardView) {
        val now = SystemClock.uptimeMillis()
        view.shift = when {
            view.shift == Shift.LOCKED -> Shift.OFF
            view.shift == Shift.ONCE && now - lastShiftTap < 400 -> Shift.LOCKED
            view.shift == Shift.ONCE -> Shift.OFF
            else -> Shift.ONCE
        }
        lastShiftTap = now
    }

    override fun onKey(key: Key) {
        val ic = currentInputConnection ?: return
        val view = keyboard ?: return
        when (key.code) {
            Codes.TEXT -> {
                val t = if (view.shift != Shift.OFF) key.text.uppercase() else key.text
                ic.commitText(t, 1)
                updateAutoCap()
            }
            Codes.SPACE -> { ic.commitText(" ", 1); updateAutoCap() }
            Codes.DELETE -> { deleteOne(ic); updateAutoCap() }
            Codes.ENTER -> { handleEnter(); updateAutoCap() }
            Codes.SHIFT -> toggleShift(view)
            Codes.SYMBOLS -> view.showSymbols()
            Codes.LETTERS -> { view.showLetters(); updateAutoCap() }
        }
    }

    override fun onLongPress(key: Key): Boolean {
        // Hold space: open the keyboard picker (emergency way back to another keyboard).
        if (key.code == Codes.SPACE) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
            return true
        }
        val alt = key.longPress ?: return false
        currentInputConnection?.commitText(alt, 1)
        updateAutoCap()
        return true
    }
}
