package com.aegisinput.ui.keyboard

data class KeyDef(
    val label: String,
    val code: String = label,
    val widthWeight: Float = 1f,
    val type: KeyType = KeyType.CHARACTER
)

enum class KeyType {
    CHARACTER,
    MODIFIER,
    SPACE,
    ENTER,
    BACKSPACE
}

object KeyboardLayout {

    fun rowsFor(
        mode: KeyboardMode,
        chineseMode: KeyboardMode,
        uppercaseLatin: Boolean,
        zhuyinPage: Int = 1
    ): List<List<KeyDef>> {
        return when (mode) {
            KeyboardMode.LATIN -> latinRows(chineseMode, uppercaseLatin)
            KeyboardMode.COMMANDS -> commandRows(chineseMode)
            KeyboardMode.PINYIN -> pinyinRows(uppercaseLatin)
            KeyboardMode.ZHUYIN -> if (zhuyinPage == 2) zhuyinPage2Rows() else zhuyinPage1Rows()
            KeyboardMode.SYMBOLS -> symbolRows(chineseMode)
        }
    }

    private fun latinRows(chineseMode: KeyboardMode, uppercaseLatin: Boolean): List<List<KeyDef>> {
        return qwertyRows(
            leadingSwitch = KeyDef(
                label = if (chineseMode == KeyboardMode.ZHUYIN) "注音" else "拼音",
                code = "MODE_CHINESE",
                widthWeight = 1.5f,
                type = KeyType.MODIFIER
            ),
            uppercaseLatin = uppercaseLatin
        )
    }

    private fun commandRows(chineseMode: KeyboardMode): List<List<KeyDef>> = listOf(
        listOf(
            KeyDef("q"), KeyDef("w"), KeyDef("e"), KeyDef("r"), KeyDef("t"),
            KeyDef("y"), KeyDef("u"), KeyDef("i"), KeyDef("o"), KeyDef("p")
        ),
        listOf(
            KeyDef("a"), KeyDef("s"), KeyDef("d"), KeyDef("f"), KeyDef("g"),
            KeyDef("h"), KeyDef("j"), KeyDef("k"), KeyDef("l")
        ),
        listOf(
            KeyDef("/"),
            KeyDef("-"),
            KeyDef("z"), KeyDef("x"), KeyDef("c"), KeyDef("v"),
            KeyDef("b"), KeyDef("n"), KeyDef("m"),
            KeyDef("⌫", "BACKSPACE", 1.5f, KeyType.BACKSPACE)
        ),
        listOf(
            KeyDef(
                label = if (chineseMode == KeyboardMode.ZHUYIN) "注音" else "拼音",
                code = "MODE_CHINESE",
                widthWeight = 1.2f,
                type = KeyType.MODIFIER
            ),
            KeyDef("ABC", "MODE_LATIN", 1.1f, KeyType.MODIFIER),
            KeyDef("123", "SYMBOLS", 1.1f, KeyType.MODIFIER),
            KeyDef(" ", "SPACE", 2.9f, KeyType.SPACE),
            KeyDef(".", ".", 0.9f, KeyType.CHARACTER),
            KeyDef("↵", "ENTER", 1.3f, KeyType.ENTER)
        )
    )

    private fun pinyinRows(uppercaseLatin: Boolean): List<List<KeyDef>> {
        return qwertyRows(
            leadingSwitch = KeyDef("ABC", "MODE_LATIN", 1.5f, KeyType.MODIFIER),
            uppercaseLatin = uppercaseLatin
        )
    }

    private fun qwertyRows(
        leadingSwitch: KeyDef,
        uppercaseLatin: Boolean
    ): List<List<KeyDef>> = listOf(
        listOf(
            letterKey("q", uppercaseLatin),
            letterKey("w", uppercaseLatin),
            letterKey("e", uppercaseLatin),
            letterKey("r", uppercaseLatin),
            letterKey("t", uppercaseLatin),
            letterKey("y", uppercaseLatin),
            letterKey("u", uppercaseLatin),
            letterKey("i", uppercaseLatin),
            letterKey("o", uppercaseLatin),
            letterKey("p", uppercaseLatin)
        ),
        listOf(
            letterKey("a", uppercaseLatin),
            letterKey("s", uppercaseLatin),
            letterKey("d", uppercaseLatin),
            letterKey("f", uppercaseLatin),
            letterKey("g", uppercaseLatin),
            letterKey("h", uppercaseLatin),
            letterKey("j", uppercaseLatin),
            letterKey("k", uppercaseLatin),
            letterKey("l", uppercaseLatin)
        ),
        listOf(
            KeyDef("⇧", "SHIFT", 1.5f, KeyType.MODIFIER),
            letterKey("z", uppercaseLatin),
            letterKey("x", uppercaseLatin),
            letterKey("c", uppercaseLatin),
            letterKey("v", uppercaseLatin),
            letterKey("b", uppercaseLatin),
            letterKey("n", uppercaseLatin),
            letterKey("m", uppercaseLatin),
            KeyDef("⌫", "BACKSPACE", 1.5f, KeyType.BACKSPACE)
        ),
        listOf(
            leadingSwitch,
            KeyDef("CMD", "MODE_COMMANDS", 1.1f, KeyType.MODIFIER),
            KeyDef("123", "SYMBOLS", 1.2f, KeyType.MODIFIER),
            KeyDef(" ", "SPACE", 2.5f, KeyType.SPACE),
            KeyDef(".", ".", 0.9f, KeyType.CHARACTER),
            KeyDef("↵", "ENTER", 1.3f, KeyType.ENTER)
        )
    )

