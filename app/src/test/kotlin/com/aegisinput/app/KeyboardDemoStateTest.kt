package com.aegisinput.app

import com.aegisinput.ui.keyboard.KeyboardMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardDemoStateTest {

    @Test
    fun chineseInputBuildsCompositionAndCandidates() {
        val state = KeyboardDemoState()
            .handleKeyPress("ㄅ")
            .handleKeyPress("ㄚ")

        assertEquals("ㄅㄚ", state.composingText)
        assertEquals(listOf("ㄅㄚ"), state.candidates)
        assertTrue(state.committedText.isEmpty())
    }

    @Test
    fun pinyinInputNormalizesToLowercase() {
        val state = KeyboardDemoState()
            .setChineseMode(KeyboardMode.PINYIN)
            .handleKeyPress("N")
            .handleKeyPress("I")

        assertEquals("ni", state.composingText)
        assertEquals(listOf("ni"), state.candidates)
    }

    @Test
    fun spaceCommitsFirstChineseCandidate() {
        val state = KeyboardDemoState()
            .handleKeyPress("ㄓ")
            .handleKeyPress("SPACE")

        assertEquals("ㄓ", state.committedText)
        assertTrue(state.composingText.isEmpty())
        assertTrue(state.candidates.isEmpty())
    }

    @Test
    fun latinModeCommitsKeysDirectly() {
        val state = KeyboardDemoState()
            .setKeyboardMode(KeyboardMode.LATIN)
            .handleKeyPress("A")
            .handleKeyPress("B")

        assertEquals("AB", state.committedText)
        assertTrue(state.composingText.isEmpty())
        assertTrue(state.candidates.isEmpty())
    }
}
