package com.shiftsaver.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shiftsaver.model.DownloadState
import com.shiftsaver.ui.components.DownloadCard
import com.shiftsaver.viewmodel.MainUiState
import com.shiftsaver.viewmodel.MainViewModel
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun HistoryScreen(
    state: MainUiState,
    viewModel: MainViewModel,
    isMiuix: Boolean
) {
    val done = state.downloads.filter { it.state == DownloadState.DONE || it.state == DownloadState.ERROR }

    if (done.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isMiuix) {
                MiuixText("No history yet", style = MiuixTheme.textStyles.body)
            } else {
                Text("No history yet", style = MaterialTheme.typography.bodyMedium)
            }
        }
    } else {
        LazyColumn(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(done) { item ->
                DownloadCard(item = item, isMiuix = isMiuix, onRemove = { viewModel.removeDownload(item.id) })
            }
        }
    }
}
