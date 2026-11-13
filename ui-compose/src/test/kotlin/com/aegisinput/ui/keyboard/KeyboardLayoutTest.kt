package com.aegisinput.ui.keyboard

import org.junit.Assert.*
import org.junit.Test

class KeyboardLayoutTest {
    private fun rows(mode: KeyboardMode, shift: Boolean = false) = KeyboardLayout.rowsFor(mode, KeyboardMode.PINYIN, shift)

    @Test fun englishAndPinyinShareFamiliarLetterRows() {
        for (mode in listOf(KeyboardMode.LATIN, KeyboardMode.PINYIN)) {
            assertEquals("qwertyuiop", rows(mode)[0].joinToString("") { it.code })
            assertEquals("asdfghjkl", rows(mode)[1].joinToString("") { it.code })
            assertEquals("zxcvbnm", rows(mode)[2].filter { it.type == KeyType.CHARACTER }.joinToString("") { it.code })
        }
    }
    @Test fun languageSwitchIsVisibleWithoutCrowdingOutSpace() {
        assertTrue(rows(KeyboardMode.LATIN).last().any { it.code == "MODE_CHINESE" && it.label == "中" })
        assertTrue(rows(KeyboardMode.PINYIN).last().any { it.code == "MODE_LATIN" && it.label == "EN" })
        assertTrue(rows(KeyboardMode.PINYIN).last().first { it.code == "SPACE" }.widthWeight >= 3f)
        assertFalse(rows(KeyboardMode.LATIN).last().any { it.code == "MODE_COMMANDS" })
    }
    @Test fun shiftCapitalizesEnglishAndKeepsPinyinLowercase() {
        assertEquals("Q", rows(KeyboardMode.LATIN, true)[0][0].code)
        assertEquals("q", rows(KeyboardMode.PINYIN, true)[0][0].code)
    }
    @Test fun punctuationIncludesExplicitSyllableBoundaryAndBothLanguageReturns() {
        assertTrue(rows(KeyboardMode.SYMBOLS).flatten().any { it.code == "'" })
        assertTrue(rows(KeyboardMode.SYMBOLS).last().any { it.code == "MODE_CHINESE" })
        assertTrue(rows(KeyboardMode.SYMBOLS).last().any { it.code == "MODE_LATIN" })
    }
}
