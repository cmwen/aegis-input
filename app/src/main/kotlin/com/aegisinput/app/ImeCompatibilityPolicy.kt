package com.aegisinput.app

import android.text.InputType
import android.view.inputmethod.EditorInfo
import com.aegisinput.ui.keyboard.KeyboardMode

internal object ImeCompatibilityPolicy {

    fun shouldUseFullscreenMode(): Boolean = false

    fun resolveInitialKeyboardMode(
        editorInfo: EditorInfo?,
        chineseMode: KeyboardMode,
        nativeEngineAvailable: Boolean
    ): KeyboardMode {
        if (!nativeEngineAvailable) return KeyboardMode.LATIN

        val inputType = editorInfo?.inputType ?: return chineseMode
        return when (inputType and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_NUMBER,
            InputType.TYPE_CLASS_DATETIME,
            InputType.TYPE_CLASS_PHONE -> KeyboardMode.SYMBOLS
            InputType.TYPE_CLASS_TEXT -> {
                when (inputType and InputType.TYPE_MASK_VARIATION) {
                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                    InputType.TYPE_TEXT_VARIATION_EMAIL_SUBJECT,
                    InputType.TYPE_TEXT_VARIATION_FILTER,
                    InputType.TYPE_TEXT_VARIATION_PASSWORD,
                    InputType.TYPE_TEXT_VARIATION_PERSON_NAME,
                    InputType.TYPE_TEXT_VARIATION_PHONETIC,
                    InputType.TYPE_TEXT_VARIATION_URI,
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                    InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
                    InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> KeyboardMode.LATIN
                    else -> chineseMode
                }
            }
            else -> chineseMode
        }
    }

    fun resolveEnterKeyBehavior(imeOptions: Int?): EnterKeyBehavior {
        if (imeOptions == null) return EnterKeyBehavior.SendEnterKey
        if ((imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) {
            return EnterKeyBehavior.SendEnterKey
        }

        return when (val actionId = imeOptions and EditorInfo.IME_MASK_ACTION) {
            EditorInfo.IME_ACTION_NONE,
            EditorInfo.IME_ACTION_UNSPECIFIED -> EnterKeyBehavior.SendEnterKey
            else -> EnterKeyBehavior.PerformEditorAction(actionId)
        }
    }
}

internal sealed interface EnterKeyBehavior {
    data class PerformEditorAction(val actionId: Int) : EnterKeyBehavior
    data object SendEnterKey : EnterKeyBehavior
}
