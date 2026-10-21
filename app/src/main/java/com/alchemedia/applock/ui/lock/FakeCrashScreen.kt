package com.alchemedia.applock.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alchemedia.applock.R

/**
 * Looks like Android's "app keeps stopping" dialog. Tapping either button just goes home;
 * a long-press on "Close app" reveals the real lock screen.
 */
@Composable
fun FakeCrashScreen(
    appLabel: String,
    onClose: () -> Unit,
    onReveal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val text = Color(0xFFE3E3E8)
    val link = Color(0xFF9CC0FF)
    Box(
        modifier
            .fillMaxSize()
            .background(Color(0xFF0C0D10)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF2A2C31),
            modifier = Modifier
                .padding(32.dp)
                .widthIn(max = 340.dp),
        ) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.fake_crash_title, appLabel), color = text, fontSize = 22.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.fake_crash_app_info),
                    color = link,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(role = Role.Button, onClick = onClose)
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                )
                Text(
                    stringResource(R.string.fake_crash_close),
                    color = link,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .semantics { role = Role.Button }
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { onClose() }, onLongPress = { onReveal() })
                        }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                )
            }
        }
    }
}
