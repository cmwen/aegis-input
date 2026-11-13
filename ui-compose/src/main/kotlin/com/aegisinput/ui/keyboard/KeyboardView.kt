package com.aegisinput.ui.keyboard

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onCandidateSelected: (Int, String) -> Unit,
    candidates: List<String>,
    composingText: String = "",
    quickCommandSuggestions: List<String> = emptyList(),
    onHideKeyboard: () -> Unit = {},
    onSwitchKeyboard: () -> Unit = {},
    modifier: Modifier = Modifier,
    chineseEnabled: Boolean = true,
    statusMessage: String = "",
    enterLabel: String = "↵"
) {
    var shiftEnabled by remember(keyboardMode) { mutableStateOf(false) }
    var zhuyinPage by remember(keyboardMode) { mutableStateOf(1) }

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
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = composingText.ifEmpty { statusMessage.ifEmpty { when(keyboardMode) { KeyboardMode.PINYIN -> "拼音 · 离线"; KeyboardMode.ZHUYIN -> "注音 · 離線"; else -> "English · QWERTY" } } },
                    color = if (composingText.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                    fontSize = if (composingText.isEmpty()) 12.sp else 18.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                if (chineseEnabled) {
                    Text("拼音", fontSize = 14.sp, modifier = Modifier.padding(8.dp).clickable { onKeyboardModeChange(KeyboardMode.PINYIN) })
                    Text("注音", fontSize = 14.sp, modifier = Modifier.padding(8.dp).clickable { onKeyboardModeChange(KeyboardMode.ZHUYIN) })
                }
            }
            CandidateBar(
                candidates = visibleCandidates,
                onCandidateSelected = onCandidateSelected
            )

            fun press(key: KeyDef) {
                when (key.code) {
                    "MODE_LATIN" -> onKeyboardModeChange(KeyboardMode.LATIN)
                    "MODE_COMMANDS" -> onKeyboardModeChange(KeyboardMode.COMMANDS)
                    "MODE_CHINESE" -> if (chineseEnabled) onKeyboardModeChange(chineseMode)
                    "SYMBOLS" -> onKeyboardModeChange(KeyboardMode.SYMBOLS)
                    "SHIFT" -> { shiftEnabled = !shiftEnabled }
                    else -> {
                        onKeyPress(key.code)
                        if (shiftEnabled && keyboardMode.supportsShift()) shiftEnabled = false
                    }
                }
            }
            rows.forEachIndexed { rowIndex, row ->
                val bounds = remember(row) { mutableStateMapOf<String, DynamicHitbox.KeyBounds>() }
                val touchWeights = remember(keyboardMode) { mutableStateMapOf<String, Float>() }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (rowIndex == 1 && keyboardMode != KeyboardMode.ZHUYIN) 18.dp else 4.dp)
                        .pointerInput(row, chineseEnabled) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                down.consume()
                                val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                                if (up != null) {
                                    up.consume()
                                    DynamicHitbox.resolveKey(up.position, bounds.values.toList(), touchWeights)?.let { key ->
                                        if (key.code != "MODE_CHINESE" || chineseEnabled) {
                                            touchWeights[key.code] = ((touchWeights[key.code] ?: 1f) + 0.01f).coerceAtMost(1.15f)
                                            press(key)
                                        }
                                    }
                                }
                            }
                        }
                ) {
                    row.forEach { original ->
                        val keyDef = when {
                            original.code == "ENTER" -> original.copy(label = enterLabel)
                            original.code == "SHIFT" && shiftEnabled -> original.copy(label = "⬆")
                            else -> original
                        }
                        KeyButton(
                            keyDef = keyDef,
                            enabled = keyDef.code != "MODE_CHINESE" || chineseEnabled,
                            onPress = ::press,
                            onBoundsChanged = { bounds[it.keyDef.code] = it },
                            modifier = Modifier.weight(keyDef.widthWeight)
                        )
                    }
                }
            }

            SystemControlBar(
                onHideKeyboard = onHideKeyboard,
                onSwitchKeyboard = onSwitchKeyboard
            )
        }
    }
}

@Composable
fun SystemControlBar(
    onHideKeyboard: () -> Unit,
    onSwitchKeyboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(32.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Switch Keyboard button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(48.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable { onSwitchKeyboard() }
        ) {
            Text(
                text = "🌐",
                fontSize = 18.sp
            )
        }

        // Hide Keyboard button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(48.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable { onHideKeyboard() }
        ) {
            Text(
                text = "▼",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun KeyboardMode.supportsShift(): Boolean {
    return this == KeyboardMode.LATIN
}
