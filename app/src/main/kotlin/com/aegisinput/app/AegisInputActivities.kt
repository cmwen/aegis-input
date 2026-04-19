package com.aegisinput.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aegisinput.engine.RimeBridge
import com.aegisinput.engine.RimeSession
import com.aegisinput.ui.keyboard.KeyboardMode
import com.aegisinput.ui.keyboard.KeyboardView
import com.aegisinput.ui.theme.AegisInputTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AegisInputTheme {
                AegisSetupScreen(
                    title = getString(R.string.launcher_title),
                    subtitle = getString(R.string.launcher_subtitle),
                    onOpenKeyboardSettings = {
                        startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                    },
                    onShowInputPicker = { showInputMethodPicker() }
                )
            }
        }
    }

    private fun showInputMethodPicker() {
        (getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager)?.showInputMethodPicker()
    }
}

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AegisInputTheme {
                AegisSetupScreen(
                    title = getString(R.string.settings_title),
                    subtitle = getString(R.string.settings_subtitle),
                    onOpenKeyboardSettings = {
                        startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                    },
                    onShowInputPicker = { showInputMethodPicker() }
                )
            }
        }
    }

    private fun showInputMethodPicker() {
        (getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager)?.showInputMethodPicker()
    }
}

@Composable
private fun AegisSetupScreen(
    title: String,
    subtitle: String,
    onOpenKeyboardSettings: () -> Unit,
    onShowInputPicker: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Get started",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Enable AegisInput, then switch to it from any text field to type with Zhuyin or Pinyin.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onOpenKeyboardSettings,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Enable keyboard")
                        }
                        OutlinedButton(
                            onClick = onShowInputPicker,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Switch keyboard")
                        }
                    }
                }
            }

            FeatureSummaryCard()
            KeyboardDemoCard()
        }
    }
}

@Composable
private fun FeatureSummaryCard() {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "MVP features",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            FeatureRow("Zhuyin layout", "Traditional Chinese phonetic input with candidate selection.")
            FeatureRow("Pinyin layout", "Latin-keyboard input for simplified Chinese workflows.")
            FeatureRow("Latin + symbols", "Quick fallback for English text, digits, and punctuation.")
            FeatureRow("Privacy-first", "All composing stays on-device with no network permissions.")
        }
    }
}

@Composable
private fun FeatureRow(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun KeyboardDemoCard() {
    var chineseMode by remember { mutableStateOf(KeyboardMode.ZHUYIN) }
    var keyboardMode by remember { mutableStateOf(KeyboardMode.ZHUYIN) }
    var committedText by remember { mutableStateOf("") }
    var composingText by remember { mutableStateOf("") }
    val candidates = remember { mutableStateListOf<String>() }
    var session by remember { mutableStateOf<RimeSession?>(null) }

    DisposableEffect(Unit) {
        val demoSession = RimeBridge.createSession()
        session = demoSession
        onDispose {
            RimeBridge.destroySession(demoSession)
        }
    }

    fun clearComposition(resetSession: Boolean = true) {
        if (resetSession) {
            session?.reset()
        }
        composingText = ""
        candidates.clear()
    }

    fun syncFromSession() {
        val activeSession = session ?: return
        composingText = activeSession.composingText
        candidates.clear()
        candidates.addAll(activeSession.candidates)
    }

    fun commitCandidate(candidate: String) {
        committedText += candidate
        clearComposition()
    }

    fun handleKeyPress(key: String) {
        val activeSession = session ?: return
        when (key) {
            "BACKSPACE" -> {
                if (keyboardMode.isChineseMode() && activeSession.hasComposing()) {
                    activeSession.processKey("BackSpace")
                    syncFromSession()
                } else if (committedText.isNotEmpty()) {
                    committedText = committedText.dropLast(1)
                }
            }
            "SPACE" -> {
                if (keyboardMode.isChineseMode() && candidates.isNotEmpty()) {
                    commitCandidate(candidates.first())
                } else {
                    committedText += " "
                }
            }
            "ENTER" -> {
                if (keyboardMode.isChineseMode() && activeSession.hasComposing()) {
                    val committed = activeSession.commit()
                    committedText += committed
                    clearComposition(resetSession = false)
                } else {
                    committedText += "\n"
                }
            }
            else -> {
                if (keyboardMode.isChineseMode()) {
                    activeSession.processKey(
                        if (chineseMode == KeyboardMode.PINYIN) key.lowercase() else key
                    )
                    syncFromSession()
                } else {
                    committedText += key
                }
            }
        }
    }

    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Try the keyboard",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Preview the input flows before enabling the IME. In the current stub engine, candidates mirror the composing sequence.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DemoModeButton(
                    label = "Zhuyin",
                    selected = chineseMode == KeyboardMode.ZHUYIN,
                    onClick = {
                        chineseMode = KeyboardMode.ZHUYIN
                        if (keyboardMode.isChineseMode()) {
                            keyboardMode = chineseMode
                        }
                        clearComposition()
                    }
                )
                DemoModeButton(
                    label = "Pinyin",
                    selected = chineseMode == KeyboardMode.PINYIN,
                    onClick = {
                        chineseMode = KeyboardMode.PINYIN
                        if (keyboardMode.isChineseMode()) {
                            keyboardMode = chineseMode
                        }
                        clearComposition()
                    }
                )
                DemoModeButton(
                    label = "Clear",
                    selected = false,
                    onClick = {
                        committedText = ""
                        clearComposition()
                    }
                )
            }
            DemoTextSurface(
                committedText = committedText,
                composingText = composingText
            )
            KeyboardView(
                keyboardMode = keyboardMode,
                chineseMode = chineseMode,
                onKeyboardModeChange = { keyboardMode = it },
                onKeyPress = { key -> handleKeyPress(key) },
                onCandidateSelected = { candidate -> commitCandidate(candidate) },
                candidates = candidates,
                modifier = Modifier.widthIn(max = 640.dp)
            )
        }
    }
}

@Composable
private fun DemoModeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    if (selected) {
        Button(onClick = onClick) {
            Text(text = label)
        }
    } else {
        OutlinedButton(onClick = onClick) {
            Text(text = label)
        }
    }
}

@Composable
private fun DemoTextSurface(
    committedText: String,
    composingText: String
) {
    val displayText = buildString {
        append(committedText)
        if (composingText.isNotEmpty()) {
            append('[')
            append(composingText)
            append(']')
        }
        if (isEmpty()) {
            append("Tap the keys below to preview input.")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun KeyboardMode.isChineseMode(): Boolean {
    return this == KeyboardMode.PINYIN || this == KeyboardMode.ZHUYIN
}
