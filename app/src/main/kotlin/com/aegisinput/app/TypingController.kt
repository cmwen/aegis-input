package com.aegisinput.app

/** Input behavior shared by every editor; independent of Android for regression tests. */
internal interface ChineseComposer {
    val text: String
    val candidates: List<String>
    fun key(key: String)
    fun select(index: Int): String
    fun reset()
}

internal interface TextEditor {
    fun compose(text: String)
    fun commit(text: String)
    fun delete()
    fun enter()
}

internal class TypingController(
    private val editor: TextEditor,
    private val composer: ChineseComposer?
) {
    fun key(key: String, chinese: Boolean) {
        val active = if (chinese) composer else null
        when (key) {
            "BACKSPACE" -> if (active?.text?.isNotEmpty() == true) {
                active.key("BackSpace")
                editor.compose(active.text)
            } else editor.delete()
            "SPACE" -> if (active?.text?.isNotEmpty() == true) {
                if (active.candidates.isNotEmpty()) select(0) else commitRaw()
            } else editor.commit(" ")
            "ENTER" -> if (active?.text?.isNotEmpty() == true) commitRaw() else editor.enter()
            else -> if (active != null && key.length == 1 && (key[0].isLetter() || key == "'")) {
                active.key(key.lowercase())
                editor.compose(active.text)
            } else {
                if (active?.text?.isNotEmpty() == true) {
                    if (active.candidates.isNotEmpty()) select(0) else commitRaw()
                }
                editor.commit(key)
            }
        }
    }

    fun select(index: Int) {
        val active = composer ?: return
        if (index !in active.candidates.indices || active.text.isEmpty()) return
        val committed = active.select(index)
        if (committed.isNotEmpty()) editor.commit(committed)
    }

    /** Keep visible Roman text when leaving Chinese mode; never leave a hidden composition. */
    fun commitRaw() {
        val active = composer ?: return
        val raw = active.text
        active.reset()
        if (raw.isNotEmpty()) editor.commit(raw)
    }
}

internal fun com.aegisinput.ui.keyboard.KeyboardMode.isChineseMode(): Boolean =
    this == com.aegisinput.ui.keyboard.KeyboardMode.PINYIN || this == com.aegisinput.ui.keyboard.KeyboardMode.ZHUYIN
