package com.aegisinput.app

import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection

/**
 * Thread-safe wrapper around Android's InputConnection that handles
 * text insertion, deletion, and composing text states.
 */
class InputConnectionWrapper {

    private var inputConnection: InputConnection? = null
    private var editorInfo: EditorInfo? = null
    private var isComposing = false

    fun bind(ic: InputConnection?, info: EditorInfo?) {
        inputConnection = ic
        editorInfo = info
        isComposing = false
    }

    fun unbind() {
        finishComposingText()
        inputConnection = null
        editorInfo = null
    }

    fun commitText(text: CharSequence, newCursorPosition: Int) {
        isComposing = false
        inputConnection?.commitText(text, newCursorPosition)
    }

    fun setComposingText(text: CharSequence, newCursorPosition: Int) {
        isComposing = true
        inputConnection?.setComposingText(text, newCursorPosition)
    }

    fun finishComposingText() {
        if (isComposing) {
            isComposing = false
            inputConnection?.finishComposingText()
        }
    }

    fun deleteSurroundingText(beforeLength: Int, afterLength: Int) {
        inputConnection?.deleteSurroundingText(beforeLength, afterLength)
    }

    fun deletePreviousCharacter() {
        val ic = inputConnection ?: return
        val selected = ic.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            ic.commitText("", 1)
        } else if (!ic.deleteSurroundingTextInCodePoints(1, 0)) {
            // Some editors do not implement the code-point API.
            val before = ic.getTextBeforeCursor(2, 0)?.toString().orEmpty()
            val count = if (before.length >= 2 && Character.isSurrogatePair(before[before.length - 2], before.last())) 2 else 1
            ic.deleteSurroundingText(count, 0)
        }
    }

    fun sendKeyEvent(event: KeyEvent) {
        inputConnection?.sendKeyEvent(event)
    }

    fun performEnterAction() {
        when (val behavior = ImeCompatibilityPolicy.resolveEnterKeyBehavior(editorInfo?.imeOptions)) {
            is EnterKeyBehavior.PerformEditorAction -> {
                if (inputConnection?.performEditorAction(behavior.actionId) == true) {
                    return
                }
            }
            EnterKeyBehavior.SendEnterKey -> Unit
        }

        sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
    }

    fun getTextBeforeCursor(length: Int): CharSequence? {
        return inputConnection?.getTextBeforeCursor(length, 0)
    }

    fun getTextAfterCursor(length: Int): CharSequence? {
        return inputConnection?.getTextAfterCursor(length, 0)
    }
}
