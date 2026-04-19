package com.aegisinput.app

import com.aegisinput.ui.keyboard.KeyboardMode

internal data class KeyboardDemoState(
    val chineseMode: KeyboardMode = KeyboardMode.ZHUYIN,
    val keyboardMode: KeyboardMode = KeyboardMode.ZHUYIN,
    val committedText: String = "",
    val composingText: String = "",
    val candidates: List<String> = emptyList()
)

internal fun KeyboardDemoState.setChineseMode(mode: KeyboardMode): KeyboardDemoState {
    val nextKeyboardMode = if (keyboardMode.isChineseMode()) mode else keyboardMode
    return copy(chineseMode = mode, keyboardMode = nextKeyboardMode).clearComposition()
}

internal fun KeyboardDemoState.setKeyboardMode(mode: KeyboardMode): KeyboardDemoState {
    return copy(keyboardMode = mode)
}

internal fun KeyboardDemoState.clearAll(): KeyboardDemoState {
    return copy(committedText = "").clearComposition()
}

internal fun KeyboardDemoState.commitCandidate(candidate: String): KeyboardDemoState {
    return copy(committedText = committedText + candidate).clearComposition()
}

internal fun KeyboardDemoState.handleKeyPress(key: String): KeyboardDemoState {
    return when (key) {
        "BACKSPACE" -> {
            if (keyboardMode.isChineseMode() && composingText.isNotEmpty()) {
                updateComposition(composingText.dropLast(1))
            } else if (committedText.isNotEmpty()) {
                copy(committedText = committedText.dropLast(1))
            } else {
                this
            }
        }

        "SPACE" -> {
            if (keyboardMode.isChineseMode() && candidates.isNotEmpty()) {
                commitCandidate(candidates.first())
            } else {
                copy(committedText = committedText + " ")
            }
        }

        "ENTER" -> {
            if (keyboardMode.isChineseMode() && composingText.isNotEmpty()) {
                copy(committedText = committedText + composingText).clearComposition()
            } else {
                copy(committedText = committedText + "\n")
            }
        }

        else -> {
            if (keyboardMode.isChineseMode()) {
                val normalizedKey = if (chineseMode == KeyboardMode.PINYIN) {
                    key.lowercase()
                } else {
                    key
                }
                updateComposition(composingText + normalizedKey)
            } else {
                copy(committedText = committedText + key)
            }
        }
    }
}

internal fun KeyboardMode.isChineseMode(): Boolean {
    return this == KeyboardMode.PINYIN || this == KeyboardMode.ZHUYIN
}

private fun KeyboardDemoState.clearComposition(): KeyboardDemoState {
    return copy(composingText = "", candidates = emptyList())
}

private fun KeyboardDemoState.updateComposition(composition: String): KeyboardDemoState {
    val nextCandidates = if (composition.isEmpty()) emptyList() else listOf(composition)
    return copy(composingText = composition, candidates = nextCandidates)
}
