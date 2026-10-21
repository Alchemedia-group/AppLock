package com.alchemedia.applock.ui.main

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Surface
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import com.alchemedia.applock.ui.components.IconTile
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import com.alchemedia.applock.R
import com.alchemedia.applock.intruder.IntruderShot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

@Composable
fun IntrudersTab(
    shots: List<IntruderShot>,
    onDelete: (IntruderShot) -> Unit,
    onDeleteAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf<IntruderShot?>(null) }
    var confirmDeleteAll by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val unknownApp = stringResource(R.string.intruder_unknown_app)
    val scope = rememberCoroutineScope()
    var pendingSave by remember { mutableStateOf<IntruderShot?>(null) }
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { uri ->
        val shot = pendingSave
        pendingSave = null
        if (uri != null && shot != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            shot.file.inputStream().use { it.copyTo(out) }
                        } != null
                    }.getOrDefault(false)
                }
                Toast.makeText(context, if (ok) R.string.intruder_saved else R.string.intruder_save_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun labelFor(shot: IntruderShot): String {
        val pkg = shot.packageName ?: return unknownApp
        return try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (_: Exception) {
            pkg
        }
    }

    Box(modifier.fillMaxSize()) {
        if (shots.isEmpty()) {
            Column(
                Modifier
                    .align(Alignment.Center)
                    .padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                IconTile(
                    painterResource(R.drawable.ic_camera),
                    size = 72.dp,
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    stringResource(R.string.intruders_empty),
                    textAlign = TextAlign.Center,
                    color = colors.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 4.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = MaterialTheme.shapes.large,
                        color = colors.errorContainer,
                    ) {
                        Row(Modifier.padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                pluralStringResource(R.plurals.intruders_summary, shots.size, shots.size),
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.onErrorContainer,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = {
                                if (confirmDeleteAll) {
                                    confirmDeleteAll = false
                                    onDeleteAll()
                                } else {
                                    confirmDeleteAll = true
                                }
                            }) {
                                Text(
                                    stringResource(
                                        if (confirmDeleteAll) R.string.intruder_delete_all_confirm else R.string.intruder_delete_all,
                                    ),
                                    color = colors.onErrorContainer,
                                )
                            }
                        }
                    }
                }
                items(shots, key = { it.file.name }) { shot ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clickable { open = shot },
                        shape = MaterialTheme.shapes.large,
                        color = colors.surfaceContainer,
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            ShotImage(
                                file = shot.file,
                                maxDimension = 400,
                                modifier = Modifier
                                    .width(78.dp)
                                    .height(104.dp)
                                    .clip(MaterialTheme.shapes.medium),
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(labelFor(shot), style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                Text(
                                    formatTime(context, shot.timestamp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSurfaceVariant,
                                )
                                Surface(shape = CircleShape, color = colors.errorContainer) {
                                    Text(
                                        stringResource(R.string.failed_unlock),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = colors.onErrorContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        open?.let { shot ->
            // Drawn in-window instead of as a Dialog so the navigation bar stays hidden.
            Column(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .safeDrawingPadding()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ShotImage(
                    file = shot.file,
                    maxDimension = 1600,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large),
                )
                Text(
                    stringResource(R.string.intruder_tried_to_open, labelFor(shot)),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(formatTime(context, shot.timestamp), color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
                Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(onClick = { shareShot(context, shot, labelFor(shot)) }) { Text(stringResource(R.string.intruder_share)) }
                    FilledTonalButton(onClick = {
                        pendingSave = shot
                        saveLauncher.launch("applock-intruder-${shot.timestamp}.jpg")
                    }) { Text(stringResource(R.string.intruder_save)) }
                }
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(onClick = { open = null }) { Text(stringResource(R.string.intruder_close)) }
                    Button(
                        onClick = {
                            onDelete(shot)
                            open = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.error, contentColor = colors.onError),
                    ) { Text(stringResource(R.string.intruder_delete)) }
                }
            }
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

@Composable
private fun ShotImage(
    file: File,
    maxDimension: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val bitmap by produceState<ImageBitmap?>(null, file) {
        value = withContext(Dispatchers.IO) { decodeUpright(file, maxDimension) }
    }
    Box(modifier.background(Color.DarkGray), contentAlignment = Alignment.Center) {
        bitmap?.let {
            Image(bitmap = it, contentDescription = null, contentScale = contentScale, modifier = Modifier.fillMaxSize())
        }
    }
}

private fun formatTime(context: Context, timestamp: Long): String =
    DateUtils.formatDateTime(
        context,
        timestamp,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH,
    )

/** Decodes a downsampled copy of [file] and applies its EXIF rotation (CameraX writes orientation as metadata). */
private fun decodeUpright(file: File, maxDimension: Int): ImageBitmap? {
    val path = file.absolutePath
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / sample > maxDimension) sample *= 2

    val decoded = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
        ?: return null
    val degrees = when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    val upright: Bitmap = if (degrees == 0f) {
        decoded
    } else {
        Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(degrees) }, true)
    }
    return upright.asImageBitmap()
}
