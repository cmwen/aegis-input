package com.aegisinput.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.aegisinput.ui.theme.AegisInputTheme

open class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AegisInputTheme {
                SetupScreen(
                    onEnable = { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) },
                    onSwitch = { getSystemService(InputMethodManager::class.java)?.showInputMethodPicker() }
                )
            }
        }
    }
}

class SettingsActivity : MainActivity()

@Composable
private fun SetupScreen(onEnable: () -> Unit, onSwitch: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("AegisInput", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("English · 拼音 · 注音. Entirely offline.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Make yourself at home", style = MaterialTheme.typography.titleLarge)
                    Text("1. Enable AegisInput in Android settings.\n2. Choose it as your keyboard.\n3. Tap the text field below to try it.")
                    Button(onClick = onEnable, modifier = Modifier.fillMaxWidth()) { Text("Enable keyboard") }
                    OutlinedButton(onClick = onSwitch, modifier = Modifier.fillMaxWidth()) { Text("Choose AegisInput") }
                }
            }
            Text("Try your keyboard", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("English or 中文") },
                placeholder = { Text("Try nihao, ㄋㄧˇㄏㄠˇ, or hello") },
                minLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                modifier = Modifier.fillMaxWidth()
            )
            Card {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("A little guide", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Use 拼音 / 注音 above the keys to switch Chinese input. EN opens English; 中 returns to your chosen Chinese mode.")
                    Text("Type Pinyin without tones. Tap a Chinese candidate, or press space to choose the first one. Swipe the candidate row for more.")
                    Text("Enter keeps the letters you typed. ?123 opens numbers and punctuation. Use v for ü, and an apostrophe to separate syllables.")
                    Text("Private by design · learns on this device", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Your typing and learned candidate preferences stay on this device. Password fields use English and never learn. No network access, accounts, or cloud suggestions.")
                }
            }
        }
    }
}
