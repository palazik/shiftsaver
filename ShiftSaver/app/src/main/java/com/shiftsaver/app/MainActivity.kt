package com.shiftsaver.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsEthernet
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

private val Context.dataStore by preferencesDataStore("settings")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShiftSaverApp(SettingsStore(applicationContext), ShiftSaverApi())
        }
    }
}

private enum class ThemeMode(val label: String) {
    White("White"),
    Dark("Dark")
}

private enum class AppTab(val label: String, val icon: ImageVector) {
    Download("Download", Icons.Rounded.CloudDownload),
    Servers("Servers", Icons.Rounded.SettingsEthernet),
    Settings("Settings", Icons.Rounded.Settings),
    About("About", Icons.Rounded.Info)
}

private data class Settings(
    val host: String = "192.168.1.10",
    val port: String = "8787",
    val themeMode: ThemeMode = ThemeMode.White
)

private data class DownloadJob(
    val id: String,
    val status: String,
    val title: String?,
    val fileName: String?,
    val error: String?
)

private class SettingsStore(private val context: Context) {
    private val hostKey = stringPreferencesKey("host")
    private val portKey = stringPreferencesKey("port")
    private val themeKey = stringPreferencesKey("theme")

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            host = prefs[hostKey] ?: "192.168.1.10",
            port = prefs[portKey] ?: "8787",
            themeMode = ThemeMode.entries.firstOrNull { it.name == prefs[themeKey] } ?: ThemeMode.White
        )
    }

    suspend fun save(settings: Settings) {
        context.dataStore.edit { prefs ->
            prefs[hostKey] = settings.host
            prefs[portKey] = settings.port
            prefs[themeKey] = settings.themeMode.name
        }
    }
}

private class ShiftSaverApi {
    private val json = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun health(baseUrl: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$baseUrl/health").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            JSONObject(response.body?.string().orEmpty()).optString("version", "unknown")
        }
    }

    suspend fun startDownload(baseUrl: String, url: String): DownloadJob = withContext(Dispatchers.IO) {
        val body = JSONObject().put("url", url).toString().toRequestBody(json)
        val request = Request.Builder().url("$baseUrl/downloads").post(body).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException(response.body?.string().orEmpty().ifBlank { "HTTP ${response.code}" })
            }
            parseJob(JSONObject(response.body?.string().orEmpty()))
        }
    }

    suspend fun getJob(baseUrl: String, id: String): DownloadJob = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$baseUrl/downloads/$id").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            parseJob(JSONObject(response.body?.string().orEmpty()))
        }
    }

    private fun parseJob(json: JSONObject) = DownloadJob(
        id = json.optString("id"),
        status = json.optString("status"),
        title = json.optString("title").ifBlank { null },
        fileName = json.optString("file_name").ifBlank { null },
        error = json.optString("error").ifBlank { null }
    )
}

@Composable
private fun ShiftSaverApp(store: SettingsStore, api: ShiftSaverApi) {
    val settings by store.settings.collectAsState(initial = Settings())
    ShiftSaverTheme(settings.themeMode) {
        ShiftSaverScreen(settings, store, api)
    }
}

@Composable
private fun ShiftSaverTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val white = lightColorScheme(
        primary = Color(0xFF0A84FF),
        secondary = Color(0xFF34C759),
        tertiary = Color(0xFFFF9500),
        background = Color(0xFFF2F2F7),
        surface = Color(0xFFF2F2F7),
        surfaceContainer = Color.White,
        onSurface = Color(0xFF111113),
        onSurfaceVariant = Color(0xFF6E6E73)
    )
    val dark = darkColorScheme(
        primary = Color(0xFF0A84FF),
        secondary = Color(0xFF30D158),
        tertiary = Color(0xFFFF9F0A),
        background = Color(0xFF000000),
        surface = Color(0xFF000000),
        surfaceContainer = Color(0xFF1C1C1E),
        onSurface = Color(0xFFF5F5F7),
        onSurfaceVariant = Color(0xFF98989D)
    )
    MaterialTheme(colorScheme = if (themeMode == ThemeMode.Dark) dark else white, content = content)
}

