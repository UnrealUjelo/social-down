package com.socialdown.app.feature.home

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.socialdown.app.R
import com.socialdown.app.core.common.FormatNormalizer
import com.socialdown.app.core.common.UrlValidator
import com.socialdown.app.core.model.AudioOutput
import com.socialdown.app.core.model.MediaInfo
import com.socialdown.app.core.model.MediaKind
import com.socialdown.app.core.model.VideoFormat
import com.socialdown.app.ui.theme.Blue
import com.socialdown.app.ui.theme.Indigo
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    contentPadding: PaddingValues,
    onRequestDownload: () -> Unit,
    onShowDownloads: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.queuedMessage) {
        val message = state.queuedMessage ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(message, actionLabel = "View")
        viewModel.consumeQueuedMessage()
        if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) onShowDownloads()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = contentPadding.calculateTopPadding() + 18.dp,
                end = 20.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { BrandHeader() }
            item {
                UrlEntry(
                    value = state.url,
                    isLoading = state.analysis is AnalysisState.Loading,
                    onValueChange = viewModel::setUrl,
                    onPaste = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val text = clipboard.primaryClip
                            ?.getItemAt(0)
                            ?.coerceToText(context)
                            ?.toString()
                        UrlValidator.firstUrl(text)?.let(viewModel::setUrl)
                    },
                    onAnalyze = viewModel::analyze,
                )
            }
            item {
                AnimatedContent(
                    targetState = state.analysis,
                    contentKey = { it::class },
                    label = "analysis result",
                ) { analysis ->
                    when (analysis) {
                        AnalysisState.Idle -> IdleHint()
                        AnalysisState.Loading -> LoadingCard()
                        is AnalysisState.Error -> ErrorCard(analysis.message)
                        is AnalysisState.Ready -> MediaOptions(
                            state = state,
                            media = analysis.media,
                            onKindSelected = viewModel::selectKind,
                            onHeightSelected = viewModel::selectHeight,
                            onContainerSelected = viewModel::selectContainer,
                            onAudioOutputSelected = viewModel::selectAudioOutput,
                            onBitrateSelected = viewModel::selectBitrate,
                            onDownload = onRequestDownload,
                        )
                    }
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = contentPadding.calculateBottomPadding() + 8.dp),
        )
    }
}

@Composable
private fun BrandHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.social_down_icon),
            contentDescription = null,
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(16.dp)),
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text("Social Down", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Video & audio downloader",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun UrlEntry(
    value: String,
    isLoading: Boolean,
    onValueChange: (String) -> Unit,
    onPaste: () -> Unit,
    onAnalyze: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true,
            placeholder = { Text("Paste a link…") },
            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = onPaste, enabled = !isLoading) {
                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste from clipboard")
                }
            },
            shape = RoundedCornerShape(18.dp),
        )
        Button(
            onClick = onAnalyze,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = Blue,
                contentColor = androidx.compose.ui.graphics.Color.White,
            ),
            shape = RoundedCornerShape(17.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(Icons.Default.Search, contentDescription = null)
            }
            Spacer(Modifier.width(10.dp))
            Text(if (isLoading) "Analyzing" else "Analyze")
        }
    }
}

@Composable
private fun IdleHint() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Only save media you’re allowed to download",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LoadingCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(118.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Text("Finding available formats…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(18.dp),
    ) {
        Text(
            message,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun MediaOptions(
    state: HomeUiState,
    media: MediaInfo,
    onKindSelected: (MediaKind) -> Unit,
    onHeightSelected: (Int) -> Unit,
    onContainerSelected: (String) -> Unit,
    onAudioOutputSelected: (AudioOutput) -> Unit,
    onBitrateSelected: (Int) -> Unit,
    onDownload: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        MediaPreview(media)
        KindSelector(
            selected = state.selectedKind,
            hasVideo = media.videoFormats.isNotEmpty(),
            hasAudio = media.audioFormats.isNotEmpty(),
            onSelected = onKindSelected,
        )
        if (state.selectedKind == MediaKind.VIDEO) {
            VideoOptions(
                media = media,
                selectedHeight = state.selectedHeight,
                selectedContainer = state.selectedContainer,
                onHeightSelected = onHeightSelected,
                onContainerSelected = onContainerSelected,
            )
        } else {
            AudioOptions(
                selectedOutput = state.selectedAudioOutput,
                selectedBitrate = state.selectedBitrate,
                onOutputSelected = onAudioOutputSelected,
                onBitrateSelected = onBitrateSelected,
            )
        }
        val selectedSize = media.videoFormats
            .firstOrNull { it.height == state.selectedHeight }
            ?.fileSizeBytes
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            selectedSize?.let {
                Text(
                    "~${formatBytes(it)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 7.dp),
                )
            }
            Button(
                onClick = onDownload,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Blue,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                ),
                shape = RoundedCornerShape(18.dp),
                enabled = state.selectedKind == MediaKind.AUDIO || state.selectedHeight != null,
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Download")
            }
        }
    }
}

