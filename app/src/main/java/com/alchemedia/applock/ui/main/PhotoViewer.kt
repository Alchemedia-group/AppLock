package com.alchemedia.applock.ui.main

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.alchemedia.applock.R
import com.alchemedia.applock.intruder.IntruderShot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Full-screen view of one intruder photo, drawn above everything (including the bottom navigation).
 * Back and Delete sit in a top bar so nothing can cover them, and the system back gesture closes it.
 * Delete needs a second tap so an evidence photo can't be lost by accident.
 */
@Composable
fun PhotoViewer(
    shot: IntruderShot,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onClose)

    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val unknownApp = stringResource(R.string.intruder_unknown_app)
    val label = remember(shot) { appLabelFor(context, shot, unknownApp) }
    val scope = rememberCoroutineScope()
    var confirmDelete by remember(shot) { mutableStateOf(false) }
    var pendingSave by remember { mutableStateOf<IntruderShot?>(null) }

    // The second-tap prompt expires, so the button doesn't stay armed.
    LaunchedEffect(confirmDelete) {
        if (confirmDelete) {
            delay(3_000)
            confirmDelete = false
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { uri ->
        val toSave = pendingSave
        pendingSave = null
        if (uri != null && toSave != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            toSave.file.inputStream().use { it.copyTo(out) }
                        } != null
                    }.getOrDefault(false)
                }
                Toast.makeText(context, if (ok) R.string.intruder_saved else R.string.intruder_save_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .safeDrawingPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = onClose,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.onboard_back), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.weight(1f))
            if (confirmDelete) {
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.error, contentColor = colors.onError),
                ) { Text(stringResource(R.string.intruder_delete_confirm)) }
            } else {
                TextButton(
                    onClick = { confirmDelete = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.error),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.intruder_delete), style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        ShotImage(
            file = shot.file,
            maxDimension = 1600,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(MaterialTheme.shapes.large),
        )

        Text(
            stringResource(R.string.intruder_tried_to_open, label),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(formatTime(context, shot.timestamp), color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
        Row(Modifier.padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(onClick = { shareShot(context, shot, label) }) { Text(stringResource(R.string.intruder_share)) }
            FilledTonalButton(onClick = {
                pendingSave = shot
                saveLauncher.launch("applock-intruder-${shot.timestamp}.jpg")
            }) { Text(stringResource(R.string.intruder_save)) }
        }
    }
}

/** Hands the photo to the system share sheet through the FileProvider (never a raw file path). */
private fun shareShot(context: Context, shot: IntruderShot, appLabel: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", shot.file)
    val send = Intent(Intent.ACTION_SEND)
        .setType("image/jpeg")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_TEXT, context.getString(R.string.intruder_share_text, appLabel))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, context.getString(R.string.intruder_share_chooser)))
}
