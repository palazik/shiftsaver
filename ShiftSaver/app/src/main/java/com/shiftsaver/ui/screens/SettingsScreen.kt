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
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.TextField as MiuixTextField
import top.yukonga.miuix.kmp.extra.SwitchPreference
import top.yukonga.miuix.kmp.extra.RadioButtonPreference
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
        // Server
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

        // Theme
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

        // Dark mode
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Server card
        MiuixCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MiuixText("Server", style = MiuixTheme.textStyles.title)
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
                    MiuixButton("Save", onClick = {
                        viewModel.saveServer(hostField, portField.toIntOrNull() ?: 5050)
                    }, modifier = Modifier.weight(1f))
                    MiuixButton("Test Connection", onClick = { viewModel.testConnection() }, modifier = Modifier.weight(1f))
                }
            }
        }

        // Design preference
        MiuixCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MiuixText("Design", style = MiuixTheme.textStyles.title)
                RadioButtonPreference(
                    title = "Material 3",
                    summary = "Google Material You design",
                    selected = state.theme == "md3",
                    onSelectedChange = { if (it) viewModel.setTheme("md3") }
                )
                RadioButtonPreference(
                    title = "Miuix",
                    summary = "Xiaomi HyperOS design",
                    selected = state.theme == "miuix",
                    onSelectedChange = { if (it) viewModel.setTheme("miuix") }
                )
            }
        }

        // Dark mode
        MiuixCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MiuixText("Dark Mode", style = MiuixTheme.textStyles.title)
                listOf("system" to "System Default", "light" to "Light", "dark" to "Dark").forEach { (key, label) ->
                    RadioButtonPreference(
                        title = label,
                        selected = state.darkMode == key,
                        onSelectedChange = { if (it) viewModel.setDarkMode(key) }
                    )
                }
            }
        }
    }
}