@Composable
private fun MediaPreview(media: MediaInfo) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = media.thumbnailUrl,
                contentDescription = "Media thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(142.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(13.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    media.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                media.source?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                media.durationSeconds?.let {
                    Text(
                        formatDuration(it),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun KindSelector(
    selected: MediaKind,
    hasVideo: Boolean,
    hasAudio: Boolean,
    onSelected: (MediaKind) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            Segment(
                text = "Video",
                icon = { Icon(Icons.Default.VideoLibrary, contentDescription = null) },
                selected = selected == MediaKind.VIDEO,
                enabled = hasVideo,
                modifier = Modifier.weight(1f),
                onClick = { onSelected(MediaKind.VIDEO) },
            )
            Segment(
                text = "Audio",
                icon = { Icon(Icons.Default.Audiotrack, contentDescription = null) },
                selected = selected == MediaKind.AUDIO,
                enabled = hasAudio,
                modifier = Modifier.weight(1f),
                onClick = { onSelected(MediaKind.AUDIO) },
            )
        }
    }
}

@Composable
private fun Segment(
    text: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent,
        tonalElevation = if (selected) 2.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(vertical = 11.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Spacer(Modifier.width(8.dp))
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun VideoOptions(
    media: MediaInfo,
    selectedHeight: Int?,
    selectedContainer: String?,
    onHeightSelected: (Int) -> Unit,
    onContainerSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("Quality")
        media.videoFormats.forEach { format ->
            val height = format.height ?: return@forEach
            QualityRow(
                format = format,
                selected = height == selectedHeight,
                onClick = { onHeightSelected(height) },
            )
        }
        Spacer(Modifier.height(2.dp))
        SectionLabel("Format")
        val containers = FormatNormalizer.availableContainers(media.videoFormats, selectedHeight)
        ChipRow(
            items = containers,
            selected = selectedContainer,
            label = String::uppercase,
            onSelected = onContainerSelected,
        )
    }
}

@Composable
private fun QualityRow(format: VideoFormat, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = null)
            Spacer(Modifier.width(5.dp))
            Text(
                "${format.height}p",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.width(64.dp),
            )
            ResolutionBadge(format.height ?: 0)
            Spacer(Modifier.weight(1f))
            Text(
                format.fileSizeBytes?.let(::formatBytes).orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ResolutionBadge(height: Int) {
    val label = when {
        height >= 2160 -> "4K"
        height >= 1440 -> "QHD"
        height >= 1080 -> "Full HD"
        height >= 720 -> "HD"
        else -> null
    } ?: return
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(7.dp),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun AudioOptions(
    selectedOutput: AudioOutput,
    selectedBitrate: Int,
    onOutputSelected: (AudioOutput) -> Unit,
    onBitrateSelected: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("Format")
        ChipRow(
            items = AudioOutput.entries,
            selected = selectedOutput,
            label = AudioOutput::label,
            onSelected = onOutputSelected,
        )
        AnimatedVisibility(selectedOutput in setOf(AudioOutput.MP3, AudioOutput.M4A)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Spacer(Modifier.height(2.dp))
                SectionLabel("Quality")
                ChipRow(
                    items = listOf(128, 192, 256, 320),
                    selected = selectedBitrate,
                    label = { "$it kbps" },
                    onSelected = onBitrateSelected,
                )
            }
        }
        AnimatedVisibility(selectedOutput in setOf(AudioOutput.FLAC, AudioOutput.WAV)) {
            Text(
                "Lossless output · source quality preserved",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun <T> ChipRow(
    items: List<T>,
    selected: T?,
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
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, remainingSeconds)
    else "%d:%02d".format(minutes, remainingSeconds)
}

private fun formatBytes(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) String.format(Locale.US, "%.1f GB", mb / 1024)
    else String.format(Locale.US, "%.0f MB", mb)
}
