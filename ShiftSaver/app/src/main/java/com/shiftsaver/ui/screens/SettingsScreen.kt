package com.shiftsaver.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shiftsaver.viewmodel.MainUiState
import com.shiftsaver.viewmodel.MainViewModel
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.SmallTitle as MiuixSmallTitle
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults
import top.yukonga.miuix.kmp.basic.TextField as MiuixTextField
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsScreen(
    state: MainUiState,
    viewModel: MainViewModel,
    isMiuix: Boolean
) {
    if (isMiuix) {
        SettingsMiuix(state, viewModel)
    } else {
        SettingsMD3(state, viewModel)
    }
}

@Composable
private fun SettingsMD3(state: MainUiState, viewModel: MainViewModel) {
    var hostField by remember(state.host) { mutableStateOf(state.host) }
    var portField by remember(state.port) { mutableStateOf(state.port.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Server", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = hostField,
                    onValueChange = { hostField = it },
                    label = { Text("IP Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = portField,
                    onValueChange = { portField = it },
                    label = { Text("Port") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.saveServer(hostField, portField.toIntOrNull() ?: 5050) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Save") }
                    Button(
                        onClick = { viewModel.testConnection() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Test") }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Design", style = MaterialTheme.typography.titleMedium)
                listOf("md3" to "Material 3 (default)", "miuix" to "Miuix (HyperOS)").forEach { (key, label) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(label)
                        RadioButton(selected = state.theme == key, onClick = { viewModel.setTheme(key) })
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Dark Mode", style = MaterialTheme.typography.titleMedium)
                listOf("system" to "System Default", "light" to "Light", "dark" to "Dark").forEach { (key, label) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(label)
                        RadioButton(selected = state.darkMode == key, onClick = { viewModel.setDarkMode(key) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsMiuix(state: MainUiState, viewModel: MainViewModel) {
    var hostField by remember(state.host) { mutableStateOf(state.host) }
    var portField by remember(state.port) { mutableStateOf(state.port.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // ── Server ──────────────────────────────────────────────────────────
        MiuixSmallTitle("Server")
        MiuixCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MiuixTextField(
                    value = hostField,
                    onValueChange = { hostField = it },
                    label = "IP Address",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                MiuixTextField(
                    value = portField,
                    onValueChange = { portField = it },
                    label = "Port",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiuixTextButton(
                        text = "Save",
                        onClick = { viewModel.saveServer(hostField, portField.toIntOrNull() ?: 5050) },
                        modifier = Modifier.weight(1f)
                    )
                    MiuixTextButton(
                        text = "Test Connection",
                        onClick = { viewModel.testConnection() },
                        modifier = Modifier.weight(1f),
                        colors = MiuixButtonDefaults.textButtonColorsPrimary()
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Design ──────────────────────────────────────────────────────────
        MiuixSmallTitle("Design")
        MiuixCard(modifier = Modifier.fillMaxWidth()) {
            RadioButtonPreference(
                title = "Material 3",
                summary = "Google Material You design",
                selected = state.theme == "md3",
                onClick = { viewModel.setTheme("md3") }
            )
            RadioButtonPreference(
                title = "Miuix",
                summary = "Xiaomi HyperOS design",
                selected = state.theme == "miuix",
                onClick = { viewModel.setTheme("miuix") }
            )
        }

        Spacer(Modifier.height(8.dp))

        // ── Dark Mode ───────────────────────────────────────────────────────
        MiuixSmallTitle("Dark Mode")
        MiuixCard(modifier = Modifier.fillMaxWidth()) {
            listOf(
                "system" to "System Default",
                "light"  to "Light",
                "dark"   to "Dark"
            ).forEach { (key, label) ->
                RadioButtonPreference(
                    title = label,
                    selected = state.darkMode == key,
                    onClick = { viewModel.setDarkMode(key) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
