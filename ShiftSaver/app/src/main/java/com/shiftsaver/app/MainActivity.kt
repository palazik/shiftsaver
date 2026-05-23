package com.shiftsaver.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SettingsEthernet
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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

private enum class DesignStyle(val label: String) {
    Miuix("MIUIX"),
    Material3("Material 3")
}

private data class Settings(
    val host: String = "192.168.1.10",
    val port: String = "8787",
    val style: DesignStyle = DesignStyle.Miuix
)

private class SettingsStore(private val context: Context) {
    private val hostKey = stringPreferencesKey("host")
    private val portKey = stringPreferencesKey("port")
    private val styleKey = stringPreferencesKey("style")

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            host = prefs[hostKey] ?: "192.168.1.10",
            port = prefs[portKey] ?: "8787",
            style = DesignStyle.entries.firstOrNull { it.name == prefs[styleKey] } ?: DesignStyle.Miuix
        )
    }

    suspend fun save(settings: Settings) {
        context.dataStore.edit { prefs ->
            prefs[hostKey] = settings.host
            prefs[portKey] = settings.port
            prefs[styleKey] = settings.style.name
        }
    }
}

private data class DownloadJob(
    val id: String,
    val status: String,
    val title: String?,
    val fileName: String?,
    val error: String?
)

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
            if (!response.isSuccessful) throw IOException(response.body?.string().orEmpty().ifBlank { "HTTP ${response.code}" })
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
    ShiftSaverTheme(settings.style) {
        ShiftSaverScreen(settings, store, api)
    }
}

@Composable
private fun ShiftSaverTheme(style: DesignStyle, content: @Composable () -> Unit) {
    val miuix = lightColorScheme(
        primary = Color(0xFF0A84FF),
        secondary = Color(0xFF19C37D),
        surface = Color(0xFFF6F7FB),
        surfaceContainer = Color.White,
        background = Color(0xFFF6F7FB)
    )
    val md3 = lightColorScheme(
        primary = Color(0xFF006D3E),
        secondary = Color(0xFF56605A),
        tertiary = Color(0xFF38656D),
        surface = Color(0xFFFBFDF8),
        surfaceContainer = Color(0xFFEFF4ED),
        background = Color(0xFFFBFDF8)
    )
    MaterialTheme(colorScheme = if (style == DesignStyle.Miuix) miuix else md3, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShiftSaverScreen(settings: Settings, store: SettingsStore, api: ShiftSaverApi) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var draft by remember(settings) { mutableStateOf(settings) }
    var url by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Server not checked") }
    var busy by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<DownloadJob?>(null) }
    val baseUrl = remember(draft.host, draft.port) { "http://${draft.host.trim()}:${draft.port.trim()}" }

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
        topBar = {
            TopAppBar(
                title = {
                    Text("ShiftSaver", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HeroPanel(draft.style)
            DesignPicker(draft.style) { style ->
                draft = draft.copy(style = style)
                scope.launch { store.save(draft) }
            }
            ServerPanel(
                settings = draft,
                onHost = { draft = draft.copy(host = it) },
                onPort = { draft = draft.copy(port = it) },
                onSave = { scope.launch { store.save(draft) } },
                onTest = {
                    busy = true
                    scope.launch {
                        runCatching { api.health(baseUrl) }
                            .onSuccess { message = "Connected to ShiftSaver server $it" }
                            .onFailure { message = it.message ?: "Connection failed" }
                        busy = false
                    }
                },
                busy = busy,
                message = message
            )
            DownloadPanel(
                url = url,
                onUrl = { url = it },
                onPaste = { pasteUrl(clipboard)?.let { url = it } },
                onDownload = {
                    if (url.isBlank()) {
                        message = "Paste a public video or photo URL first"
                        return@DownloadPanel
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
                busy = busy
            )
            job?.let {
                JobPanel(baseUrl, it) { fileUrl ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl)))
                }
            }
            Text(
                "Use this only for your own media or media you have permission to save. Private, DRM-protected, login-only, or paywalled content is not supported.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HeroPanel(style: DesignStyle) {
    val shape = if (style == DesignStyle.Miuix) RoundedCornerShape(28.dp) else RoundedCornerShape(12.dp)
    val gradient = if (style == DesignStyle.Miuix) {
        Brush.linearGradient(listOf(Color(0xFF0A84FF), Color(0xFF19C37D)))
    } else {
        Brush.linearGradient(listOf(Color(0xFF006D3E), Color(0xFF38656D)))
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(gradient)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.CloudDownload, null, tint = Color.White, modifier = Modifier.size(34.dp))
            Text("Save public media through your own Linux box", color = Color.White, style = MaterialTheme.typography.headlineSmall)
            Text("Connect to the server, paste a link, and collect the finished file.", color = Color.White.copy(alpha = 0.88f))
        }
    }
}

@Composable
private fun DesignPicker(style: DesignStyle, onStyle: (DesignStyle) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        DesignStyle.entries.forEach {
            FilterChip(
                selected = style == it,
                onClick = { onStyle(it) },
                label = { Text(it.label) },
                leadingIcon = if (style == it) {
                    { Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(18.dp)) }
                } else null
            )
        }
    }
}

@Composable
private fun ServerPanel(
    settings: Settings,
    onHost: (String) -> Unit,
    onPort: (String) -> Unit,
    onSave: () -> Unit,
    onTest: () -> Unit,
    busy: Boolean,
    message: String
) {
    ShiftCard {
        Text("Server", style = MaterialTheme.typography.titleLarge)
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
            Button(onClick = onSave, enabled = !busy) { Text("Save") }
            Button(onClick = onTest, enabled = !busy) {
                if (busy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Test")
            }
        }
        Spacer(Modifier.height(10.dp))
        AssistChip(onClick = {}, label = { Text(message) })
    }
}

@Composable
private fun DownloadPanel(
    url: String,
    onUrl: (String) -> Unit,
    onPaste: () -> Unit,
    onDownload: () -> Unit,
    busy: Boolean
) {
    ShiftCard {
        Text("Download", style = MaterialTheme.typography.titleLarge)
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
            Button(onClick = onPaste, enabled = !busy) {
                Icon(Icons.Rounded.ContentPaste, null)
                Spacer(Modifier.width(8.dp))
                Text("Paste")
            }
            Button(onClick = onDownload, enabled = !busy) {
                Icon(Icons.Rounded.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("Start")
            }
        }
    }
}

@Composable
private fun JobPanel(baseUrl: String, job: DownloadJob, open: (String) -> Unit) {
    val done = job.status == "done" && job.fileName != null
    ShiftCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(if (job.error == null) Icons.Rounded.CloudDownload else Icons.Rounded.Error, null)
            Column(Modifier.weight(1f)) {
                Text(job.title ?: "Media job", style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(job.status.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            }
        }
        job.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        if (done) {
            Spacer(Modifier.height(12.dp))
            Button(onClick = { open("$baseUrl/files/${Uri.encode(job.fileName)}") }) {
                Text("Open file")
            }
        }
    }
}

@Composable
private fun ShiftCard(content: @Composable Column.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

private fun pasteUrl(clipboard: ClipboardManager): String? {
    return clipboard.getText()?.text?.trim()?.takeIf {
        it.startsWith("http://") || it.startsWith("https://")
    }
}
