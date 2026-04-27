package com.aegisinput.app

import android.view.inputmethod.EditorInfo

internal object ImeCompatibilityPolicy {

    fun shouldUseFullscreenMode(): Boolean = false

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
