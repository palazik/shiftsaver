package com.shiftsaver.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shiftsaver.model.DownloadItem
import com.shiftsaver.model.DownloadRequest
import com.shiftsaver.model.DownloadState
import com.shiftsaver.model.Platform
import com.shiftsaver.network.ApiClient
import com.shiftsaver.network.PrefsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class MainUiState(
    val host: String = PrefsRepository.DEFAULT_HOST,
    val port: Int = PrefsRepository.DEFAULT_PORT,
    val theme: String = PrefsRepository.DEFAULT_THEME,
    val darkMode: String = PrefsRepository.DEFAULT_DARK,
    val connected: Boolean = false,
    val connecting: Boolean = false,
    val downloads: List<DownloadItem> = emptyList(),
    val urlInput: String = "",
    val snackbarMessage: String? = null
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = PrefsRepository(app)

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                prefs.host,
                prefs.port,
                prefs.theme,
                prefs.darkMode
            ) { host, port, theme, dark ->
                _state.value.copy(host = host, port = port, theme = theme, darkMode = dark)
            }.collect { updated -> _state.value = updated }
        }
    }

    fun onUrlChange(url: String) {
        _state.value = _state.value.copy(urlInput = url)
    }

    fun testConnection() {
        viewModelScope.launch {
            _state.value = _state.value.copy(connecting = true, connected = false)
            try {
                val api = ApiClient.build(_state.value.host, _state.value.port)
                val resp = api.getStatus()
                _state.value = _state.value.copy(
                    connected = resp.isSuccessful,
                    connecting = false,
                    snackbarMessage = if (resp.isSuccessful) "Connected to server ✓" else "Server error: ${resp.code()}"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    connected = false,
                    connecting = false,
                    snackbarMessage = "Cannot reach server: ${e.message}"
                )
            }
        }
    }

    fun saveServer(host: String, port: Int) {
        viewModelScope.launch {
            prefs.saveServer(host, port)
            _state.value = _state.value.copy(host = host, port = port)
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch { prefs.saveTheme(theme) }
    }

    fun setDarkMode(mode: String) {
        viewModelScope.launch { prefs.saveDarkMode(mode) }
    }

    fun download(url: String = _state.value.urlInput) {
        if (url.isBlank()) {
            _state.value = _state.value.copy(snackbarMessage = "Please enter a URL")
            return
        }
        if (!_state.value.connected) {
            _state.value = _state.value.copy(snackbarMessage = "Not connected to server — check settings")
            return
        }
        val id = UUID.randomUUID().toString()
        val platform = Platform.fromUrl(url)
        val item = DownloadItem(
            id = id,
            url = url,
            title = "Downloading…",
            platform = platform,
            state = DownloadState.DOWNLOADING
        )
        addDownload(item)
        _state.value = _state.value.copy(urlInput = "")

        viewModelScope.launch {
            try {
                val api = ApiClient.build(_state.value.host, _state.value.port)
                val resp = api.download(DownloadRequest(url = url))
                if (resp.isSuccessful && resp.body()?.success == true) {
                    val body = resp.body()!!
                    updateDownload(id) { it.copy(
                        state = DownloadState.DONE,
                        title = body.title ?: url,
                        filepath = body.filepath,
                        thumbnail = body.thumbnail,
                        filesize = body.filesize
                    )}
                    _state.value = _state.value.copy(snackbarMessage = "Downloaded: ${body.title ?: "file"}")
                } else {
                    val err = resp.body()?.error ?: "HTTP ${resp.code()}"
                    updateDownload(id) { it.copy(state = DownloadState.ERROR, title = "Error: $err") }
                    _state.value = _state.value.copy(snackbarMessage = "Download failed: $err")
                }
            } catch (e: Exception) {
                updateDownload(id) { it.copy(state = DownloadState.ERROR, title = "Error: ${e.message}") }
                _state.value = _state.value.copy(snackbarMessage = "Error: ${e.message}")
            }
        }
    }

    fun clearSnackbar() {
        _state.value = _state.value.copy(snackbarMessage = null)
    }

    fun removeDownload(id: String) {
        _state.value = _state.value.copy(downloads = _state.value.downloads.filter { it.id != id })
    }

    private fun addDownload(item: DownloadItem) {
        _state.value = _state.value.copy(downloads = listOf(item) + _state.value.downloads)
    }

    private fun updateDownload(id: String, transform: (DownloadItem) -> DownloadItem) {
        _state.value = _state.value.copy(
            downloads = _state.value.downloads.map { if (it.id == id) transform(it) else it }
        )
    }
}
