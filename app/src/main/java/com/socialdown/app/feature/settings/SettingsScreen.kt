package com.socialdown.app.feature.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.socialdown.app.BuildConfig
import com.socialdown.app.core.preferences.ThemeMode

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    contentPadding: PaddingValues,
) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = contentPadding.calculateTopPadding() + 22.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { Text("Settings", style = MaterialTheme.typography.headlineMedium) }
        item {
            SettingGroup("Theme") {
                SettingsChipRow(
                    items = ThemeMode.entries,
                    selected = preferences.themeMode,
                    label = { it.name.lowercase().replaceFirstChar(Char::uppercase) },
                    onSelected = viewModel::setTheme,
                )
            }
        }
        item {
            SettingGroup("Default video quality") {
                SettingsChipRow(
                    items = listOf(720, 1080, 1440, 2160),
                    selected = preferences.defaultVideoHeight,
                    label = { if (it == 2160) "4K" else "${it}p" },
                    onSelected = viewModel::setVideoHeight,
                )
            }
        }
        item {
            SettingGroup("Default audio format") {
                SettingsChipRow(
                    items = listOf("Original", "MP3", "M4A", "FLAC", "WAV"),
                    selected = preferences.defaultAudioFormat,
                    label = { it },
                    onSelected = viewModel::setAudioFormat,
                )
            }
        }
        item {
            InfoRow(
                icon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                title = "Download location",
                value = "Downloads/Social Down",
            )
        }
        item {
            InfoRow(
                icon = { Icon(Icons.Outlined.Shield, contentDescription = null) },
                title = "Privacy",
                value = "Local only · no analytics",
            )
        }
        item {
            InfoRow(
                icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                title = "About",
                value = "Social Down ${BuildConfig.VERSION_NAME} · Open source",
            )
        }
    }
}

@Composable
private fun SettingGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun <T> SettingsChipRow(
    items: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            FilterChip(
                selected = item == selected,
                onClick = { onSelected(item) },
                label = { Text(label(item)) },
                leadingIcon = if (item == selected) {
                    { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(17.dp)) }
                } else null,
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: @Composable () -> Unit,
    title: String,
    value: String,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                androidx.compose.foundation.layout.Box(Modifier.padding(10.dp)) { icon() }
            }
            Spacer(Modifier.width(13.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
