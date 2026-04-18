package com.aegisinput.engine

/**
 * Represents an active RIME input session. Each typing context
 * gets its own session to isolate composing state.
 */
class RimeSession(val id: Long) {

    val composingText: String
        get() = RimeBridge.getComposingText(id)

    val candidates: List<String>
        get() = RimeBridge.getCandidates(id)

    fun hasComposing(): Boolean = composingText.isNotEmpty()

    fun processKey(key: String): Boolean {
        return RimeBridge.processKey(id, key)
    }

    fun commit(): String {
        return RimeBridge.commitComposition(id)
    }

    fun reset() {
        RimeBridge.resetSession(id)
    }
}
