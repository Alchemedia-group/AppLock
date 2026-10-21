package com.alchemedia.applock.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.alchemedia.applock.R

/**
 * The AppLock shield. [size] is the height of the visible shield; the drawing has padding built in,
 * so it is scaled up to fill that height, and an optional mint glow sits behind it.
 */
@Composable
fun AppLogo(size: Dp, modifier: Modifier = Modifier, glow: Boolean = true) {
    val glowColor = MaterialTheme.colorScheme.primary
    Box(
        modifier
            .size(size)
            .then(
                if (glow) {
                    Modifier.drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(listOf(glowColor.copy(alpha = 0.38f), Color.Transparent)),
                            radius = this.size.minDimension * 0.95f,
                        )
                    }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_logo),
            contentDescription = null,
            modifier = Modifier.requiredSize(size * 1.72f),
        )
    }
}