    private fun zhuyinPage1Rows(): List<List<KeyDef>> = listOf(
        listOf(
            KeyDef("ㄅ"), KeyDef("ㄆ"), KeyDef("ㄇ"), KeyDef("ㄈ"), KeyDef("ㄉ"), KeyDef("ㄊ"), KeyDef("ㄋ")
        ),
        listOf(
            KeyDef("ㄌ"), KeyDef("ㄍ"), KeyDef("ㄎ"), KeyDef("ㄏ"), KeyDef("ㄐ"), KeyDef("ㄑ"), KeyDef("ㄒ")
        ),
        listOf(
            KeyDef("ㄓ/ㄗ", "ㄓ_ㄗ"), KeyDef("ㄔ/ㄘ", "ㄔ_ㄘ"), KeyDef("ㄕ/ㄙ", "ㄕ_ㄙ"), KeyDef("ㄖ"),
            KeyDef("ㄧ"), KeyDef("ㄨ"), KeyDef("ㄩ")
        ),
        listOf(
            KeyDef("ABC", "MODE_LATIN", 1.1f, KeyType.MODIFIER),
            KeyDef("⌫", "BACKSPACE", 1.1f, KeyType.BACKSPACE),
            KeyDef(" ", "SPACE", 2.4f, KeyType.SPACE),
            KeyDef("⇋", "TOGGLE_PAGE", 1.1f, KeyType.MODIFIER),
            KeyDef("↵", "ENTER", 1.3f, KeyType.ENTER)
        )
    )

    private fun zhuyinPage2Rows(): List<List<KeyDef>> = listOf(
        listOf(
            KeyDef("ㄚ"), KeyDef("ㄛ"), KeyDef("ㄜ"), KeyDef("ㄝ"), KeyDef("ㄞ"), KeyDef("ㄟ"), KeyDef("ㄠ")
        ),
        listOf(
            KeyDef("ㄡ"), KeyDef("ㄦ"), KeyDef("ㄢ/ㄤ", "ㄢ_ㄤ"), KeyDef("ㄣ/ㄥ", "ㄣ_ㄥ"),
            KeyDef("ㄧㄝ", "ㄧㄝ"), KeyDef("ㄨㄛ", "ㄨㄛ"), KeyDef("ˊ")
        ),
        listOf(
            KeyDef("ㄧㄢ/ㄤ", "ㄧㄢ_ㄤ"), KeyDef("ㄨㄢ/ㄤ", "ㄨㄢ_ㄤ"),
            KeyDef("ㄧㄣ/ㄥ", "ㄧㄣ_ㄥ"), KeyDef("ㄨㄣ/ㄥ", "ㄨㄣ_ㄥ"),
            KeyDef("ˇ"), KeyDef("ˋ"), KeyDef("˙")
        ),
        listOf(
            KeyDef("ABC", "MODE_LATIN", 1.1f, KeyType.MODIFIER),
            KeyDef("⌫", "BACKSPACE", 1.1f, KeyType.BACKSPACE),
            KeyDef(" ", "SPACE", 2.4f, KeyType.SPACE),
            KeyDef("⇋", "TOGGLE_PAGE", 1.1f, KeyType.MODIFIER),
            KeyDef("↵", "ENTER", 1.3f, KeyType.ENTER)
        )
    )

    private fun symbolRows(chineseMode: KeyboardMode): List<List<KeyDef>> = listOf(
        listOf(
            KeyDef("1"), KeyDef("2"), KeyDef("3"), KeyDef("4"), KeyDef("5"),
            KeyDef("6"), KeyDef("7"), KeyDef("8"), KeyDef("9"), KeyDef("0")
        ),
        listOf(
            KeyDef("@"), KeyDef("#"), KeyDef("$"), KeyDef("%"), KeyDef("&"),
            KeyDef("-"), KeyDef("+"), KeyDef("("), KeyDef(")")
        ),
        listOf(
            KeyDef("*"), KeyDef("\""), KeyDef("'"), KeyDef(":"), KeyDef(";"),
            KeyDef("!"), KeyDef("?"), KeyDef("/"),
            KeyDef("⌫", "BACKSPACE", 1.5f, KeyType.BACKSPACE)
        ),
        listOf(
            KeyDef(
                label = if (chineseMode == KeyboardMode.ZHUYIN) "注音" else "拼音",
                code = "MODE_CHINESE",
                widthWeight = 1.3f,
                type = KeyType.MODIFIER
            ),
            KeyDef("ABC", "MODE_LATIN", 1.2f, KeyType.MODIFIER),
            KeyDef(",", ",", 0.9f, KeyType.CHARACTER),
            KeyDef(" ", "SPACE", 2.8f, KeyType.SPACE),
            KeyDef(".", ".", 0.9f, KeyType.CHARACTER),
            KeyDef("↵", "ENTER", 1.3f, KeyType.ENTER)
        )
    )

    private fun letterKey(letter: String, uppercaseLatin: Boolean): KeyDef {
        val output = if (uppercaseLatin) letter.uppercase() else letter
        return KeyDef(output)
    }
}
