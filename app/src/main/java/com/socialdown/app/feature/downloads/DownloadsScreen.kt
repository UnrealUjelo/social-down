package com.socialdown.app.feature.downloads

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.socialdown.app.core.database.DownloadEntity
import com.socialdown.app.core.model.DownloadStatus
import java.util.Locale

@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel,
    contentPadding: PaddingValues,
    onGoHome: () -> Unit,
) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val active = downloads.filter { it.status.isActive() }
    val recent = downloads.filterNot { it.status.isActive() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = contentPadding.calculateTopPadding() + 22.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Downloads", style = MaterialTheme.typography.headlineMedium) }
        if (downloads.isEmpty()) {
            item { EmptyDownloads(onGoHome) }
        } else {
            if (active.isNotEmpty()) {
                item { SectionTitle("Active") }
                items(active, key = DownloadEntity::id) { item ->
                    ActiveDownloadCard(item, onCancel = { viewModel.cancel(item.id) })
                }
            }
            if (recent.isNotEmpty()) {
                item {
                    if (active.isNotEmpty()) Spacer(Modifier.height(4.dp))
                    SectionTitle(if (recent.any { it.status == DownloadStatus.COMPLETED }) "Completed" else "Recent")
                }
                items(recent, key = DownloadEntity::id) { item ->
                    CompletedDownloadCard(item, onDelete = { viewModel.delete(item.id) })
                }
            }
        }
    }
}

@Composable
private fun EmptyDownloads(onGoHome: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Icon(
                Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.padding(20.dp).size(36.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text("No downloads yet", style = MaterialTheme.typography.titleLarge)
        Button(onClick = onGoHome) { Text("Paste a link") }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ActiveDownloadCard(item: DownloadEntity, onCancel: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            DownloadIdentity(item)
            LinearProgressIndicator(
                progress = { item.progressPercent / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.status.label(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.weight(1f))
                item.etaSeconds?.takeIf { it > 0 }?.let {
                    Text(
                        formatEta(it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Text("${item.progressPercent}%", style = MaterialTheme.typography.labelLarge)
            }
            OutlinedButton(onClick = onCancel, modifier = Modifier.align(Alignment.End)) {
                Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun CompletedDownloadCard(item: DownloadEntity, onDelete: () -> Unit) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(94.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    when (item.status) {
                        DownloadStatus.COMPLETED -> listOf(item.qualityLabel, item.formatLabel)
                            .filter(String::isNotBlank)
                            .joinToString(" · ")
                        DownloadStatus.FAILED -> item.errorMessage ?: "Failed"
                        DownloadStatus.CANCELED -> "Canceled"
                        else -> item.status.label()
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.status == DownloadStatus.FAILED) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Download actions")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (item.status == DownloadStatus.COMPLETED && item.outputUri != null) {
                        DropdownMenuItem(
                            text = { Text("Open") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                openMedia(context, item.outputUri)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Share") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                shareMedia(context, item.outputUri)
                            },
                        )
                        HorizontalDivider()
                    }
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadIdentity(item: DownloadEntity) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = item.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(94.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(11.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val details = listOf(item.qualityLabel, item.formatLabel)
                .filter(String::isNotBlank)
                .joinToString(" · ")
            if (details.isNotBlank()) {
                Text(
                    details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun DownloadStatus.isActive() = this in setOf(
    DownloadStatus.QUEUED,
    DownloadStatus.ANALYZING,
    DownloadStatus.DOWNLOADING,
    DownloadStatus.MERGING,
    DownloadStatus.CONVERTING,
)

private fun DownloadStatus.label(): String = when (this) {
    DownloadStatus.QUEUED -> "Queued"
    DownloadStatus.ANALYZING -> "Analyzing"
    DownloadStatus.DOWNLOADING -> "Downloading"
    DownloadStatus.MERGING -> "Merging"
    DownloadStatus.CONVERTING -> "Converting"
    DownloadStatus.COMPLETED -> "Done"
    DownloadStatus.FAILED -> "Failed"
    DownloadStatus.CANCELED -> "Canceled"
}

private fun formatEta(seconds: Long): String {
    val minutes = seconds / 60
    val remaining = seconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, remaining)
}

private fun openMedia(context: Context, value: String) {
    val uri = value.toUri()
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(intent) }
}

private fun shareMedia(context: Context, value: String) {
    val uri = value.toUri()
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = context.contentResolver.getType(uri) ?: "*/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Share download"))
    }
}
