package com.aegisinput.engine

/** One editor's Rime session; visible candidate indices retain their native identity. */
class RimeSession(val id: Long) {
    private var frequencies: Map<String, Int> = emptyMap()
    private var nativeCandidates: List<String> = emptyList()
    val composingText: String get() = RimeBridge.getComposingText(id)
    val candidates: List<String> get() {
        nativeCandidates = RimeBridge.getCandidates(id)
        return nativeCandidates.sortedByDescending { frequencies[it] ?: 0 }
    }
    fun rank(frequencies: Map<String, Int>) { this.frequencies = frequencies }
    fun hasComposing() = composingText.isNotEmpty()
    fun processKey(key: String): Boolean {
        frequencies = emptyMap()
        return RimeBridge.processKey(id, key)
    }
    fun commit() = RimeBridge.commitComposition(id)
    fun selectCandidate(index: Int): String {
        val word = candidates.getOrNull(index) ?: return ""
        return RimeBridge.selectCandidate(id, nativeCandidates.indexOf(word))
    }
    fun selectSchema(zhuyin: Boolean) {
        frequencies = emptyMap()
        check(RimeBridge.selectSchema(id, zhuyin)) { "Unable to load Rime input schema" }
    }
    fun reset() { frequencies = emptyMap(); RimeBridge.resetSession(id) }
}
