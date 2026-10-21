package com.alchemedia.applock.ui.main

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alchemedia.applock.BuildConfig
import com.alchemedia.applock.R
import com.alchemedia.applock.billing.BillingMessage
import com.alchemedia.applock.data.AppSettings
import com.alchemedia.applock.ui.components.IconTile

/** Paywall for the one-time Pro purchase. Also the "you have Pro" confirmation. */
@Composable
fun ProScreen(settings: AppSettings, viewModel: MainViewModel, onClose: () -> Unit) {
    val billing by viewModel.billingState.collectAsState()
    val activity = LocalActivity.current
    val colors = MaterialTheme.colorScheme
    val price = billing.priceText ?: stringResource(R.string.pro_price_fallback)

    DisposableEffect(Unit) {
        viewModel.startBilling()
        onDispose { viewModel.clearBillingMessage() }
    }

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IconTile(painterResource(R.drawable.ic_camera), size = 80.dp)
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.pro_title), style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.pro_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))

        Surface(shape = MaterialTheme.shapes.large, color = colors.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Feature(stringResource(R.string.pro_feature_photo))
                Feature(stringResource(R.string.pro_feature_attempts))
                Feature(stringResource(R.string.pro_feature_once))
            }
        }
        Spacer(Modifier.height(24.dp))

        if (settings.proUnlocked) {
            Text(stringResource(R.string.pro_owned_title), style = MaterialTheme.typography.titleLarge, color = colors.primary)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.pro_owned_body),
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                Text(stringResource(R.string.guide_done), style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Button(
                onClick = { activity?.let(viewModel::purchasePro) },
                enabled = !billing.purchasing,
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                if (billing.purchasing) {
                    CircularProgressIndicator(Modifier.height(22.dp).width(22.dp), strokeWidth = 2.dp, color = colors.onPrimary)
                } else {
                    Text(stringResource(R.string.pro_buy, price), style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = viewModel::restorePurchases) { Text(stringResource(R.string.pro_restore)) }

            billing.message?.let { message ->
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(
                        when (message) {
                            BillingMessage.Unavailable -> R.string.pro_unavailable
                            BillingMessage.Cancelled -> R.string.pro_cancelled
                            BillingMessage.Pending -> R.string.pro_pending
                            BillingMessage.Failed -> R.string.pro_failed
                            BillingMessage.Restored -> R.string.pro_restored
                            BillingMessage.NothingToRestore -> R.string.pro_nothing_to_restore
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            if (!billing.storeReady && billing.message == null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.pro_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            if (BuildConfig.DEBUG) {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = viewModel::debugUnlockPro) { Text(stringResource(R.string.pro_debug_unlock)) }
            }
            TextButton(onClick = onClose) { Text(stringResource(R.string.guide_close)) }
        }
    }
}

@Composable
private fun Feature(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text("✓", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}
