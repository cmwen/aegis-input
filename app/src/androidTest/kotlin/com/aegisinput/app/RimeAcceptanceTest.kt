package com.aegisinput.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.Room
import com.aegisinput.engine.RimeBridge
import com.aegisinput.engine.data.UserDictDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Tests the APK's real JNI library, bundled dictionaries and SQLite on Android. */
@RunWith(AndroidJUnit4::class)
class RimeAcceptanceTest {
    private fun convert(keys: List<String>, zhuyin: Boolean, expected: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue(RimeBridge.unavailableMessage(), RimeBridge.initialize(context))
        val session = RimeBridge.createSession()
        try {
            session.selectSchema(zhuyin)
            keys.forEach { session.processKey(it) }
            assertTrue("Composition is empty", session.composingText.isNotEmpty())
            val index = session.candidates.indexOf(expected)
            assertTrue("Missing $expected for $keys: ${session.candidates.take(12)}", index >= 0)
            assertEquals(expected, session.selectCandidate(index))
            assertEquals("", session.composingText)
            session.processKey(if (zhuyin) "ㄋ" else "n")
            session.reset()
            assertEquals("", session.composingText)
        } finally { RimeBridge.destroySession(session) }
    }

    @Test fun realPinyinPhraseConversion() = convert("nihao".map { it.toString() }, false, "你好")
    @Test fun realPinyinFuzzyInitial() = convert("zongwen".map { it.toString() }, false, "中文")
    @Test fun realZhuyinPhraseConversion() = convert(listOf("ㄋ", "ㄧ", "ˇ", "ㄏ", "ㄠ", "ˇ"), true, "你好")
    @Test fun realZhuyinFuzzyInitial() = convert(listOf("ㄗ", "ㄨ", "ㄥ", "ˉ", "ㄨ", "ㄣ", "ˊ"), true, "中文")

    @Test fun roomLearningKeepsMultipleWordsAndBoostsFrequency() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, UserDictDatabase::class.java).build()
        try {
            val dao = database.userDictDao()
            dao.learn("PINYIN:shi", "是")
            dao.learn("PINYIN:shi", "時")
            dao.learn("PINYIN:shi", "時")
            val words = dao.getEntries("PINYIN:shi")
            assertEquals(2, words.size)
            assertEquals("時", words.first().word)
            assertEquals(2, words.first().frequency)
        } finally { database.close() }
    }
}
