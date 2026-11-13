package com.aegisinput.app

import org.junit.Assert.*
import org.junit.Test

class TypingControllerTest {
    private class Composer : ChineseComposer {
        override var text = ""
        override val candidates get() = if (text == "nihao") listOf("你好", "你号") else emptyList()
        override fun key(key: String) { text = if (key == "BackSpace") text.dropLast(1) else text + key }
        override fun select(index: Int): String = candidates[index].also { reset() }
        override fun reset() { text = "" }
    }
    private class Editor : TextEditor {
        var value = ""; var composing = ""; var enters = 0
        override fun compose(text: String) { composing = text }
        override fun commit(text: String) { value += text; composing = "" }
        override fun delete() { value = value.dropLast(1) }
        override fun enter() { enters++ }
    }
    private val composer = Composer()
    private val editor = Editor()
    private val typing = TypingController(editor, composer)
    private fun type(text: String, chinese: Boolean = true) { text.forEach { typing.key(it.toString(), chinese) } }

    @Test fun englishCommitsDirectly() { type("Hello", false); assertEquals("Hello", editor.value); assertEquals("", composer.text) }
    @Test fun candidateSelectionReplacesCompositionOnce() { type("nihao"); assertEquals("nihao", editor.composing); typing.select(1); assertEquals("你号", editor.value); assertEquals("", editor.composing); assertEquals("", composer.text) }
    @Test fun spaceChoosesChineseWithoutAppendingSpace() { type("nihao"); typing.key("SPACE", true); assertEquals("你好", editor.value) }
    @Test fun deletingLastComposingCharacterClearsEditorRegion() { type("n"); typing.key("BACKSPACE", true); assertEquals("", editor.composing); assertEquals("", composer.text) }
    @Test fun enterCommitsRawBeforePerformingEditorAction() { type("nihao"); typing.key("ENTER", true); assertEquals("nihao", editor.value); assertEquals(0, editor.enters); typing.key("ENTER", true); assertEquals(1, editor.enters) }
    @Test fun languageSwitchKeepsRawTextAndClearsSession() { type("ni"); typing.commitRaw(); type("Hello", false); assertEquals("niHello", editor.value); assertEquals("", composer.text) }
    @Test fun punctuationCommitsCandidateThenPunctuation() { type("nihao"); typing.key(".", true); assertEquals("你好.", editor.value); assertEquals("", composer.text) }
    @Test fun invalidCandidateCannotCommitStaleText() { type("n"); typing.select(99); assertEquals("", editor.value); assertEquals("n", editor.composing) }
}
