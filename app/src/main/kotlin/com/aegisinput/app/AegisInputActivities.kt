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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aegisinput.engine.RimeBridge
import com.aegisinput.ui.keyboard.CommandPalette
import com.aegisinput.ui.keyboard.KeyboardMode
import com.aegisinput.ui.keyboard.KeyboardView
import com.aegisinput.ui.theme.AegisInputTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val nativeEngineWarningMessage = RimeBridge.unavailableMessage()
        setContent {
            AegisInputTheme {
                AegisSetupScreen(
                    title = getString(R.string.launcher_title),
                    subtitle = getString(R.string.launcher_subtitle),
                    nativeEngineWarningMessage = nativeEngineWarningMessage,
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
        val nativeEngineWarningMessage = RimeBridge.unavailableMessage()
        setContent {
            AegisInputTheme {
                AegisSetupScreen(
                    title = getString(R.string.settings_title),
                    subtitle = getString(R.string.settings_subtitle),
                    nativeEngineWarningMessage = nativeEngineWarningMessage,
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
    nativeEngineWarningMessage: String?,
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

            nativeEngineWarningMessage?.let { message ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.native_engine_unavailable_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = stringResource(R.string.native_engine_unavailable_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
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
            FeatureRow("Command palette", "Lowercase-first slash-command mode with quick inserts for chat, AI, and productivity fields.")
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
    var demoState by remember { mutableStateOf(KeyboardDemoState()) }

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
                text = "Preview the input flows before enabling the IME. This local demo mirrors the composing sequence without starting a live input session.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DemoModeButton(
                    label = "Zhuyin",
                    selected = demoState.chineseMode == KeyboardMode.ZHUYIN,
                    onClick = {
                        demoState = demoState.setChineseMode(KeyboardMode.ZHUYIN)
                    }
                )
                DemoModeButton(
                    label = "Pinyin",
                    selected = demoState.chineseMode == KeyboardMode.PINYIN,
                    onClick = {
                        demoState = demoState.setChineseMode(KeyboardMode.PINYIN)
                    }
                )
                DemoModeButton(
                    label = "Clear",
                    selected = false,
                    onClick = {
                        demoState = demoState.clearAll()
                    }
                )
                DemoModeButton(
                    label = "Cmd",
                    selected = demoState.keyboardMode == KeyboardMode.COMMANDS,
                    onClick = {
                        demoState = demoState.setKeyboardMode(KeyboardMode.COMMANDS)
                    }
                )
            }
            DemoTextSurface(
                committedText = demoState.committedText,
                composingText = demoState.composingText
            )
            KeyboardView(
                keyboardMode = demoState.keyboardMode,
                chineseMode = demoState.chineseMode,
                onKeyboardModeChange = { demoState = demoState.setKeyboardMode(it) },
                onKeyPress = { key -> demoState = demoState.handleKeyPress(key) },
                onCandidateSelected = { candidate -> demoState = demoState.commitCandidate(candidate) },
                candidates = demoState.candidates,
                quickCommandSuggestions = if (demoState.keyboardMode == KeyboardMode.COMMANDS) {
                    CommandPalette.defaultQuickCommands
                } else {
                    emptyList()
                },
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
