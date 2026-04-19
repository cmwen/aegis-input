package com.aegisinput.ui.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardLayoutTest {

    @Test
    fun latinLayoutIncludesChineseModeSwitch() {
        val bottomRow = KeyboardLayout.rowsFor(
            mode = KeyboardMode.LATIN,
            chineseMode = KeyboardMode.ZHUYIN,
            uppercaseLatin = false
        ).last()

        assertEquals("MODE_CHINESE", bottomRow.first().code)
        assertEquals("注音", bottomRow.first().label)
    }

    @Test
    fun pinyinLayoutUsesLatinEscapeKey() {
        val bottomRow = KeyboardLayout.rowsFor(
            mode = KeyboardMode.PINYIN,
            chineseMode = KeyboardMode.PINYIN,
            uppercaseLatin = false
        ).last()

        assertEquals("MODE_LATIN", bottomRow.first().code)
        assertEquals("ABC", bottomRow.first().label)
    }

    @Test
    fun uppercaseLatinRowsCapitalizeLetters() {
        val topLeftKey = KeyboardLayout.rowsFor(
            mode = KeyboardMode.LATIN,
            chineseMode = KeyboardMode.ZHUYIN,
            uppercaseLatin = true
        ).first().first()

        assertEquals("Q", topLeftKey.label)
        assertEquals("Q", topLeftKey.code)
    }

    @Test
    fun symbolLayoutOffersBothReturnModes() {
        val bottomRow = KeyboardLayout.rowsFor(
            mode = KeyboardMode.SYMBOLS,
            chineseMode = KeyboardMode.PINYIN,
            uppercaseLatin = false
        ).last()

        assertTrue(bottomRow.any { it.code == "MODE_CHINESE" && it.label == "拼音" })
        assertTrue(bottomRow.any { it.code == "MODE_LATIN" && it.label == "ABC" })
    }
}
