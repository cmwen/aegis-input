package com.aegisinput.ui.keyboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun KeyButton(
    keyDef: KeyDef,
    onPress: (KeyDef) -> Unit,
    modifier: Modifier = Modifier,
    hintLabel: String? = null,
    onBoundsChanged: (DynamicHitbox.KeyBounds) -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            keyDef.type == KeyType.MODIFIER -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
            keyDef.type == KeyType.ENTER -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surface
        },
        label = "keyBgColor"
    )

    val textColor = when {
        keyDef.type == KeyType.ENTER -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    val fontSize = when (keyDef.type) {
        KeyType.CHARACTER -> 18.sp
        else -> 14.sp
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(48.dp)
            .padding(horizontal = 2.dp, vertical = 3.dp)
            .shadow(if (isPressed) 0.dp else 2.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                shape = RoundedCornerShape(8.dp)
            )
            .trackKeyBounds(keyDef, onBoundsChanged)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onPress(keyDef)
            }
    ) {
        Text(
            text = keyDef.label,
            color = textColor,
            fontSize = fontSize,
            fontWeight = if (keyDef.type == KeyType.CHARACTER) FontWeight.Normal else FontWeight.Medium
        )
        if (hintLabel != null) {
            Text(
                text = hintLabel,
                color = textColor.copy(alpha = 0.5f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 4.dp)
            )
        }
    }
}
