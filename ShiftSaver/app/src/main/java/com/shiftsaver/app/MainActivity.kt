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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.ui.text.font.FontWeight
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
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme
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
    val colors = if (themeMode == ThemeMode.Dark) {
        darkColorScheme(
            primary = Color(0xFF277AF7),
            primaryVariant = Color(0xFF277AF7),
            surface = Color(0xFF000000),
            surfaceVariant = Color(0xFF191919),
            surfaceContainer = Color(0xFF242424),
            surfaceContainerHigh = Color(0xFF2D2D2D),
            surfaceContainerHighest = Color(0xFF323232),
            background = Color(0xFF191919)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF3482FF),
            primaryVariant = Color(0xFF3482FF),
            surface = Color(0xFFF7F7F7),
            surfaceVariant = Color(0xFFF2F2F2),
            surfaceContainer = Color.White,
            background = Color.White
        )
    }
    MiuixTheme(colors = colors, content = content)
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
        topBar = {
            SmallTopAppBar(
                title = currentTab.label,
                color = MiuixTheme.colorScheme.surface,
                actions = {
                    StatusDot(busy = busy)
                }
            )
        },
        bottomBar = {
            NavigationBar(mode = NavigationBarDisplayMode.IconAndText) {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = tab.icon,
                        label = tab.label
                    )
                }
            }
        },
        containerColor = MiuixTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = baseUrl,
                modifier = Modifier.padding(horizontal = 6.dp),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
    DownloadSummaryCard()
    MiuixPanel {
        Text(
            text = "Media link",
            style = MiuixTheme.textStyles.title3,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(14.dp))
        TextField(
            value = url,
            onValueChange = onUrl,
            label = "TikTok, YouTube, Instagram URL",
            leadingIcon = {
                Icon(imageVector = Icons.Rounded.Link, contentDescription = null)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiuixButton(onClick = onPaste, enabled = !busy) {
                Icon(imageVector = Icons.Rounded.ContentPaste, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Paste")
            }
            MiuixButton(onClick = onDownload, enabled = !busy, primary = true) {
                if (busy) {
                    CircularProgressIndicator(size = 18.dp, strokeWidth = 2.dp)
                } else {
                    Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null)
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
    MiuixPanel {
        Text(
            text = "Active server",
            style = MiuixTheme.textStyles.title3,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(14.dp))
        TextField(
            value = settings.host,
            onValueChange = onHost,
            label = "IP address",
            leadingIcon = {
                Icon(imageVector = Icons.Rounded.WifiTethering, contentDescription = null)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        TextField(
            value = settings.port,
            onValueChange = onPort,
            label = "Port",
            leadingIcon = {
                Icon(imageVector = Icons.Rounded.SettingsEthernet, contentDescription = null)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiuixButton(onClick = onSave, enabled = !busy) { Text("Save") }
            MiuixButton(onClick = onTest, enabled = !busy, primary = true) {
                if (busy) CircularProgressIndicator(size = 18.dp, strokeWidth = 2.dp) else Text("Test")
            }
        }
    }
    Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(vertical = 4.dp)) {
        BasicComponent(
            title = "Connection",
            summary = message,
            startAction = {
                Icon(
                    imageVector = if (message.startsWith("Connected")) Icons.Rounded.CheckCircle else Icons.Rounded.Info,
                    contentDescription = null,
                    tint = if (message.startsWith("Connected")) {
                        Color(0xFF15C85D)
                    } else {
                        MiuixTheme.colorScheme.primary
                    }
                )
            }
        )
    }
    Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(vertical = 4.dp)) {
        ServerPreset("Home LAN", "192.168.1.10", "8787", onPreset)
        ServerPreset("Laptop hotspot", "192.168.43.1", "8787", onPreset)
        ServerPreset("Local test", "10.0.2.2", "8787", onPreset)
    }
    Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(vertical = 4.dp)) {
        BasicComponent(
            title = "Linux command",
            summary = "./linux/install.sh\nThe script prints the IP and port to use here.",
            startAction = {
                Icon(imageVector = Icons.Rounded.SettingsEthernet, contentDescription = null)
            }
        )
    }
}

@Composable
private fun ServerPreset(label: String, host: String, port: String, onPreset: (String, String) -> Unit) {
    BasicComponent(
        title = label,
        summary = "$host:$port",
        startAction = {
            Icon(
                imageVector = Icons.Rounded.WifiTethering,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.primary
            )
        },
        endActions = {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions
            )
        },
        onClick = { onPreset(host, port) }
    )
}

@Composable
private fun SettingsTab(settings: Settings, onTheme: (ThemeMode) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(vertical = 4.dp)) {
        ThemeMode.entries.forEach { mode ->
            SelectableRow(
                title = mode.label,
                summary = if (mode == ThemeMode.White) "HyperOS white surfaces" else "HyperOS dark surfaces",
                icon = Icons.Rounded.Settings,
                selected = settings.themeMode == mode,
                onClick = { onTheme(mode) }
            )
        }
    }
    Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(vertical = 4.dp)) {
        BasicComponent(
            title = "Interface",
            summary = "Miuix UI components, Miuix colors, Miuix navigation, and grouped HyperOS-style surfaces.",
            startAction = {
                Icon(imageVector = Icons.Rounded.Info, contentDescription = null)
            }
        )
    }
}

@Composable
private fun AboutTab(baseUrl: String) {
    MiuixPanel {
        AccentIcon(Icons.Rounded.CloudDownload)
        Spacer(Modifier.height(14.dp))
        Text("ShiftSaver", style = MiuixTheme.textStyles.title1, fontWeight = FontWeight.SemiBold)
        Text(
            "Version 0.1.0",
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Android companion app for a self-hosted Linux media downloader on your local network.",
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
    }
    Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(vertical = 4.dp)) {
        BasicComponent(
            title = "Current server",
            summary = baseUrl,
            startAction = {
                Icon(imageVector = Icons.Rounded.SettingsEthernet, contentDescription = null)
            }
        )
    }
    NoticeCard()
}

@Composable
private fun DownloadSummaryCard() {
    MiuixPanel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            AccentIcon(Icons.Rounded.CloudDownload)
            Column(Modifier.weight(1f)) {
                Text(
                    "Quick Save",
                    style = MiuixTheme.textStyles.title2,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Paste a public link and let your Linux server handle the download.",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        }
    }
}

@Composable
private fun JobPanel(baseUrl: String, job: DownloadJob, open: (String) -> Unit) {
    val done = job.status == "done" && job.fileName != null
    MiuixPanel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                imageVector = if (job.error == null) Icons.Rounded.CloudDownload else Icons.Rounded.Error,
                contentDescription = null,
                tint = if (job.error == null) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.error
            )
            Column(Modifier.weight(1f)) {
                Text(
                    job.title ?: "Media job",
                    style = MiuixTheme.textStyles.headline1,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    job.status.uppercase(),
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.primary
                )
            }
        }
        job.error?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = MiuixTheme.colorScheme.error)
        }
        if (done) {
            Spacer(Modifier.height(14.dp))
            MiuixButton(onClick = { open("$baseUrl/files/${Uri.encode(job.fileName)}") }, primary = true) {
                Text("Open file")
            }
        }
    }
}

