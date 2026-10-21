package com.alchemedia.applock.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alchemedia.applock.R
import kotlin.math.max

const val PIN_MIN_LENGTH = 4
const val PIN_MAX_LENGTH = 8

/**
 * Numeric keypad with a row of dots. [onSubmit] receives the digits when the user taps the check key,
 * and the field clears itself afterwards.
 */
@Composable
fun PinEntry(
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
    shuffle: Boolean = false,
    enabled: Boolean = true,
) {
    var digits by remember { mutableStateOf("") }
    val order = remember(shuffle) {
        if (shuffle) "0123456789".toList().shuffled() else "1234567890".toList()
    }
    val grid = order.take(9).chunked(3)
    val last = order[9]

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        PinDots(count = digits.length)
        Spacer(Modifier.height(36.dp))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            grid.forEach { rowKeys ->
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    rowKeys.forEach { key ->
                        Key(key.toString(), enabled) { if (digits.length < PIN_MAX_LENGTH) digits += key }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Key("⌫", enabled, description = stringResource(R.string.delete_digit), subtle = true) {
                    digits = digits.dropLast(1)
                }
                Key(last.toString(), enabled) { if (digits.length < PIN_MAX_LENGTH) digits += last }
                Key("✓", enabled, description = stringResource(R.string.confirm_pin), accent = true) {
                    val entered = digits
                    digits = ""
                    if (entered.isNotEmpty()) onSubmit(entered)
                }
            }
        }
    }
}

@Composable
private fun PinDots(count: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(24.dp),
    ) {
        repeat(max(PIN_MIN_LENGTH, count)) { index ->
            val filled = index < count
            val scale by animateFloatAsState(
                targetValue = if (filled) 1f else 0.72f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "pinDot",
            )
            val color = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            Box(
                Modifier
                    .size(16.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(if (filled) color else Color.Transparent)
                    .border(2.dp, color, CircleShape),
            )
        }
    }
}

@Composable
private fun Key(
    label: String,
    enabled: Boolean,
    description: String? = null,
    accent: Boolean = false,
    subtle: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val background = when {
        accent -> colors.primary
        subtle -> Color.Transparent
        else -> colors.surfaceContainerHigh.copy(alpha = 0.75f)
    }
    Box(
        Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { if (description != null) contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = if (label.length == 1 && label[0].isDigit()) 30.sp else 24.sp,
            fontWeight = FontWeight.Normal,
            color = if (accent) colors.onPrimary else colors.onSurface,
        )
    }
}
