package com.aegisinput.engine

import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzyMatcherTest {

    @Test
    fun zhuyinVariantsIncludeKnownSwap() {
        val variants = FuzzyMatcher.generateVariants("ㄓㄣ", useZhuyin = true)

        assertTrue("ㄓㄣ" in variants)
        assertTrue("ㄗㄣ" in variants)
    }

    @Test
    fun pinyinVariantsIncludeKnownSwap() {
        val variants = FuzzyMatcher.generateVariants("sheng", useZhuyin = false)

        assertTrue("sheng" in variants)
        assertTrue("seng" in variants)
    }
}
