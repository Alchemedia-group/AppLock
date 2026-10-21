package com.alchemedia.applock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.alchemedia.applock.R

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
        Spacer(Modifier.height(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            grid.forEach { rowKeys ->
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    rowKeys.forEach { key ->
                        Key(key.toString(), enabled) { if (digits.length < PIN_MAX_LENGTH) digits += key }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Key("⌫", enabled, description = stringResource(R.string.delete_digit)) {
                    digits = digits.dropLast(1)
                }
                Key(last.toString(), enabled) { if (digits.length < PIN_MAX_LENGTH) digits += last }
                Key("✓", enabled, description = stringResource(R.string.confirm_pin), emphasized = true) {
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
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(20.dp),
    ) {
        repeat(count) {
            Box(Modifier.size(14.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        }
        if (count == 0) {
            Box(Modifier.size(14.dp).background(MaterialTheme.colorScheme.outlineVariant, CircleShape))
        }
    }
}

@Composable
private fun Key(
    label: String,
    enabled: Boolean,
    description: String? = null,
    emphasized: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = if (emphasized) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .size(72.dp)
            .semantics { if (description != null) contentDescription = description },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.headlineSmall)
        }
    }
}
