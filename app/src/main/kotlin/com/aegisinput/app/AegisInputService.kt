package com.aegisinput.app

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.ui.platform.ComposeView
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
import com.aegisinput.ui.keyboard.KeyboardView

class AegisInputService : InputMethodService(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private var rimeSession: RimeSession? = null
    private val inputConnectionWrapper = InputConnectionWrapper()

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        RimeBridge.initialize(applicationContext)
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        rimeSession = RimeBridge.createSession()
        inputConnectionWrapper.bind(currentInputConnection, attribute)
    }

    override fun onCreateInputView(): View {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@AegisInputService)
            setViewTreeSavedStateRegistryOwner(this@AegisInputService)
            setContent {
                KeyboardView(
                    onKeyPress = { key -> handleKeyPress(key) },
                    onCandidateSelected = { candidate -> commitCandidate(candidate) },
                    candidates = rimeSession?.candidates ?: emptyList()
                )
            }
        }
        return composeView
    }

    override fun onFinishInput() {
        super.onFinishInput()
        inputConnectionWrapper.unbind()
        rimeSession?.let {
            RimeBridge.destroySession(it)
        }
        rimeSession = null
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
                if (session.hasComposing()) {
                    session.processKey("BackSpace")
                    ic.setComposingText(session.composingText, 1)
                } else {
                    ic.deleteSurroundingText(1, 0)
                }
            }
            key == "ENTER" -> {
                if (session.hasComposing()) {
                    ic.commitText(session.composingText, 1)
                    session.reset()
                } else {
                    ic.sendKeyEvent(android.view.KeyEvent(
                        android.view.KeyEvent.ACTION_DOWN,
                        android.view.KeyEvent.KEYCODE_ENTER
                    ))
                }
            }
            key == "SPACE" -> {
                if (session.hasComposing() && session.candidates.isNotEmpty()) {
                    commitCandidate(session.candidates.first())
                } else {
                    ic.commitText(" ", 1)
                }
            }
            else -> {
                session.processKey(key)
                if (session.hasComposing()) {
                    ic.setComposingText(session.composingText, 1)
                } else {
                    ic.commitText(key, 1)
                }
            }
        }
    }

    private fun commitCandidate(candidate: String) {
        inputConnectionWrapper.commitText(candidate, 1)
        rimeSession?.reset()
    }
}
