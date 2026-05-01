package com.aegisinput.app

import android.text.InputType
import android.view.inputmethod.EditorInfo
import com.aegisinput.ui.keyboard.KeyboardMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ImeCompatibilityPolicyTest {

    @Test
    fun fullscreenModeRemainsDisabledForComposeIme() {
        assertFalse(ImeCompatibilityPolicy.shouldUseFullscreenMode())
    }

    @Test
    fun chineseModeStaysEnabledForGeneralTextFields() {
        val editorInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
        }

        val keyboardMode = ImeCompatibilityPolicy.resolveInitialKeyboardMode(
            editorInfo = editorInfo,
            chineseMode = KeyboardMode.ZHUYIN,
            nativeEngineAvailable = true
        )

        assertEquals(KeyboardMode.ZHUYIN, keyboardMode)
    }

    @Test
    fun passwordFieldsPreferLatinModeForCompatibility() {
        val editorInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val keyboardMode = ImeCompatibilityPolicy.resolveInitialKeyboardMode(
            editorInfo = editorInfo,
            chineseMode = KeyboardMode.PINYIN,
            nativeEngineAvailable = true
        )

        assertEquals(KeyboardMode.LATIN, keyboardMode)
    }

    @Test
    fun numericFieldsPreferSymbolsMode() {
        val editorInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        val keyboardMode = ImeCompatibilityPolicy.resolveInitialKeyboardMode(
            editorInfo = editorInfo,
            chineseMode = KeyboardMode.ZHUYIN,
            nativeEngineAvailable = true
        )

        assertEquals(KeyboardMode.SYMBOLS, keyboardMode)
    }

    @Test
    fun latinModeIsForcedWhenNativeEngineIsUnavailable() {
        val editorInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
        }

        val keyboardMode = ImeCompatibilityPolicy.resolveInitialKeyboardMode(
            editorInfo = editorInfo,
            chineseMode = KeyboardMode.ZHUYIN,
            nativeEngineAvailable = false
        )

        assertEquals(KeyboardMode.LATIN, keyboardMode)
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
