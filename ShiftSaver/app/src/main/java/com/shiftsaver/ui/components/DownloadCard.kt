package com.shiftsaver.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shiftsaver.model.DownloadItem
import com.shiftsaver.model.DownloadState
import top.yukonga.miuix.kmp.miuix.Card as MiuixCard
import top.yukonga.miuix.kmp.miuix.Text as MiuixText
import top.yukonga.miuix.kmp.miuix.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.miuix.Icon as MiuixIcon
import top.yukonga.miuix.kmp.miuix.MiuixTheme

@Composable
fun DownloadCard(item: DownloadItem, isMiuix: Boolean, onRemove: () -> Unit) {
    if (isMiuix) DownloadCardMiuix(item, onRemove)
    else DownloadCardMD3(item, onRemove)
}

@Composable
private fun DownloadCardMD3(item: DownloadItem, onRemove: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StateIcon(item.state, isMiuix = false)
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${item.platform.label} · ${item.state.name.lowercase().replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                item.filesize?.let { Text(formatSize(it), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            if (item.state == DownloadState.DONE || item.state == DownloadState.ERROR) {
                IconButton(onClick = onRemove) { Icon(Icons.Default.Close, contentDescription = "Remove") }
            } else {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }
    }
}

@Composable
private fun DownloadCardMiuix(item: DownloadItem, onRemove: () -> Unit) {
    MiuixCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StateIcon(item.state, isMiuix = true)
            Column(Modifier.weight(1f)) {
                MiuixText(item.title, style = MiuixTheme.textStyles.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                MiuixText("${item.platform.label} · ${item.state.name.lowercase().replaceFirstChar { it.uppercase() }}", style = MiuixTheme.textStyles.footnote)
                item.filesize?.let { MiuixText(formatSize(it), style = MiuixTheme.textStyles.footnote) }
            }
            if (item.state == DownloadState.DONE || item.state == DownloadState.ERROR) {
                MiuixIconButton(onClick = onRemove) { MiuixIcon(Icons.Default.Close, contentDescription = "Remove") }
            } else {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }
    }
}

@Composable
private fun StateIcon(state: DownloadState, isMiuix: Boolean) {
    val (icon, tint): Pair<ImageVector, Color> = when (state) {
        DownloadState.DONE -> Icons.Default.CheckCircle to Color(0xFF4CAF50)
        DownloadState.ERROR -> Icons.Default.Error to Color(0xFFF44336)
        DownloadState.DOWNLOADING -> Icons.Default.HourglassEmpty to Color(0xFF2196F3)
        DownloadState.QUEUED -> Icons.Default.HourglassEmpty to Color(0xFF9E9E9E)
    }
    if (isMiuix) MiuixIcon(icon, contentDescription = null, tint = tint)
    else Icon(icon, contentDescription = null, tint = tint)
}

private fun formatSize(bytes: Long): String = when {
    bytes > 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    bytes > 1_000 -> "%.1f KB".format(bytes / 1_000.0)
    else -> "$bytes B"
}
