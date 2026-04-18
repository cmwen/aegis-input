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

    fun sendKeyEvent(event: KeyEvent) {
        inputConnection?.sendKeyEvent(event)
    }

    fun getTextBeforeCursor(length: Int): CharSequence? {
        return inputConnection?.getTextBeforeCursor(length, 0)
    }

    fun getTextAfterCursor(length: Int): CharSequence? {
        return inputConnection?.getTextAfterCursor(length, 0)
    }
}
