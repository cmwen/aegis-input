package com.aegisinput.engine

/**
 * Handles Zhuyin/Pinyin fuzzy matching to correct common pronunciation confusions.
 * This runs as a pre-processing layer before keys are sent to RIME.
 *
 * Common confusions handled:
 * - ㄥ (eng) vs ㄣ (en)
 * - ㄓ (zh) vs ㄗ (z)
 * - ㄔ (ch) vs ㄘ (c)
 * - ㄕ (sh) vs ㄙ (s)
 * - ㄈ (f) vs ㄏ (h)
 * - ㄌ (l) vs ㄋ (n)
 * - ㄢ (an) vs ㄤ (ang)
 */
object FuzzyMatcher {

    data class FuzzyPair(val primary: String, val alternate: String)

    private val zhuyinFuzzyPairs = listOf(
        FuzzyPair("ㄥ", "ㄣ"),
        FuzzyPair("ㄓ", "ㄗ"),
        FuzzyPair("ㄔ", "ㄘ"),
        FuzzyPair("ㄕ", "ㄙ"),
        FuzzyPair("ㄈ", "ㄏ"),
        FuzzyPair("ㄌ", "ㄋ"),
        FuzzyPair("ㄢ", "ㄤ"),
    )

    private val pinyinFuzzyPairs = listOf(
        FuzzyPair("eng", "en"),
        FuzzyPair("zh", "z"),
        FuzzyPair("ch", "c"),
        FuzzyPair("sh", "s"),
        FuzzyPair("f", "h"),
        FuzzyPair("l", "n"),
        FuzzyPair("an", "ang"),
        FuzzyPair("in", "ing"),
    )

    /**
     * Generate all fuzzy variants of the given input.
     * Returns the original input plus all single-substitution variants.
     */
    fun generateVariants(input: String, useZhuyin: Boolean = true): List<String> {
        val pairs = if (useZhuyin) zhuyinFuzzyPairs else pinyinFuzzyPairs
        val variants = mutableSetOf(input)

        for (pair in pairs) {
            if (input.contains(pair.primary)) {
                variants.add(input.replace(pair.primary, pair.alternate))
            }
            if (input.contains(pair.alternate)) {
                variants.add(input.replace(pair.alternate, pair.primary))
            }
        }

        return variants.toList()
    }

    /**
     * Convert RIME fuzzy config entries for the given mode.
     * Returns config lines to be appended to the RIME schema.
     */
    fun generateRimeConfig(useZhuyin: Boolean = true): List<String> {
        val pairs = if (useZhuyin) zhuyinFuzzyPairs else pinyinFuzzyPairs
        return pairs.flatMap { pair ->
            listOf(
                "- derive/${pair.primary}/${pair.alternate}/",
                "- derive/${pair.alternate}/${pair.primary}/"
            )
        }
    }
}
