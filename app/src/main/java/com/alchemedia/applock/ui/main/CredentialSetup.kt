package com.alchemedia.applock.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alchemedia.applock.R
import com.alchemedia.applock.data.LockType
import com.alchemedia.applock.ui.components.PATTERN_MIN_DOTS
import com.alchemedia.applock.ui.components.PIN_MIN_LENGTH
import com.alchemedia.applock.ui.components.PatternEntry
import com.alchemedia.applock.ui.components.PinEntry

/** Pick PIN or pattern, enter it, confirm it. Used for first-run setup and for changing the secret. */
@Composable
fun CredentialSetup(
    onDone: (LockType, String) -> Unit,
    modifier: Modifier = Modifier,
    onCancel: (() -> Unit)? = null,
) {
    var type by remember { mutableStateOf(LockType.PIN) }
    var first by remember { mutableStateOf<String?>(null) }
    var errorRes by remember { mutableStateOf<Int?>(null) }

    fun onSecret(secret: String) {
        val minLength = if (type == LockType.PIN) PIN_MIN_LENGTH else PATTERN_MIN_DOTS
        val firstEntry = first
        when {
            secret.length < minLength -> {
                errorRes = if (type == LockType.PIN) R.string.setup_too_short_pin else R.string.setup_too_short_pattern
            }
            firstEntry == null -> {
                first = secret
                errorRes = null
            }
            firstEntry == secret -> onDone(type, secret)
            else -> {
                first = null
                errorRes = R.string.setup_mismatch
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.setup_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = type == LockType.PIN,
                onClick = { type = LockType.PIN; first = null; errorRes = null },
                label = { Text(stringResource(R.string.lock_type_pin)) },
            )
            FilterChip(
                selected = type == LockType.PATTERN,
                onClick = { type = LockType.PATTERN; first = null; errorRes = null },
                label = { Text(stringResource(R.string.lock_type_pattern)) },
            )
        }
        Spacer(Modifier.height(16.dp))

        val prompt = when {
            type == LockType.PIN && first == null -> R.string.setup_enter_pin
            type == LockType.PIN -> R.string.setup_confirm_pin
            first == null -> R.string.setup_draw_pattern
            else -> R.string.setup_confirm_pattern
        }
        Text(stringResource(prompt), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            errorRes?.let { stringResource(it) }.orEmpty(),
            color = MaterialTheme.colorScheme.tertiary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.height(24.dp),
        )
        Spacer(Modifier.height(8.dp))

        when (type) {
            LockType.PIN -> PinEntry(onSubmit = { onSecret(it) })
            LockType.PATTERN -> PatternEntry(onSubmit = { onSecret(it) })
        }

        if (onCancel != null) {
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
        }
    }
}
