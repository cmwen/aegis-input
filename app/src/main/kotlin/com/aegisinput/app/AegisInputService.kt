package com.aegisinput.app

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype
import android.widget.Toast
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.setViewTreeOnBackPressedDispatcherOwner
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.aegisinput.engine.RimeBridge
import com.aegisinput.engine.RimeSession
import com.aegisinput.ui.keyboard.CommandPalette
import com.aegisinput.ui.keyboard.KeyboardMode
import com.aegisinput.ui.keyboard.KeyboardView

class AegisInputService : InputMethodService(), LifecycleOwner, SavedStateRegistryOwner,
    ViewModelStoreOwner, OnBackPressedDispatcherOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val viewModelStore = ViewModelStore()
    override val onBackPressedDispatcher = OnBackPressedDispatcher()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private var keyboardComposeView: ComposeView? = null
    private var inputViewContentBound = false
    private var rimeSession: RimeSession? = null
    private val inputConnectionWrapper = InputConnectionWrapper()
    private val candidates = mutableStateListOf<String>()
    private var chineseMode by mutableStateOf(KeyboardMode.ZHUYIN)
    private var keyboardMode by mutableStateOf(KeyboardMode.ZHUYIN)
    private var nativeEngineAvailable = false
    private var nativeEngineUnavailableMessage: String? = null
    private var hasShownNativeEngineUnavailableToast = false

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        nativeEngineAvailable = RimeBridge.initialize(applicationContext)
        nativeEngineUnavailableMessage = RimeBridge.unavailableMessage()
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        inputConnectionWrapper.bind(currentInputConnection, attribute)
        rimeSession?.let(RimeBridge::destroySession)
        rimeSession = null
        chineseMode = resolveChineseMode(inputMethodManager.currentInputMethodSubtype)
        keyboardMode = ImeCompatibilityPolicy.resolveInitialKeyboardMode(
            editorInfo = attribute,
            chineseMode = chineseMode,
            nativeEngineAvailable = nativeEngineAvailable
        )
        clearCompositionState(resetSession = false)
        if (!nativeEngineAvailable) {
            showNativeEngineUnavailableToast()
            return
        }
        rimeSession = RimeBridge.createSession()
    }

    override fun onCreateInputView(): View {
        return keyboardComposeView ?: ComposeView(this).apply {
            id = R.id.keyboard_compose_view
            setViewTreeLifecycleOwner(this@AegisInputService)
            setViewTreeSavedStateRegistryOwner(this@AegisInputService)
            setViewTreeViewModelStoreOwner(this@AegisInputService)
            setViewTreeOnBackPressedDispatcherOwner(this@AegisInputService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    bindInputViewContent(this@apply)
                }

                override fun onViewDetachedFromWindow(v: View) {
                    inputViewContentBound = false
                }
            })
            if (isAttachedToWindow) {
                bindInputViewContent(this)
            }
        }.also { composeView ->
            keyboardComposeView = composeView
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        moveLifecycleToResumed()
    }

    override fun onEvaluateFullscreenMode(): Boolean {
        return ImeCompatibilityPolicy.shouldUseFullscreenMode()
    }

    override fun onEvaluateInputViewShown(): Boolean {
        return ImeCompatibilityPolicy.shouldShowInputView()
    }

    override fun onShowInputRequested(flags: Int, configChange: Boolean): Boolean {
        return ImeCompatibilityPolicy.shouldShowInputView(
            requestFlags = flags,
            configChange = configChange
        )
    }

    override fun onUpdateExtractingVisibility(info: EditorInfo?) {
        isExtractViewShown = false
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        moveLifecycleToCreated()
        super.onFinishInputView(finishingInput)
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
        if (nativeEngineAvailable && keyboardMode.isChineseMode()) {
            keyboardMode = chineseMode
        }
    }

    override fun onDestroy() {
        moveLifecycleToCreated()
        keyboardComposeView?.disposeComposition()
        keyboardComposeView = null
        inputViewContentBound = false
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
        if (nativeEngineAvailable) {
            RimeBridge.shutdown()
        }
        super.onDestroy()
    }

    private fun moveLifecycleToResumed() {
        when (lifecycleRegistry.currentState) {
            Lifecycle.State.CREATED -> lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            Lifecycle.State.STARTED -> Unit
            Lifecycle.State.RESUMED -> return
            else -> return
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    private fun moveLifecycleToCreated() {
        if (lifecycleRegistry.currentState == Lifecycle.State.RESUMED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }
        if (lifecycleRegistry.currentState == Lifecycle.State.STARTED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
    }

    private fun bindInputViewContent(composeView: ComposeView) {
        if (inputViewContentBound) return
        inputViewContentBound = true
        composeView.setContent {
            KeyboardView(
                keyboardMode = keyboardMode,
                chineseMode = chineseMode,
                onKeyboardModeChange = { mode -> handleKeyboardModeChange(mode) },
                onKeyPress = { key -> handleKeyPress(key) },
                onCandidateSelected = { candidate -> commitCandidate(candidate) },
                candidates = candidates,
                quickCommandSuggestions = if (keyboardMode == KeyboardMode.COMMANDS) {
                    CommandPalette.defaultQuickCommands
                } else {
                    emptyList()
                }
            )
        }
    }

    private fun handleKeyPress(key: String) {
        val ic = inputConnectionWrapper
        val session = rimeSession

        when {
            key == "BACKSPACE" -> {
                if (keyboardMode.isChineseMode() && session?.hasComposing() == true) {
                    session.processKey("BackSpace")
                    syncSessionState(session)
                } else {
                    ic.deleteSurroundingText(1, 0)
                }
            }
            key == "ENTER" -> {
                if (keyboardMode.isChineseMode() && session?.hasComposing() == true) {
                    val committed = session.commit()
                    if (committed.isNotEmpty()) {
                        ic.commitText(committed, 1)
                    }
                    clearCompositionState(resetSession = false)
                } else {
                    ic.performEnterAction()
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
                    val activeSession = session ?: return
                    activeSession.processKey(normalizeKeyForChineseMode(key))
                    syncSessionState(activeSession)
                } else {
                    ic.commitText(key, 1)
                }
            }
        }
    }

    private fun handleKeyboardModeChange(mode: KeyboardMode) {
        if (!nativeEngineAvailable && mode.isChineseMode()) {
            keyboardMode = KeyboardMode.LATIN
            showNativeEngineUnavailableToast()
            return
        }
        keyboardMode = mode
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

    private fun showNativeEngineUnavailableToast() {
        if (hasShownNativeEngineUnavailableToast) return
        val detail = nativeEngineUnavailableMessage ?: getString(R.string.native_engine_unavailable_body)
        Toast.makeText(
            this,
            getString(R.string.native_engine_unavailable_toast, detail),
            Toast.LENGTH_LONG
        ).show()
        hasShownNativeEngineUnavailableToast = true
    }

}
