package com.socialdown.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.socialdown.app.core.common.UrlValidator
import com.socialdown.app.feature.downloads.DownloadsScreen
import com.socialdown.app.feature.downloads.DownloadsViewModel
import com.socialdown.app.feature.home.HomeScreen
import com.socialdown.app.feature.home.HomeViewModel
import com.socialdown.app.feature.settings.SettingsScreen
import com.socialdown.app.feature.settings.SettingsViewModel
import com.socialdown.app.ui.theme.SocialDownTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var sharedUrl by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sharedUrl = intent.extractUrl()
        setContent {
            val mainViewModel: MainViewModel = viewModel()
            val themeMode by mainViewModel.themeMode.collectAsStateWithLifecycle()
            SocialDownTheme(themeMode) {
                SocialDownApp(
                    sharedUrl = sharedUrl,
                    onSharedUrlConsumed = { sharedUrl = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sharedUrl = intent.extractUrl()
    }

    private fun Intent.extractUrl(): String? {
        val shared = if (action == Intent.ACTION_SEND && type == "text/plain") {
            getStringExtra(Intent.EXTRA_TEXT)
        } else {
            dataString
        }
        return UrlValidator.firstUrl(shared)
    }
}

private enum class Destination(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Outlined.Home),
    DOWNLOADS("Downloads", Icons.Outlined.Download),
    SETTINGS("Settings", Icons.Outlined.Settings),
}

@Composable
private fun SocialDownApp(
    sharedUrl: String?,
    onSharedUrlConsumed: () -> Unit,
) {
    var destination by rememberSaveable { mutableStateOf(Destination.HOME) }
    val homeViewModel: HomeViewModel = viewModel()
    val downloadsViewModel: DownloadsViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val context = LocalContext.current
    var pendingDownload by remember { mutableStateOf<(() -> Unit)?>(null) }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        pendingDownload?.invoke()
        pendingDownload = null
    }

    LaunchedEffect(sharedUrl) {
        sharedUrl?.let {
            destination = Destination.HOME
            homeViewModel.acceptSharedUrl(it)
            onSharedUrlConsumed()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { destination = item },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { contentPadding ->
        when (destination) {
            Destination.HOME -> HomeScreen(
                viewModel = homeViewModel,
                contentPadding = contentPadding,
                onRequestDownload = {
                    val download = { homeViewModel.enqueueDownload() }
                    val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) != PackageManager.PERMISSION_GRANTED
                    if (needsPermission) {
                        pendingDownload = download
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        download()
                    }
                },
                onShowDownloads = { destination = Destination.DOWNLOADS },
            )
            Destination.DOWNLOADS -> DownloadsScreen(
                viewModel = downloadsViewModel,
                contentPadding = contentPadding,
                onGoHome = { destination = Destination.HOME },
            )
            Destination.SETTINGS -> SettingsScreen(
                viewModel = settingsViewModel,
                contentPadding = contentPadding,
            )
        }
    }
}

