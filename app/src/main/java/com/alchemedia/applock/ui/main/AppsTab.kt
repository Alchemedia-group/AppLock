package com.alchemedia.applock.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.alchemedia.applock.R
import com.alchemedia.applock.data.AppSettings
import com.alchemedia.applock.security.SetupStatus
import com.alchemedia.applock.ui.components.AppIcon
import com.alchemedia.applock.ui.components.SectionLabel

@Composable
fun AppsTab(
    settings: AppSettings,
    apps: List<AppEntry>?,
    status: SetupStatus,
    onToggle: (String, Boolean) -> Unit,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val openAccessibility = openAccessibilitySettings()
    val visible = remember(apps, query) {
        apps.orEmpty().filter { it.label.contains(query.trim(), ignoreCase = true) }
    }
    val (protectedApps, otherApps) = visible.partition { it.packageName in settings.lockedPackages }

    if (apps == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            ProtectionHero(
                active = status.accessibilityEnabled,
                lockedCount = settings.lockedPackages.size,
                onTurnOn = openAccessibility,
                onHelp = onHelp,
            )
        }
        if (settings.captureIntruders && !status.cameraGranted) {
            item { CameraPermissionBanner() }
        }
        item {
            val colors = MaterialTheme.colorScheme
            TextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text(stringResource(R.string.search_apps)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.surfaceContainer,
                    unfocusedContainerColor = colors.surfaceContainer,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }

        if (visible.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.apps_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(32.dp),
                )
            }
        }
        if (protectedApps.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.section_protected) + "  ·  " + protectedApps.size) }
            items(protectedApps, key = { "p-" + it.packageName }) { AppRow(it, true, onToggle) }
        }
        if (otherApps.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.section_other)) }
            items(otherApps, key = { "o-" + it.packageName }) { AppRow(it, false, onToggle) }
        }
    }
}

@Composable
private fun AppRow(app: AppEntry, locked: Boolean, onToggle: (String, Boolean) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.medium,
        color = if (locked) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            AppIcon(app.icon, size = 44.dp)
            Spacer(Modifier.width(14.dp))
            Text(
                app.label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = locked, onCheckedChange = { onToggle(app.packageName, it) })
        }
    }
}
