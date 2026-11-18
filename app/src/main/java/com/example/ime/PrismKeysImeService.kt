package com.example.ime

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.FileProvider
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.PrismKeysApp
import com.example.data.local.StickerEntity
import com.example.ui.keyboard.ExpressiveKeyboardView
import com.example.ui.keyboard.KeyboardController
import com.example.ui.keyboard.KeyboardTarget
import com.example.ui.theme.KeyboardThemePreset
import com.example.ui.theme.PrismKeysTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PrismKeysImeService :
    InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private lateinit var app: PrismKeysApp
    private lateinit var controller: KeyboardController
    private var clipboardManager: ClipboardManager? = null

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        if (::controller.isInitialized && controller.settings.value.clipboardAutoCapture) {
            serviceScope.launch(Dispatchers.IO) {
                app.clipboardRepository.captureSystemClipboardIfPresent()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        app = application as PrismKeysApp
        controller = KeyboardController(
            context = this,
            scope = serviceScope,
            preferencesRepository = app.preferencesRepository,
            clipboardRepository = app.clipboardRepository,
            stickerRepository = app.stickerRepository,
            suggestionEngine = app.suggestionEngine,
            gestureDecoder = app.gestureDecoder,
            translationEngine = app.translationEngine,
            target = imeTarget,
        )

        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboardManager?.addPrimaryClipChangedListener(clipListener)
    }

    override fun onEvaluateFullscreenMode(): Boolean {
        // Always dock cleanly at the bottom across Android home/root search and all apps
        return false
    }

    override fun onCreateInputView(): View {
        val decorView = window?.window?.decorView
        decorView?.let {
            it.setViewTreeLifecycleOwner(this)
            it.setViewTreeViewModelStoreOwner(this)
            it.setViewTreeSavedStateRegistryOwner(this)
        }

        return ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setViewTreeLifecycleOwner(this@PrismKeysImeService)
            setViewTreeViewModelStoreOwner(this@PrismKeysImeService)
            setViewTreeSavedStateRegistryOwner(this@PrismKeysImeService)
            setContent {
                val settings by controller.settings.collectAsStateWithLifecycle()
                val systemDark = isSystemInDarkTheme()
                val darkTheme = when {
                    settings.themePreset == KeyboardThemePreset.DYNAMIC_YOU -> when (settings.appDarkModeOverride) {
                        "DARK" -> true
                        "LIGHT" -> false
                        else -> systemDark
                    }
                    else -> settings.themePreset.isDark
                }
                PrismKeysTheme(
                    darkTheme = darkTheme,
                    dynamicColor = settings.themePreset == KeyboardThemePreset.DYNAMIC_YOU,
                ) {
                    ExpressiveKeyboardView(
                        controller = controller,
                        stickerRepository = app.stickerRepository,
                        showImeSwitcherKey = shouldOfferSwitcherSafe(),
                        modifier = Modifier.navigationBarsPadding(),
                    )
                }
            }
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (lifecycleRegistry.currentState == Lifecycle.State.CREATED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }
        if (lifecycleRegistry.currentState == Lifecycle.State.STARTED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        controller.resetComposingForNewField()
        if (controller.settings.value.clipboardAutoCapture) {
            serviceScope.launch(Dispatchers.IO) {
                app.clipboardRepository.captureSystemClipboardIfPresent()
            }
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        if (lifecycleRegistry.currentState == Lifecycle.State.RESUMED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }
        if (lifecycleRegistry.currentState == Lifecycle.State.STARTED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
    }

    override fun onFinishInput() {
        super.onFinishInput()
        if (::controller.isInitialized) {
            controller.resetComposingForNewField()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && ::controller.isInitialized) {
            if (controller.handleBackToQwerty()) {
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        clipboardManager?.removePrimaryClipChangedListener(clipListener)
        controller.release()
        serviceScope.cancel()
        store.clear()
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
        super.onDestroy()
    }

    private fun shouldOfferSwitcherSafe(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            shouldOfferSwitchingToNextInputMethod()
        } else {
            true
        }
    }

    private val imeTarget = object : KeyboardTarget {
        override fun commitText(text: String) {
            currentInputConnection?.commitText(text, 1)
        }

        override fun setComposingText(text: String) {
            currentInputConnection?.setComposingText(text, 1)
        }

        override fun finishComposingText() {
            currentInputConnection?.finishComposingText()
        }

        override fun deleteBeforeCursor(count: Int) {
            val ic = currentInputConnection ?: return
            val selected = ic.getSelectedText(0)
            if (!selected.isNullOrEmpty()) {
                ic.commitText("", 1)
            } else {
                ic.deleteSurroundingText(count, 0)
            }
        }

        override fun moveCursor(delta: Int) {
            val ic = currentInputConnection ?: return
            val keyCode = if (delta > 0) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
            repeat(kotlin.math.abs(delta)) {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
            }
        }

        override fun sendActionOrNewline() {
            val ic = currentInputConnection ?: return
            val editorInfo = currentInputEditorInfo
            val action = editorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
            if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                ic.performEditorAction(action)
            } else {
                ic.commitText("\n", 1)
            }
        }

        override fun getTextBeforeCursor(maxChars: Int): String {
            return currentInputConnection?.getTextBeforeCursor(maxChars, 0)?.toString().orEmpty()
        }

        override fun onStickerSelected(sticker: StickerEntity) {
            val ic = currentInputConnection ?: return
            val editorInfo = currentInputEditorInfo ?: return
            val mimeTypes = EditorInfoCompat.getContentMimeTypes(editorInfo)
            val supportsPng = mimeTypes.any {
                ClipDescription.compareMimeTypes(it, "image/png") ||
                    ClipDescription.compareMimeTypes(it, "image/*")
            }

            if (supportsPng) {
                serviceScope.launch {
                    val file = app.stickerRepository.resolveShareableFile(sticker)
                    if (file != null) {
                        val uri = FileProvider.getUriForFile(
                            this@PrismKeysImeService,
                            "${packageName}.fileprovider",
                            file,
                        )
                        val info = InputContentInfoCompat(
                            uri,
                            ClipDescription(sticker.label, arrayOf("image/png")),
                            null,
                        )
                        val flags = InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
                        val committed = InputConnectionCompat.commitContent(ic, editorInfo, info, flags, null)
                        if (committed) return@launch
                    }
                    ic.commitText("${sticker.emojiFallback} ", 1)
                }
            } else {
                ic.commitText("${sticker.emojiFallback} ", 1)
            }
        }

        override fun switchToNextIme() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                switchToNextInputMethod(false)
            }
        }

        override fun requestMicPermission() {
            val intent = Intent(this@PrismKeysImeService, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("request_mic_permission", true)
            }
            startActivity(intent)
        }
    }
}
