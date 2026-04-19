package com.aegisinput.app

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype
import androidx.compose.ui.platform.ComposeView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.aegisinput.engine.RimeBridge
import com.aegisinput.engine.RimeSession
import com.aegisinput.ui.keyboard.KeyboardMode
import com.aegisinput.ui.keyboard.KeyboardView

class AegisInputService : InputMethodService(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private var rimeSession: RimeSession? = null
    private val inputConnectionWrapper = InputConnectionWrapper()
    private val candidates = mutableStateListOf<String>()
    private var chineseMode by mutableStateOf(KeyboardMode.ZHUYIN)
    private var keyboardMode by mutableStateOf(KeyboardMode.ZHUYIN)

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        rimeSession?.let(RimeBridge::destroySession)
        rimeSession = RimeBridge.createSession()
        inputConnectionWrapper.bind(currentInputConnection, attribute)
        chineseMode = resolveChineseMode(inputMethodManager.currentInputMethodSubtype)
        keyboardMode = chineseMode
        clearCompositionState(resetSession = false)
    }

    override fun onCreateInputView(): View {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@AegisInputService)
            setViewTreeSavedStateRegistryOwner(this@AegisInputService)
            setContent {
                KeyboardView(
                    keyboardMode = keyboardMode,
                    chineseMode = chineseMode,
                    onKeyboardModeChange = { mode -> keyboardMode = mode },
                    onKeyPress = { key -> handleKeyPress(key) },
                    onCandidateSelected = { candidate -> commitCandidate(candidate) },
                    candidates = candidates
                )
            }
        }
        return composeView
    }

    override fun onFinishInput() {
        super.onFinishInput()
        clearCompositionState(resetSession = false)
        inputConnectionWrapper.unbind()
        rimeSession?.let {
            RimeBridge.destroySession(it)
        }
        rimeSession = null
    }

    override fun onCurrentInputMethodSubtypeChanged(newSubtype: InputMethodSubtype?) {
        super.onCurrentInputMethodSubtypeChanged(newSubtype)
        chineseMode = resolveChineseMode(newSubtype)
        if (keyboardMode.isChineseMode()) {
            keyboardMode = chineseMode
        }
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        RimeBridge.shutdown()
        super.onDestroy()
    }

    private fun handleKeyPress(key: String) {
        val session = rimeSession ?: return
        val ic = inputConnectionWrapper

        when {
            key == "BACKSPACE" -> {
                if (keyboardMode.isChineseMode() && session.hasComposing()) {
                    session.processKey("BackSpace")
                    syncSessionState(session)
                } else {
                    ic.deleteSurroundingText(1, 0)
                }
            }
            key == "ENTER" -> {
                if (keyboardMode.isChineseMode() && session.hasComposing()) {
                    val committed = session.commit()
                    if (committed.isNotEmpty()) {
                        ic.commitText(committed, 1)
                    }
                    clearCompositionState(resetSession = false)
                } else {
                    ic.sendKeyEvent(android.view.KeyEvent(
                        android.view.KeyEvent.ACTION_DOWN,
                        android.view.KeyEvent.KEYCODE_ENTER
                    ))
                }
            }
            key == "SPACE" -> {
                if (keyboardMode.isChineseMode() && candidates.isNotEmpty()) {
                    commitCandidate(candidates.first())
                } else {
                    ic.commitText(" ", 1)
                }
            }
            else -> {
                if (keyboardMode.isChineseMode()) {
                    session.processKey(normalizeKeyForChineseMode(key))
                    syncSessionState(session)
                } else {
                    ic.commitText(key, 1)
                }
            }
        }
    }

    private fun commitCandidate(candidate: String) {
        inputConnectionWrapper.commitText(candidate, 1)
        clearCompositionState()
    }

    private fun syncSessionState(session: RimeSession) {
        if (session.hasComposing()) {
            inputConnectionWrapper.setComposingText(session.composingText, 1)
        } else {
            inputConnectionWrapper.finishComposingText()
        }
        candidates.clear()
        candidates.addAll(session.candidates)
    }

    private fun clearCompositionState(resetSession: Boolean = true) {
        if (resetSession) {
            rimeSession?.reset()
        }
        inputConnectionWrapper.finishComposingText()
        candidates.clear()
    }

    private fun normalizeKeyForChineseMode(key: String): String {
        return if (chineseMode == KeyboardMode.PINYIN) key.lowercase() else key
    }

    private fun resolveChineseMode(subtype: InputMethodSubtype?): KeyboardMode {
        return when (subtype?.getExtraValueOf("mode")?.lowercase()) {
            "pinyin" -> KeyboardMode.PINYIN
            else -> KeyboardMode.ZHUYIN
        }
    }

    private val inputMethodManager: InputMethodManager
        get() = getSystemService(InputMethodManager::class.java)

    private fun KeyboardMode.isChineseMode(): Boolean {
        return this == KeyboardMode.PINYIN || this == KeyboardMode.ZHUYIN
    }
}
