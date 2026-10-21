package com.alchemedia.applock.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

const val PATTERN_MIN_DOTS = 4

/** 3x3 pattern pad. [onSubmit] gets the visited dot indices (0–8, row-major) joined as a string. */
@Composable
fun PatternEntry(
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var padSize by remember { mutableStateOf(IntSize.Zero) }
    val selected = remember { mutableStateListOf<Int>() }
    var pointer by remember { mutableStateOf<Offset?>(null) }

    val active = MaterialTheme.colorScheme.primary
    val idle = MaterialTheme.colorScheme.outline

    fun dotCenter(index: Int): Offset {
        val cell = padSize.width / 3f
        return Offset((index % 3 + 0.5f) * cell, (index / 3 + 0.5f) * cell)
    }

    fun dotAt(point: Offset): Int? {
        val radius = padSize.width / 3f * 0.4f
        return (0..8).firstOrNull { (dotCenter(it) - point).getDistance() <= radius }
    }

    Canvas(
        modifier = modifier
            .widthIn(max = 300.dp)
            .aspectRatio(1f)
            .onSizeChanged { padSize = it }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { start ->
                        selected.clear()
                        pointer = start
                        dotAt(start)?.let { selected.add(it) }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        pointer = change.position
                        dotAt(change.position)?.let { if (it !in selected) selected.add(it) }
                    },
                    onDragEnd = {
                        val entered = selected.joinToString("")
                        selected.clear()
                        pointer = null
                        if (entered.isNotEmpty()) onSubmit(entered)
                    },
                    onDragCancel = {
                        selected.clear()
                        pointer = null
                    },
                )
            },
    ) {
        val stroke = 5.dp.toPx()
        val line = active.copy(alpha = 0.85f)
        for (i in 1 until selected.size) {
            drawLine(line, dotCenter(selected[i - 1]), dotCenter(selected[i]), stroke, StrokeCap.Round)
        }
        val tip = pointer
        if (tip != null && selected.isNotEmpty()) {
            drawLine(line, dotCenter(selected.last()), tip, stroke, StrokeCap.Round)
        }
        for (i in 0..8) {
            val on = i in selected
            if (on) drawCircle(active.copy(alpha = 0.2f), radius = 32.dp.toPx(), center = dotCenter(i))
            drawCircle(
                color = if (on) active else idle,
                radius = if (on) 12.dp.toPx() else 7.dp.toPx(),
                center = dotCenter(i),
            )
        }
    }
}