@Composable
private fun ShiftSaverScreen(settings: Settings, store: SettingsStore, api: ShiftSaverApi) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var draft by remember(settings) { mutableStateOf(settings) }
    var currentTab by remember { mutableStateOf(AppTab.Download) }
    var url by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Server not checked") }
    var busy by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<DownloadJob?>(null) }
    val baseUrl = remember(draft.host, draft.port) { "http://${draft.host.trim()}:${draft.port.trim()}" }

    fun saveDraft() {
        scope.launch { store.save(draft) }
    }

    fun testServer() {
        busy = true
        scope.launch {
            runCatching { api.health(baseUrl) }
                .onSuccess { message = "Connected to ShiftSaver server $it" }
                .onFailure { message = it.message ?: "Connection failed" }
            busy = false
        }
    }

    LaunchedEffect(job?.id, job?.status) {
        val current = job
        if (current != null && current.status in setOf("queued", "running")) {
            delay(1600)
            runCatching { api.getJob(baseUrl, current.id) }
                .onSuccess { job = it }
                .onFailure { message = it.message ?: "Could not refresh job" }
        }
    }

    Scaffold(
        bottomBar = {
            MiuixTabBar(
                selected = currentTab,
                onSelected = { currentTab = it },
                dark = draft.themeMode == ThemeMode.Dark
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PageHeader(tab = currentTab, baseUrl = baseUrl)
            when (currentTab) {
                AppTab.Download -> DownloadTab(
                    url = url,
                    job = job,
                    busy = busy,
                    baseUrl = baseUrl,
                    onUrl = { url = it },
                    onPaste = { pasteUrl(clipboard)?.let { url = it } },
                    onDownload = {
                        if (url.isBlank()) {
                            message = "Paste a public video or photo URL first"
                            return@DownloadTab
                        }
                        busy = true
                        scope.launch {
                            runCatching { api.startDownload(baseUrl, url.trim()) }
                                .onSuccess {
                                    job = it
                                    message = "Download started"
                                }
                                .onFailure { message = it.message ?: "Download failed" }
                            busy = false
                        }
                    },
                    onOpen = { fileUrl -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl))) }
                )
                AppTab.Servers -> ServersTab(
                    settings = draft,
                    message = message,
                    busy = busy,
                    onHost = { draft = draft.copy(host = it) },
                    onPort = { draft = draft.copy(port = it) },
                    onSave = { saveDraft() },
                    onTest = { testServer() },
                    onPreset = { host, port ->
                        draft = draft.copy(host = host, port = port)
                        scope.launch { store.save(draft.copy(host = host, port = port)) }
                    }
                )
                AppTab.Settings -> SettingsTab(
                    settings = draft,
                    onTheme = { mode ->
                        draft = draft.copy(themeMode = mode)
                        scope.launch { store.save(draft.copy(themeMode = mode)) }
                    }
                )
                AppTab.About -> AboutTab(baseUrl = baseUrl)
            }
        }
    }
}

