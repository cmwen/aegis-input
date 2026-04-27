package com.aegisinput.app

import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ImeCompatibilityPolicyTest {

    @Test
    fun fullscreenModeRemainsDisabledForComposeIme() {
        assertFalse(ImeCompatibilityPolicy.shouldUseFullscreenMode())
    }

    @Test
    fun enterKeyUsesEditorActionWhenAvailable() {
        val behavior = ImeCompatibilityPolicy.resolveEnterKeyBehavior(EditorInfo.IME_ACTION_DONE)

        assertEquals(EnterKeyBehavior.PerformEditorAction(EditorInfo.IME_ACTION_DONE), behavior)
    }

    @Test
    fun enterKeyFallsBackToRawEnterWhenActionIsSuppressed() {
        val behavior = ImeCompatibilityPolicy.resolveEnterKeyBehavior(
            EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_ENTER_ACTION
        )

        assertEquals(EnterKeyBehavior.SendEnterKey, behavior)
    }

    @Test
    fun enterKeyFallsBackToRawEnterWhenActionIsUnspecified() {
        val behavior = ImeCompatibilityPolicy.resolveEnterKeyBehavior(
            EditorInfo.IME_ACTION_UNSPECIFIED
        )

        assertEquals(EnterKeyBehavior.SendEnterKey, behavior)
    }
}
