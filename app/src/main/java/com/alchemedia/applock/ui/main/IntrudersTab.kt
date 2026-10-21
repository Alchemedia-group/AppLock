package com.alchemedia.applock.ui.main

import android.content.Context
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

    fun labelFor(shot: IntruderShot): String {
        val pkg = shot.packageName ?: return context.getString(R.string.intruder_unknown_app)
        return try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (_: Exception) {
            pkg
        }
    }

    Box(modifier.fillMaxSize()) {
        if (shots.isEmpty()) {
            Text(
                stringResource(R.string.intruders_empty),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
            )
        } else {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.End) {
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
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(140.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(shots, key = { it.file.name }) { shot ->
                        Column(Modifier.clickable { open = shot }) {
                            ShotImage(
                                file = shot.file,
                                maxDimension = 400,
                                modifier = Modifier.fillMaxWidth().aspectRatio(0.75f).clip(MaterialTheme.shapes.medium),
                            )
                            Text(labelFor(shot), style = MaterialTheme.typography.labelLarge, maxLines = 1)
                            Text(
                                formatTime(context, shot.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
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
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ShotImage(
                    file = shot.file,
                    maxDimension = 1600,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.intruder_tried_to_open, labelFor(shot)),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(formatTime(context, shot.timestamp), color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { open = null }) { Text(stringResource(R.string.intruder_close)) }
                    OutlinedButton(onClick = {
                        onDelete(shot)
                        open = null
                    }) { Text(stringResource(R.string.intruder_delete)) }
                }
            }
        }
    }
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
