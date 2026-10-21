package com.alchemedia.applock.ui.lock

import android.os.SystemClock
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alchemedia.applock.R
import com.alchemedia.applock.data.AppSettings
import com.alchemedia.applock.data.LockType
import com.alchemedia.applock.ui.components.PatternEntry
import com.alchemedia.applock.ui.components.PinEntry
import kotlinx.coroutines.delay

/** Full-screen unlock UI shared by the app overlay and AppLock's own entry gate. */
@Composable
fun LockScreen(
    settings: AppSettings,
    state: LockUiState,
    title: String,
    icon: ImageBitmap?,
    onSecret: (String) -> Unit,
    modifier: Modifier = Modifier,
    onBiometric: (() -> Unit)? = null,
) {
    var secondsLeft by remember { mutableLongStateOf(0L) }
    LaunchedEffect(state.lockedUntil) {
        while (true) {
            val remaining = state.lockedUntil - SystemClock.elapsedRealtime()
            secondsLeft = if (remaining > 0) (remaining + 999) / 1000 else 0
            if (remaining <= 0) break
            delay(250)
        }
    }
    val lockedOut = secondsLeft > 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(12.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))

        val hint = when {
            lockedOut -> stringResource(R.string.locked_out, secondsLeft.toInt())
            state.wrongAttempt && settings.lockType == LockType.PIN -> stringResource(R.string.wrong_pin)
            state.wrongAttempt -> stringResource(R.string.wrong_pattern)
            settings.lockType == LockType.PIN -> stringResource(R.string.enter_pin)
            else -> stringResource(R.string.draw_pattern)
        }
        Text(
            hint,
            style = MaterialTheme.typography.bodyMedium,
            color = if (state.wrongAttempt || lockedOut) MaterialTheme.colorScheme.tertiary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))

        when (settings.lockType) {
            LockType.PIN -> PinEntry(onSubmit = onSecret, shuffle = settings.shuffleKeypad, enabled = !lockedOut)
            LockType.PATTERN -> PatternEntry(onSubmit = onSecret, enabled = !lockedOut)
        }

        if (onBiometric != null) {
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onBiometric) { Text(stringResource(R.string.use_fingerprint)) }
        }
    }
}
