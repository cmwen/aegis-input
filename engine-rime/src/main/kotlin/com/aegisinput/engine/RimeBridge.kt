package com.aegisinput.engine

import android.content.Context

/**
 * JNI bridge to Librime. All native calls are routed through this singleton.
 * Runs entirely on-device — no data leaves the device.
 */
object RimeBridge {

    private const val NATIVE_LIBRARY_NAME = "aegisinput-engine"

    private val nativeLibraryLock = Any()
    @Volatile
    private var nativeLibraryLoaded = false
    @Volatile
    private var nativeLibraryErrorMessage: String? = null
    private var initialized = false

    fun isAvailable(): Boolean {
        return ensureNativeLibraryLoaded()
    }

    fun unavailableMessage(): String? {
        ensureNativeLibraryLoaded()
        return nativeLibraryErrorMessage
    }

    fun initialize(context: Context): Boolean {
        if (!ensureNativeLibraryLoaded()) return false
        if (initialized) return true
        val dataDir = context.filesDir.resolve("rime").absolutePath
        val sharedDir = context.getExternalFilesDir(null)?.resolve("rime")?.absolutePath ?: dataDir
        return try {
            nativeInitialize(dataDir, sharedDir)
            initialized = true
            true
        } catch (error: UnsatisfiedLinkError) {
            nativeLibraryErrorMessage = error.message ?: "Unable to initialize the native AegisInput engine."
            false
        }
    }

    fun shutdown() {
        if (!nativeLibraryLoaded) return
        if (!initialized) return
        nativeShutdown()
        initialized = false
    }

    fun createSession(): RimeSession {
        ensureReady()
        val sessionId = nativeCreateSession()
        return RimeSession(sessionId)
    }

    fun destroySession(session: RimeSession) {
        ensureReady()
        nativeDestroySession(session.id)
    }

    fun processKey(sessionId: Long, key: String): Boolean {
        ensureReady()
        return nativeProcessKey(sessionId, key)
    }

    fun getComposingText(sessionId: Long): String {
        ensureReady()
        return nativeGetComposingText(sessionId)
    }

    fun getCandidates(sessionId: Long): List<String> {
        ensureReady()
        return nativeGetCandidates(sessionId).toList()
    }

    fun commitComposition(sessionId: Long): String {
        ensureReady()
        return nativeCommitComposition(sessionId)
    }

    fun resetSession(sessionId: Long) {
        ensureReady()
        nativeClearComposition(sessionId)
    }

    private fun ensureNativeLibraryLoaded(): Boolean {
        if (nativeLibraryLoaded) return true
        if (nativeLibraryErrorMessage != null) return false

        synchronized(nativeLibraryLock) {
            if (nativeLibraryLoaded) return true
            if (nativeLibraryErrorMessage != null) return false

            return try {
                System.loadLibrary(NATIVE_LIBRARY_NAME)
                nativeLibraryLoaded = true
                true
            } catch (error: UnsatisfiedLinkError) {
                nativeLibraryErrorMessage =
                    error.message ?: "Unable to load the native AegisInput engine."
                false
            }
        }
    }

    private fun ensureReady() {
        check(ensureNativeLibraryLoaded()) {
            nativeLibraryErrorMessage ?: "Unable to load the native AegisInput engine."
        }
        check(initialized) {
            "RimeBridge.initialize(context) must be called before using the native engine."
        }
    }

    // --- Native methods ---
    private external fun nativeInitialize(dataDir: String, sharedDir: String)
    private external fun nativeShutdown()
    private external fun nativeCreateSession(): Long
    private external fun nativeDestroySession(sessionId: Long)
    private external fun nativeProcessKey(sessionId: Long, key: String): Boolean
    private external fun nativeGetComposingText(sessionId: Long): String
    private external fun nativeGetCandidates(sessionId: Long): Array<String>
    private external fun nativeCommitComposition(sessionId: Long): String
    private external fun nativeClearComposition(sessionId: Long)
}