@Composable
private fun NoticeCard() {
    Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(vertical = 4.dp)) {
        BasicComponent(
            title = "Responsible use",
            summary = "Use ShiftSaver only for your own media or media you have permission to save. Private, DRM-protected, login-only, or paywalled content is not supported.",
            startAction = {
                Icon(imageVector = Icons.Rounded.Info, contentDescription = null)
            }
        )
    }
}

@Composable
private fun SelectableRow(
    title: String,
    summary: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    BasicComponent(
        title = title,
        summary = summary,
        startAction = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantActions
            )
        },
        endActions = {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.primary
                )
            }
        },
        onClick = onClick
    )
}

@Composable
private fun StatusDot(busy: Boolean) {
    val brush = if (busy) {
        Brush.linearGradient(listOf(Color(0xFFFFB340), Color(0xFFFF7A1A)))
    } else {
        Brush.linearGradient(listOf(Color(0xFF3482FF), Color(0xFF5DAAFF)))
    }
    Box(
        modifier = Modifier
            .padding(end = 12.dp)
            .size(18.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(brush)
    )
}

@Composable
private fun AccentIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(MiuixTheme.colorScheme.tertiaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onTertiaryContainer
        )
    }
}

@Composable
private fun MiuixPanel(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        insideMargin = PaddingValues(18.dp),
        content = content
    )
}

@Composable
private fun MiuixButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    primary: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        cornerRadius = 18.dp,
        colors = if (primary) ButtonDefaults.buttonColorsPrimary() else ButtonDefaults.buttonColors(),
        content = content
    )
}

private fun pasteUrl(clipboard: ClipboardManager): String? {
    return clipboard.getText()?.text?.trim()?.takeIf {
        it.startsWith("http://") || it.startsWith("https://")
    }
}