@Composable
private fun PageHeader(tab: AppTab, baseUrl: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(tab.label, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(
            baseUrl,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MiuixTabBar(selected: AppTab, onSelected: (AppTab) -> Unit, dark: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (dark) Color(0xFF111113) else Color.White)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AppTab.entries.forEach { tab ->
            val active = selected == tab
            val bg = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent
            val fg = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(bg)
                    .clickable { onSelected(tab) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(tab.icon, null, tint = fg, modifier = Modifier.size(22.dp))
                Text(tab.label, color = fg, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

@Composable
private fun DownloadTab(
    url: String,
    job: DownloadJob?,
    busy: Boolean,
    baseUrl: String,
    onUrl: (String) -> Unit,
    onPaste: () -> Unit,
    onDownload: () -> Unit,
    onOpen: (String) -> Unit
) {
    HeroPanel()
    MiuixGroup {
        Text("Media link", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = url,
            onValueChange = onUrl,
            label = { Text("TikTok, YouTube, Instagram URL") },
            leadingIcon = { Icon(Icons.Rounded.Link, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiuixButton(onClick = onPaste, enabled = !busy, tonal = true) {
                Icon(Icons.Rounded.ContentPaste, null)
                Spacer(Modifier.width(8.dp))
                Text("Paste")
            }
            MiuixButton(onClick = onDownload, enabled = !busy) {
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start")
                }
            }
        }
    }
    job?.let { JobPanel(baseUrl, it, onOpen) }
    NoticeCard()
}

@Composable
private fun ServersTab(
    settings: Settings,
    message: String,
    busy: Boolean,
    onHost: (String) -> Unit,
    onPort: (String) -> Unit,
    onSave: () -> Unit,
    onTest: () -> Unit,
    onPreset: (String, String) -> Unit
) {
    MiuixGroup {
        Text("Active server", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = settings.host,
            onValueChange = onHost,
            label = { Text("IP address") },
            leadingIcon = { Icon(Icons.Rounded.WifiTethering, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = settings.port,
            onValueChange = onPort,
            label = { Text("Port") },
            leadingIcon = { Icon(Icons.Rounded.SettingsEthernet, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiuixButton(onClick = onSave, enabled = !busy, tonal = true) { Text("Save") }
            MiuixButton(onClick = onTest, enabled = !busy) {
                if (busy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Test")
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
    MiuixGroup {
        Text("Quick servers", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))
        ServerPreset("Home LAN", "192.168.1.10", "8787", onPreset)
        ServerPreset("Laptop hotspot", "192.168.43.1", "8787", onPreset)
        ServerPreset("Local test", "10.0.2.2", "8787", onPreset)
    }
    MiuixGroup {
        Text("Linux command", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("./linux/install.sh", style = MaterialTheme.typography.titleMedium)
        Text("The script prints the IP and port to use here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ServerPreset(label: String, host: String, port: String, onPreset: (String, String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onPreset(host, port) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.WifiTethering, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text("$host:$port", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsTab(settings: Settings, onTheme: (ThemeMode) -> Unit) {
    MiuixGroup {
        Text("Theme", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = settings.themeMode == mode,
                    onClick = { onTheme(mode) },
                    label = { Text(mode.label) },
                    leadingIcon = if (settings.themeMode == mode) {
                        { Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(18.dp)) }
                    } else null
                )
            }
        }
    }
    MiuixGroup {
        Text("Interface", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("MIUIX only", style = MaterialTheme.typography.titleMedium)
        Text(
            "ShiftSaver now uses one MIUIX-inspired interface with separate white and dark themes.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AboutTab(baseUrl: String) {
    MiuixGroup {
        Icon(Icons.Rounded.CloudDownload, null, modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text("ShiftSaver", style = MaterialTheme.typography.headlineSmall)
        Text("Version 0.1.0", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Text(
            "Android companion app for a self-hosted Linux media downloader on your local network.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    MiuixGroup {
        Text("Current server", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(baseUrl, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    NoticeCard()
}

@Composable
private fun HeroPanel() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF0A84FF), Color(0xFF5AC8FA), Color(0xFF34C759))))
            .padding(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.CloudDownload, null, tint = Color.White, modifier = Modifier.size(36.dp))
            Text("Quick Save", color = Color.White, style = MaterialTheme.typography.headlineSmall)
            Text("Paste a public link and let your Linux server handle the download.", color = Color.White.copy(alpha = 0.9f))
        }
    }
}

@Composable
private fun JobPanel(baseUrl: String, job: DownloadJob, open: (String) -> Unit) {
    val done = job.status == "done" && job.fileName != null
    MiuixGroup {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(if (job.error == null) Icons.Rounded.CloudDownload else Icons.Rounded.Error, null)
            Column(Modifier.weight(1f)) {
                Text(job.title ?: "Media job", style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(job.status.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        job.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        if (done) {
            Spacer(Modifier.height(12.dp))
            MiuixButton(onClick = { open("$baseUrl/files/${Uri.encode(job.fileName)}") }) {
                Text("Open file")
            }
        }
    }
}

@Composable
private fun NoticeCard() {
    MiuixGroup {
        Text("Responsible use", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            "Use ShiftSaver only for your own media or media you have permission to save. Private, DRM-protected, login-only, or paywalled content is not supported.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MiuixGroup(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(26.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

@Composable
private fun MiuixButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    tonal: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (tonal) {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            contentColor = MaterialTheme.colorScheme.primary
        )
    } else {
        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White)
    }
    Button(onClick = onClick, enabled = enabled, colors = colors, shape = RoundedCornerShape(18.dp), content = { content() })
}

private fun pasteUrl(clipboard: ClipboardManager): String? {
    return clipboard.getText()?.text?.trim()?.takeIf {
        it.startsWith("http://") || it.startsWith("https://")
    }
}
