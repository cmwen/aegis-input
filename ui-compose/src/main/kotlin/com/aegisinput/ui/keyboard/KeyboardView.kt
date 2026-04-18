package com.aegisinput.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aegisinput.ui.candidate.CandidateBar
import com.aegisinput.ui.theme.AegisInputTheme

enum class KeyboardMode {
    QWERTY,
    ZHUYIN,
    SYMBOLS
}

@Composable
fun KeyboardView(
    onKeyPress: (String) -> Unit,
    onCandidateSelected: (String) -> Unit,
    candidates: List<String>,
    modifier: Modifier = Modifier
) {
    var currentMode by remember { mutableStateOf(KeyboardMode.QWERTY) }
    val keyBoundsList = remember { mutableStateListOf<DynamicHitbox.KeyBounds>() }

    val rows = when (currentMode) {
        KeyboardMode.QWERTY -> KeyboardLayout.qwertyRows
        KeyboardMode.ZHUYIN -> KeyboardLayout.zhuyinRows
        KeyboardMode.SYMBOLS -> KeyboardLayout.qwertyRows // placeholder
    }

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
                                    "LATIN" -> currentMode = KeyboardMode.QWERTY
                                    "SYMBOLS" -> currentMode = KeyboardMode.SYMBOLS
                                    else -> onKeyPress(key.code)
                                }
                            },
                            modifier = Modifier.weight(keyDef.widthWeight),
                            onBoundsChanged = { bounds ->
                                keyBoundsList.removeAll { it.keyDef.code == bounds.keyDef.code }
                                keyBoundsList.add(bounds)
                            }
                        )
                    }
                }
            }
        }
    }
}
