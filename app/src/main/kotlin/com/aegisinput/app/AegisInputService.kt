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
    private var chineseMode by mutableStateOf(KeyboardMode.PINYIN)
    private var keyboardMode by mutableStateOf(KeyboardMode.PINYIN)
    private var composingText by mutableStateOf("")
    private var chineseEnabled by mutableStateOf(true)
    private var enterLabel by mutableStateOf("↵")
    private var preferredMode = KeyboardMode.PINYIN
    private var typingController: TypingController? = null
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
        chineseMode = KeyboardMode.PINYIN
        chineseEnabled = !ImeCompatibilityPolicy.isSensitiveField(attribute?.inputType ?: 0)
        enterLabel = ImeCompatibilityPolicy.enterLabel(attribute?.imeOptions)
        keyboardMode = ImeCompatibilityPolicy.resolveInitialKeyboardMode(
            editorInfo = attribute,
            chineseMode = preferredMode,
            nativeEngineAvailable = nativeEngineAvailable
        )
        clearCompositionState(resetSession = false)
        if (!nativeEngineAvailable) showNativeEngineUnavailableToast()
        rimeSession = if (nativeEngineAvailable) RimeBridge.createSession() else null
        val session = rimeSession
        typingController = TypingController(object : TextEditor {
            override fun compose(text: String) { inputConnectionWrapper.setComposingText(text, 1) }
            override fun commit(text: String) { inputConnectionWrapper.commitText(text, 1) }
            override fun delete() { inputConnectionWrapper.deletePreviousCharacter() }
            override fun enter() { inputConnectionWrapper.performEnterAction() }
        }, session?.let { active -> object : ChineseComposer {
            override val text get() = active.composingText
            override val candidates get() = active.candidates
            override fun key(key: String) { active.processKey(key) }
            override fun select(index: Int) = active.selectCandidate(index)
            override fun reset() { active.reset() }
        } })
    }

    override fun onCreateInputView(): View {
        val composeView = keyboardComposeView ?: ComposeView(this).apply {
            id = R.id.keyboard_compose_view
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
        }.also { cView ->
            keyboardComposeView = cView
        }

        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(this@AegisInputService)
            decorView.setViewTreeSavedStateRegistryOwner(this@AegisInputService)
            decorView.setViewTreeViewModelStoreOwner(this@AegisInputService)
            decorView.setViewTreeOnBackPressedDispatcherOwner(this@AegisInputService)
        }

        return composeView
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
        typingController = null
    }

    override fun onCurrentInputMethodSubtypeChanged(newSubtype: InputMethodSubtype?) {
        super.onCurrentInputMethodSubtypeChanged(newSubtype)
        typingController?.commitRaw()
        clearCompositionState()
        chineseMode = KeyboardMode.PINYIN
        if (nativeEngineAvailable && keyboardMode.isChineseMode()) {
            keyboardMode = chineseMode
        }
    }

    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int, candidatesStart: Int, candidatesEnd: Int) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        if (composingText.isNotEmpty() && (newSelStart != candidatesEnd || newSelEnd != candidatesEnd)) {
            clearCompositionState()
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
                composingText = composingText,
                onKeyboardModeChange = { mode -> handleKeyboardModeChange(mode) },
                onKeyPress = { key -> handleKeyPress(key) },
                onCandidateSelected = { index, candidate -> commitCandidate(index, candidate) },
                candidates = candidates,
                chineseEnabled = chineseEnabled,
                enterLabel = enterLabel,
                quickCommandSuggestions = if (keyboardMode == KeyboardMode.COMMANDS) {
                    CommandPalette.defaultQuickCommands
                } else {
                    emptyList()
                },
                onHideKeyboard = { requestHideSelf(0) },
                onSwitchKeyboard = {
                    inputMethodManager.showInputMethodPicker()
                }
            )
        }
    }

    private fun handleKeyPress(key: String) {
        typingController?.key(key, keyboardMode == KeyboardMode.PINYIN)
        rimeSession?.let(::syncSessionState)
    }

    private fun handleKeyboardModeChange(mode: KeyboardMode) {
        if ((!nativeEngineAvailable || !chineseEnabled) && mode.isChineseMode()) return
        typingController?.commitRaw()
        clearCompositionState()
        keyboardMode = if (mode == KeyboardMode.ZHUYIN) KeyboardMode.PINYIN else mode
        if (keyboardMode == KeyboardMode.PINYIN || keyboardMode == KeyboardMode.LATIN) {
            preferredMode = keyboardMode
        }
    }

    private fun commitCandidate(index: Int, candidate: String) {
        if (keyboardMode == KeyboardMode.PINYIN) {
            typingController?.select(index)
            rimeSession?.let(::syncSessionState)
        } else {
            inputConnectionWrapper.commitText(candidate, 1)
        }
    }

    private fun syncSessionState(session: RimeSession) {
        val text = session.composingText
        composingText = text
        if (text.isNotEmpty()) {
            inputConnectionWrapper.setComposingText(text, 1)
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
        composingText = ""
        candidates.clear()
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
