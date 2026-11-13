package com.aegisinput.ui.keyboard

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.*
import org.junit.Test

class DynamicHitboxTest {
    private val left = KeyDef("ㄅ")
    private val right = KeyDef("ㄉ")
    private val bounds = listOf(
        DynamicHitbox.KeyBounds(left, Rect(0f, 0f, 40f, 50f)),
        DynamicHitbox.KeyBounds(right, Rect(48f, 0f, 88f, 50f))
    )
    @Test fun gapsResolveToNearestKeyAndBoundedFrequencyAdaptsTie() {
        assertEquals(left, DynamicHitbox.resolveKey(Offset(43f, 25f), bounds))
        assertEquals(right, DynamicHitbox.resolveKey(Offset(44f, 25f), bounds, mapOf(right.code to 1.15f)))
        assertEquals(left, DynamicHitbox.resolveKey(Offset(39f, 25f), bounds, mapOf(right.code to 1.15f)))
    }
    @Test fun distantTouchesDoNotType() {
        assertNull(DynamicHitbox.resolveKey(Offset(150f, 25f), bounds))
    }
}
