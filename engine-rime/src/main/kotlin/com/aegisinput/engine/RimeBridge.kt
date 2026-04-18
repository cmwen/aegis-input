package com.aegisinput.engine

import android.content.Context

/**
 * JNI bridge to Librime. All native calls are routed through this singleton.
 * Runs entirely on-device — no data leaves the device.
 */
object RimeBridge {

    private var initialized = false

    init {
        System.loadLibrary("aegisinput-engine")
    }

    fun initialize(context: Context) {
        if (initialized) return
        val dataDir = context.filesDir.resolve("rime").absolutePath
        val sharedDir = context.getExternalFilesDir(null)?.resolve("rime")?.absolutePath ?: dataDir
        nativeInitialize(dataDir, sharedDir)
        initialized = true
    }

    fun shutdown() {
        if (!initialized) return
        nativeShutdown()
        initialized = false
    }

    fun createSession(): RimeSession {
        val sessionId = nativeCreateSession()
        return RimeSession(sessionId)
    }

    fun destroySession(session: RimeSession) {
        nativeDestroySession(session.id)
    }

    fun processKey(sessionId: Long, key: String): Boolean {
        return nativeProcessKey(sessionId, key)
    }

    fun getComposingText(sessionId: Long): String {
        return nativeGetComposingText(sessionId)
    }

    fun getCandidates(sessionId: Long): List<String> {
        return nativeGetCandidates(sessionId).toList()
    }

    fun commitComposition(sessionId: Long): String {
        return nativeCommitComposition(sessionId)
    }

    fun resetSession(sessionId: Long) {
        nativeClearComposition(sessionId)
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
