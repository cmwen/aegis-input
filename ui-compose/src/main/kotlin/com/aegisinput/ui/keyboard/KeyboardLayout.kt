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

    val qwertyRows: List<List<KeyDef>> = listOf(
        listOf(
            KeyDef("q"), KeyDef("w"), KeyDef("e"), KeyDef("r"), KeyDef("t"),
            KeyDef("y"), KeyDef("u"), KeyDef("i"), KeyDef("o"), KeyDef("p")
        ),
        listOf(
            KeyDef("a"), KeyDef("s"), KeyDef("d"), KeyDef("f"), KeyDef("g"),
            KeyDef("h"), KeyDef("j"), KeyDef("k"), KeyDef("l")
        ),
        listOf(
            KeyDef("⇧", "SHIFT", 1.5f, KeyType.MODIFIER),
            KeyDef("z"), KeyDef("x"), KeyDef("c"), KeyDef("v"),
            KeyDef("b"), KeyDef("n"), KeyDef("m"),
            KeyDef("⌫", "BACKSPACE", 1.5f, KeyType.BACKSPACE)
        ),
        listOf(
            KeyDef("123", "SYMBOLS", 1.5f, KeyType.MODIFIER),
            KeyDef(","), KeyDef(" ", "SPACE", 4f, KeyType.SPACE),
            KeyDef("."),
            KeyDef("↵", "ENTER", 1.5f, KeyType.ENTER)
        )
    )

    val zhuyinRows: List<List<KeyDef>> = listOf(
        listOf(
            KeyDef("ㄅ"), KeyDef("ㄉ"), KeyDef("ˇ"), KeyDef("ˋ"), KeyDef("ㄓ"),
            KeyDef("ˊ"), KeyDef("˙"), KeyDef("ㄚ"), KeyDef("ㄞ"), KeyDef("ㄢ")
        ),
        listOf(
            KeyDef("ㄆ"), KeyDef("ㄊ"), KeyDef("ㄍ"), KeyDef("ㄐ"), KeyDef("ㄔ"),
            KeyDef("ㄗ"), KeyDef("ㄧ"), KeyDef("ㄛ"), KeyDef("ㄟ"), KeyDef("ㄣ")
        ),
        listOf(
            KeyDef("ㄇ"), KeyDef("ㄋ"), KeyDef("ㄎ"), KeyDef("ㄑ"), KeyDef("ㄕ"),
            KeyDef("ㄘ"), KeyDef("ㄨ"), KeyDef("ㄜ"), KeyDef("ㄠ"), KeyDef("ㄤ")
        ),
        listOf(
            KeyDef("ㄈ"), KeyDef("ㄌ"), KeyDef("ㄏ"), KeyDef("ㄒ"), KeyDef("ㄖ"),
            KeyDef("ㄙ"), KeyDef("ㄩ"), KeyDef("ㄝ"), KeyDef("ㄡ"), KeyDef("ㄥ")
        ),
        listOf(
            KeyDef("ABC", "LATIN", 1.5f, KeyType.MODIFIER),
            KeyDef(" ", "SPACE", 3f, KeyType.SPACE),
            KeyDef("⌫", "BACKSPACE", 1.5f, KeyType.BACKSPACE),
            KeyDef("↵", "ENTER", 1.5f, KeyType.ENTER)
        )
    )
}
