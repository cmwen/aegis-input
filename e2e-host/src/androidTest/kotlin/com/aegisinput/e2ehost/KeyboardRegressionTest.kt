package com.aegisinput.e2ehost

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.SystemClock
import android.text.InputType
import android.util.Base64
import android.view.inputmethod.BaseInputConnection
import android.widget.EditText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import java.io.File

/** Cross-process tests: every character enters through a visible real IME key. */
@RunWith(AndroidJUnit4::class)
class KeyboardRegressionTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)
    private val imePackage = "com.aegisinput.app.e2e"
    private val ime = "$imePackage/com.aegisinput.app.AegisInputService"
    private val timeout = 15000L

    @get:Rule val diagnostics = object : TestWatcher() {
        override fun finished(description: Description) { device.unfreezeRotation() }
        override fun failed(error: Throwable, description: Description) {
            val dir = File(instrumentation.targetContext.filesDir, "diagnostics").apply { mkdirs() }
            device.dumpWindowHierarchy(File(dir, "${description.methodName}.xml"))
            if (dir.listFiles().orEmpty().count { it.extension == "png" } < 5)
                device.takeScreenshot(File(dir, "${description.methodName}.png"))
            File(dir, "${description.methodName}.txt").writeText(error.stackTraceToString())
        }
    }

    private fun shell(command: String) = device.executeShellCommand(command).trim()
    private fun await(message: String, limit: Long = timeout, condition: () -> Boolean) {
        val end = SystemClock.uptimeMillis() + limit
        do {
            if (condition()) return
            SystemClock.sleep(100) // bounded condition polling, never a UI timing assumption
        } while (SystemClock.uptimeMillis() < end)
        fail("Timed out: $message; actual editor=${text()}; composing=${composing()}")
    }
    private fun find(selector: BySelector, message: String, limit: Long = timeout): UiObject2 {
        var found: UiObject2? = null
        await(message, limit) {
            // Android 10 can retain stale Compose IME nodes after content changes.
            // Reapplying unchanged public service info clears its accessibility cache.
            instrumentation.uiAutomation.apply { serviceInfo = serviceInfo }
            found = device.findObject(selector)
            found != null
        }
        return found!!
    }
    private fun key(label: String) {
        find(By.pkg(imePackage).desc(label), "IME key: $label").click()
    }
    private fun label(value: String) {
        find(By.pkg(imePackage).text(value), "IME text: $value").click()
    }
    private fun type(value: String) = value.forEach {
        key(it.toString())
        // Wait for accessibility/editor work, including tones that normalize to
        // unchanged preedit. No assumption that every key must change text.
        device.waitForIdle(timeout)
        instrumentation.waitForIdleSync()
    }
    private fun <T> readEditor(block: (EditText) -> T): T {
        var result: T? = null
        instrumentation.runOnMainSync {
            val activity = EditorActivity.current ?: error("Editor not resumed")
            result = block(activity.findViewById(R.id.editor))
        }
        @Suppress("UNCHECKED_CAST") return result as T
    }
    private fun text() = readEditor { it.text.toString() }
    private fun composing() = readEditor { BaseInputConnection.getComposingSpanStart(it.text) >= 0 }
    private fun expect(value: String) = await("editor equals <$value>") { text() == value }
    private fun focus() {
        find(By.res("com.aegisinput.e2ehost", "editor"), "target editor").click()
        device.waitForIdle(timeout)
        instrumentation.waitForIdleSync()
        assertTrue("IME visible", device.wait(Until.hasObject(By.pkg(imePackage).desc("Enter")), timeout))
    }
    private fun launch(kind: String = "normal") {
        device.setOrientationNatural()
        val context = instrumentation.targetContext
        val previous = EditorActivity.current
        context.startActivity(Intent(context, EditorActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra("kind", kind)
        })
        await("fresh host resumed") { EditorActivity.current != null && EditorActivity.current !== previous }
        focus()
        // A fresh deployment can take minutes; keys remain available while it prepares.
        if (kind != "password") assertTrue("Chinese engine ready", device.wait(Until.hasObject(By.pkg(imePackage).text("拼音")), 180000))
    }
    private fun clear() {
        instrumentation.runOnMainSync { EditorActivity.current!!.findViewById<EditText>(R.id.editor).setText("") }
        focus()
        expect("")
    }
    private fun candidate(word: String) {
        val before = learning()
        label(word); await("composition committed") { !composing() }
        await("selected candidate persisted in Room") {
            learning().any { (key, value) -> key.endsWith(":$word") && value == (before[key] ?: 0) + 1 }
        }
    }

    @Test fun englishEditingThroughInputConnection() {
        launch(); label("拼音"); key("Switch to English")
        type("hello"); expect("hello")
        key("Space"); key("Shift"); key("W"); type("orld"); key(".")
        expect("hello World.")
        key("Backspace"); expect("hello World")
        key("Enter"); type("ok"); expect("hello World\nok")
        assertFalse(composing())
        instrumentation.runOnMainSync { EditorActivity.current!!.findViewById<EditText>(R.id.editor).setSelection(6, 11) }
        key("Backspace"); expect("hello \nok")
    }

    @Test fun pinyinFuzzyCompositionAndSchemaSwitch() {
        launch(); label("拼音"); type("nihao")
        await("real composing span") { composing() }
        candidate("你好"); expect("你好")
        clear(); type("zongwen"); candidate("中文"); expect("中文")
        clear(); type("nihaox")
        await("trailing x reaches composing editor") { composing() && text().endsWith("x") }
        val full = text(); key("Backspace")
        await("backspace removes trailing x from $full") { composing() && text() != full && text().endsWith("o") }
        candidate("你好"); expect("你好")
        clear(); type("ni")
        await("complete raw ni composition") { composing() && text().endsWith("i") }
        val enterRaw = text(); key("Enter")
        await("Enter commits visible raw composition") { text() == enterRaw && !composing() }
        clear(); type("ni")
        await("complete ni before schema switch") { composing() && text().endsWith("i") }
        val raw = text(); label("注音")
        await("schema switch commits visible raw composition") { text() == raw && !composing() }
        type("ㄋㄧˇㄏㄠˇ"); candidate("你好"); expect(raw + "你好")
    }

    @Test fun zhuyinFuzzyAndSpaceCandidate() {
        launch(); label("注音"); type("ㄋㄧˇㄏㄠˇ")
        await("Zhuyin composition") { composing() }
        candidate("你好"); expect("你好")
        clear(); type("ㄗㄨㄥˉㄨㄣˊ"); candidate("中文"); expect("中文")
        clear(); label("拼音"); type("nihao")
        find(By.pkg(imePackage).text("你好"), "space-selectable candidate")
        val before = learning()
        key("Space"); expect("你好"); assertFalse(composing())
        await("space selection persisted") {
            learning().any { (key, value) -> key.endsWith(":你好") && value == (before[key] ?: 0) + 1 }
        }
    }

    @Test fun fourRotationsPreserveEditorAndAllowRefocus() {
        launch(); label("拼音"); key("Switch to English"); type("hello"); expect("hello")
        val initialPid = shell("pidof $imePackage")
        assertTrue("IME process running before rotation", initialPid.isNotEmpty())
        label("拼音"); type("nihao"); await("complete composition before rotation") { composing() && text().endsWith("o") }
        var expected = text()
        repeat(4) { index ->
            val previous = device.displayRotation
            val previousActivity = EditorActivity.current
            if (index % 2 == 0) device.setOrientationLeft() else device.setOrientationNatural()
            await("orientation changed") { device.displayRotation != previous }
            await("new target resumed after rotation") { EditorActivity.current != null && EditorActivity.current !== previousActivity }
            expect(expected) // Activity state restoration must preserve actual editor text.
            focus() // Android may intentionally dismiss the keyboard on recreation.
            assertEquals("IME must survive rotation without process death", initialPid, shell("pidof $imePackage"))
            key("Switch to English"); type("x")
            expected += "x"; expect(expected)
            if (index < 3) {
                label("拼音"); type("ni")
                await("complete composition before next rotation") { composing() && text().endsWith("i") }
                expected = text()
            }
        }
        label("注音"); type("ㄋㄧˇㄏㄠˇ"); candidate("你好"); expect(expected + "你好")
    }

    @Test fun passwordAndSearchAction() {
        launch(); label("注音") // prove sensitive editor overrides saved Chinese preference
        val before = learning()
        launch("password")
        assertTrue(readEditor { it.inputType and InputType.TYPE_TEXT_VARIATION_PASSWORD != 0 })
        assertFalse(device.hasObject(By.pkg(imePackage).text("注音")))
        type("hello"); expect("hello"); assertFalse(composing())
        key("Backspace"); expect("hell")
        assertEquals("Passwords must not learn", before, learning())
        launch("search"); key("Switch to English"); type("hello"); key("Enter")
        assertTrue("Editor receives IME_ACTION_SEARCH", device.wait(Until.hasObject(By.res("com.aegisinput.e2ehost", "action").text("action:3")), timeout))
        expect("hello")
    }

    /** Snapshot the real private Room database plus WAL, using shell's debug run-as. */
    private fun learning(): Map<String, Int> {
        val dir = File(instrumentation.targetContext.cacheDir, "room-snapshot").apply { mkdirs() }
        val names = shell("run-as $imePackage ls databases").lines()
        listOf("", "-wal", "-shm").forEach { suffix ->
            val destination = File(dir, "aegis_user_dict.db$suffix")
            destination.delete()
            val path = "databases/aegis_user_dict.db$suffix"
            if (names.contains("aegis_user_dict.db$suffix")) {
                destination.writeBytes(Base64.decode(shell("run-as $imePackage base64 $path"), Base64.DEFAULT))
            }
        }
        val database = File(dir, "aegis_user_dict.db")
        if (!database.exists()) return emptyMap()
        return SQLiteDatabase.openDatabase(database.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("SELECT code,word,frequency FROM user_dict ORDER BY code,word", null).use { cursor ->
                buildMap { while (cursor.moveToNext()) put(cursor.getString(0) + ":" + cursor.getString(1), cursor.getInt(2)) }
            }
        }
    }
    @Test fun roomLearningAndPreferencesSurviveImeProcessRestart() {
        launch(); label("注音")
        val before = learning()
        type("ㄋㄧˇㄏㄠˇ"); candidate("你好"); expect("你好")
        var learned = emptyMap<String, Int>()
        await("Room persisted selected candidate") {
            learned = learning()
            learned.any { (key, value) -> key.startsWith("ZHUYIN:") && key.endsWith(":你好") && value == (before[key] ?: 0) + 1 }
        }
        val preferences = shell("run-as $imePackage cat shared_prefs/keyboard.xml")
        assertTrue(preferences.contains("name=\"zhuyin\" value=\"true\""))
        val pid = shell("pidof $imePackage")
        assertTrue("IME process running", pid.isNotEmpty())
        shell("am force-stop $imePackage")
        assertTrue("IME process stopped", shell("pidof $imePackage").isEmpty())
        assertEquals("Room survives real process death", learned, learning())
        assertEquals(preferences, shell("run-as $imePackage cat shared_prefs/keyboard.xml"))
        // Re-selecting clears force-stop state without touching user data.
        shell("ime set $ime")
        device.pressHome()
        instrumentation.targetContext.startActivity(Intent(instrumentation.targetContext, EditorActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        await("host refocused") { EditorActivity.current != null }; focus()
        assertTrue("new IME process", shell("pidof $imePackage").let { it.isNotEmpty() && it != pid })
        find(By.pkg(imePackage).desc("ㄋ"), "saved Zhuyin mode active", 180000)
        val expected = text(); type("ㄋㄧˇㄏㄠˇ"); candidate("你好"); expect(expected + "你好")
        // Probe in-memory English preference independently through another fresh editor.
        key("Switch to English"); launch(); type("hello"); expect("hello")
    }
}
