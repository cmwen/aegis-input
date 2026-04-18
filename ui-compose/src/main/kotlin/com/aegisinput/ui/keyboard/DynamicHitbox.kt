package com.aegisinput.ui.keyboard

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Dynamic Hitbox System for keyboard keys.
 *
 * Instead of using static rectangular hit areas, this system expands
 * the effective touch target of each key based on:
 * 1. Frequency of the key in the current input context
 * 2. Distance from the actual touch point to key centers
 * 3. Adjacent key positions to reduce mistype rates
 *
 * This significantly reduces "fat finger" errors, especially for
 * densely packed Zhuyin keyboard layouts.
 */
object DynamicHitbox {

    data class KeyBounds(
        val keyDef: KeyDef,
        val bounds: Rect
    )

    /**
     * Given a touch point and a list of key bounds, find the most likely
     * intended key using weighted distance calculation.
     *
     * @param touchPoint The actual touch coordinates
     * @param keyBounds List of all keys with their screen bounds
     * @param frequencyWeights Optional map of key codes to frequency weights (higher = more likely)
     * @return The KeyDef of the most likely intended key, or null if no key is near
     */
    fun resolveKey(
        touchPoint: Offset,
        keyBounds: List<KeyBounds>,
        frequencyWeights: Map<String, Float> = emptyMap()
    ): KeyDef? {
        if (keyBounds.isEmpty()) return null

        val maxExpansion = 12f // max pixels to expand hitbox beyond bounds

        data class ScoredKey(val keyDef: KeyDef, val score: Float)

        val scored = keyBounds.mapNotNull { kb ->
            val expandedBounds = Rect(
                left = kb.bounds.left - maxExpansion,
                top = kb.bounds.top - maxExpansion,
                right = kb.bounds.right + maxExpansion,
                bottom = kb.bounds.bottom + maxExpansion
            )

            if (!expandedBounds.contains(touchPoint)) return@mapNotNull null

            val center = kb.bounds.center
            val distance = sqrt(
                (touchPoint.x - center.x).pow(2) + (touchPoint.y - center.y).pow(2)
            )

            // Base score: inverse distance (closer = higher score)
            val distanceScore = 1f / (1f + distance)

            // Bonus if touch is within original bounds
            val withinBoundsBonus = if (kb.bounds.contains(touchPoint)) 2f else 0f

            // Frequency weight bonus
            val freqWeight = frequencyWeights[kb.keyDef.code] ?: 1f

            ScoredKey(kb.keyDef, (distanceScore + withinBoundsBonus) * freqWeight)
        }

        return scored.maxByOrNull { it.score }?.keyDef
    }
}

/**
 * Composable modifier that tracks key bounds for the dynamic hitbox system.
 */
@Composable
fun Modifier.trackKeyBounds(
    keyDef: KeyDef,
    onBoundsChanged: (DynamicHitbox.KeyBounds) -> Unit
): Modifier {
    return this.onGloballyPositioned { coordinates ->
        onBoundsChanged(
            DynamicHitbox.KeyBounds(
                keyDef = keyDef,
                bounds = coordinates.boundsInParent()
            )
        )
    }
}
