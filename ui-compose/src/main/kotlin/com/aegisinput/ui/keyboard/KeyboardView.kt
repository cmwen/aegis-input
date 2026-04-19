package com.aegisinput.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
    PINYIN,
    ZHUYIN,
    SYMBOLS
}

@Composable
fun KeyboardView(
    keyboardMode: KeyboardMode,
    chineseMode: KeyboardMode,
    onKeyboardModeChange: (KeyboardMode) -> Unit,
    onKeyPress: (String) -> Unit,
    onCandidateSelected: (String) -> Unit,
    candidates: List<String>,
    modifier: Modifier = Modifier
) {
    var shiftEnabled by remember(keyboardMode) { mutableStateOf(false) }
    val rows = KeyboardLayout.rowsFor(
        mode = keyboardMode,
        chineseMode = chineseMode,
        uppercaseLatin = shiftEnabled && keyboardMode != KeyboardMode.ZHUYIN
    )

    AegisInputTheme {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            CandidateBar(
                candidates = candidates,
                onCandidateSelected = onCandidateSelected
            )

            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    row.forEach { keyDef ->
                        KeyButton(
                            keyDef = keyDef,
                            onPress = { key ->
                                when (key.code) {
                                    "MODE_LATIN" -> onKeyboardModeChange(KeyboardMode.LATIN)
                                    "MODE_CHINESE" -> onKeyboardModeChange(chineseMode)
                                    "SYMBOLS" -> onKeyboardModeChange(KeyboardMode.SYMBOLS)
                                    "SHIFT" -> shiftEnabled = !shiftEnabled
                                    else -> {
                                        onKeyPress(key.code)
                                        if (shiftEnabled && keyboardMode == KeyboardMode.LATIN) {
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
