package com.shiftsaver.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import com.shiftsaver.model.DownloadState
import com.shiftsaver.ui.components.DownloadCard
import com.shiftsaver.viewmodel.MainUiState
import com.shiftsaver.viewmodel.MainViewModel

// MD3 imports
import androidx.compose.material3.*

// Miuix imports
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextField as MiuixTextField
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun HomeScreen(
    state: MainUiState,
    viewModel: MainViewModel,
    isMiuix: Boolean
) {
    val clipboard = LocalClipboardManager.current

    if (isMiuix) {
        HomeScreenMiuix(state, viewModel, clipboard)
    } else {
        HomeScreenMD3(state, viewModel, clipboard)
    }
}

@Composable
private fun HomeScreenMD3(
    state: MainUiState,
    viewModel: MainViewModel,
    clipboard: androidx.compose.ui.platform.ClipboardManager
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Connection status chip
        val (statusColor, statusText) = when {
            state.connecting -> MaterialTheme.colorScheme.tertiary to "Connecting…"
            state.connected -> MaterialTheme.colorScheme.primary to "Server connected"
            else -> MaterialTheme.colorScheme.error to "Not connected"
        }
        SuggestionChip(
            onClick = { viewModel.testConnection() },
            label = { Text(statusText) },
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = statusColor.copy(alpha = 0.12f),
                labelColor = statusColor
            )
        )

        // URL input card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Paste a URL",
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedTextField(
                    value = state.urlInput,
                    onValueChange = viewModel::onUrlChange,
                    placeholder = { Text("YouTube / TikTok / Instagram URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            val text = clipboard.getText()?.text ?: ""
                            viewModel.onUrlChange(text)
                        }) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                        }
                    }
                )
                Button(
                    onClick = { viewModel.download() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.urlInput.isNotBlank() && state.connected
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Download")
                }
            }
        }

        // Downloads list
        if (state.downloads.isNotEmpty()) {
            Text("Downloads", style = MaterialTheme.typography.titleSmall)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.downloads) { item ->
                    DownloadCard(item = item, isMiuix = false, onRemove = { viewModel.removeDownload(item.id) })
                }
            }
        } else {
            Box(Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                Text("No downloads yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HomeScreenMiuix(
    state: MainUiState,
    viewModel: MainViewModel,
    clipboard: androidx.compose.ui.platform.ClipboardManager
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status row
        val statusText = when {
            state.connecting -> "Connecting…"
            state.connected -> "Server connected ✓"
            else -> "Not connected — tap to retry"
        }
        MiuixCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MiuixText(statusText, style = MiuixTheme.textStyles.body)
                MiuixButton(text = "Retry", onClick = { viewModel.testConnection() }, enabled = !state.connecting)
            }
        }

        // URL input card
        MiuixCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MiuixText("Paste a URL", style = MiuixTheme.textStyles.title)
                MiuixTextField(
                    value = state.urlInput,
                    onValueChange = viewModel::onUrlChange,
                    label = "YouTube / TikTok / Instagram URL",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiuixButton(
                        text = "Paste",
                        onClick = {
                            val text = clipboard.getText()?.text ?: ""
                            viewModel.onUrlChange(text)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    MiuixButton(
                        text = "Download",
                        onClick = { viewModel.download() },
                        modifier = Modifier.weight(1f),
                        enabled = state.urlInput.isNotBlank() && state.connected
                    )
                }
            }
        }

        if (state.downloads.isNotEmpty()) {
            MiuixText("Downloads", style = MiuixTheme.textStyles.title)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.downloads) { item ->
                    DownloadCard(item = item, isMiuix = true, onRemove = { viewModel.removeDownload(item.id) })
                }
            }
        } else {
            Box(Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                MiuixText("No downloads yet", style = MiuixTheme.textStyles.body)
            }
        }
    }
}
