package com.aegisinput.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aegisinput.ui.candidate.CandidateBar
import com.aegisinput.ui.theme.AegisInputTheme

enum class KeyboardMode {
    LATIN,
    COMMANDS,
    PINYIN,
    ZHUYIN,
    SYMBOLS
}

object CommandPalette {
    val defaultQuickCommands: List<String> = listOf(
        "/help",
        "/new",
        "/search",
        "/settings"
    )
}

private val zhuyinHints = mapOf(
    "ㄅ" to "1", "ㄉ" to "2", "ˇ" to "3", "ˋ" to "4",
    "ㄆ" to "q", "ㄊ" to "w", "ㄍ" to "e", "ㄐ" to "r",
    "ㄇ" to "a", "ㄋ" to "s", "ㄎ" to "d", "ㄑ" to "f",
    "ㄈ" to "z", "ㄌ" to "x", "ㄏ" to "c", "ㄒ" to "v",
    "ㄓ/ㄗ" to "5/y", "ㄔ/ㄘ" to "t/h", "ㄕ/ㄙ" to "g/n", "ㄖ" to "b",
    "ㄧ" to "u", "ㄨ" to "j", "ㄩ" to "m",
    "ㄚ" to "8", "ㄛ" to "i", "ㄜ" to "k", "ㄝ" to ",", "ㄞ" to "9", "ㄟ" to "o", "ㄠ" to "l",
    "ㄡ" to ".", "ㄦ" to "-", "ㄢ/ㄤ" to "0/;", "ㄣ/ㄥ" to "p//",
    "ㄧㄝ" to "u,", "ㄨㄛ" to "ji", "ㄧㄢ/ㄤ" to "u0", "ㄨㄢ/ㄤ" to "j0",
    "ㄧㄣ/ㄥ" to "up", "ㄨㄣ/ㄥ" to "jp"
)

@Composable
fun KeyboardView(
    keyboardMode: KeyboardMode,
    chineseMode: KeyboardMode,
    onKeyboardModeChange: (KeyboardMode) -> Unit,
    onKeyPress: (String) -> Unit,
    onCandidateSelected: (String) -> Unit,
    candidates: List<String>,
    composingText: String = "",
    quickCommandSuggestions: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    var shiftEnabled by remember(keyboardMode) { mutableStateOf(false) }
    var zhuyinPage by remember(keyboardMode) { mutableStateOf(1) }

    LaunchedEffect(composingText) {
        if (composingText.isEmpty()) {
            zhuyinPage = 1
        } else if (zhuyinPage == 1 && composingText.isNotEmpty()) {
            zhuyinPage = 2
        }
    }

    val visibleCandidates = if (candidates.isNotEmpty()) {
        candidates
    } else if (keyboardMode == KeyboardMode.COMMANDS) {
        quickCommandSuggestions
    } else {
        emptyList()
    }
    val rows = KeyboardLayout.rowsFor(
        mode = keyboardMode,
        chineseMode = chineseMode,
        uppercaseLatin = shiftEnabled && keyboardMode.supportsShift(),
        zhuyinPage = zhuyinPage
    )

    AegisInputTheme {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            CandidateBar(
                candidates = visibleCandidates,
                onCandidateSelected = onCandidateSelected
            )

            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    row.forEach { keyDef ->
                        val hint = if (keyboardMode == KeyboardMode.ZHUYIN) {
                            zhuyinHints[keyDef.label]
                        } else null

                        KeyButton(
                            keyDef = keyDef,
                            hintLabel = hint,
                            onPress = { key ->
                                when (key.code) {
                                    "MODE_LATIN" -> onKeyboardModeChange(KeyboardMode.LATIN)
                                    "MODE_COMMANDS" -> onKeyboardModeChange(KeyboardMode.COMMANDS)
                                    "MODE_CHINESE" -> onKeyboardModeChange(chineseMode)
                                    "SYMBOLS" -> onKeyboardModeChange(KeyboardMode.SYMBOLS)
                                    "SHIFT" -> shiftEnabled = !shiftEnabled
                                    "TOGGLE_PAGE" -> {
                                        zhuyinPage = if (zhuyinPage == 1) 2 else 1
                                    }
                                    else -> {
                                        onKeyPress(key.code)
                                        if (shiftEnabled && keyboardMode.supportsShift()) {
                                            shiftEnabled = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(keyDef.widthWeight)
                        )
                    }
                }
            }
        }
    }
}

private fun KeyboardMode.supportsShift(): Boolean {
    return this == KeyboardMode.LATIN || this == KeyboardMode.PINYIN
}
