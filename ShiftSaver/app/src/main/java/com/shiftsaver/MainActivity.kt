package com.shiftsaver

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.shiftsaver.ui.Screen
import com.shiftsaver.ui.screens.HistoryScreen
import com.shiftsaver.ui.screens.HomeScreen
import com.shiftsaver.ui.screens.SettingsScreen
import com.shiftsaver.ui.theme.ShiftSaverTheme
import com.shiftsaver.viewmodel.MainViewModel
import top.yukonga.miuix.kmp.basic.NavigationBar as MiuixNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationItem as MiuixNavigationItem
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.extra.OverlayDialog

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val sharedUrl = intent.getStringExtra(Intent.EXTRA_TEXT)
        setContent {
            val vm: MainViewModel = viewModel()
            val state by vm.state.collectAsState()

            // Handle shared URL from other apps
            LaunchedEffect(sharedUrl) {
                if (!sharedUrl.isNullOrBlank()) {
                    vm.onUrlChange(sharedUrl)
                }
            }

            ShiftSaverTheme(themeChoice = state.theme, darkMode = state.darkMode) {
                if (state.theme == "miuix") {
                    ShiftSaverMiuixRoot(state = state, vm = vm)
                } else {
                    ShiftSaverMD3Root(state = state, vm = vm)
                }
            }
        }
    }
}

// ─── MD3 root ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShiftSaverMD3Root(
    state: com.shiftsaver.viewmodel.MainUiState,
    vm: MainViewModel
) {
    val navController = rememberNavController()
    val backEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backEntry?.destination?.route ?: Screen.Home.route
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHost.showSnackbar(it)
            vm.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(title = { Text("ShiftSaver") })
        },
        bottomBar = {
            NavigationBar {
                navItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) navController.navigate(item.route) {
                                popUpTo(Screen.Home.route)
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = Screen.Home.route, Modifier.padding(padding)) {
            composable(Screen.Home.route) { HomeScreen(state, vm, isMiuix = false) }
            composable(Screen.History.route) { HistoryScreen(state, vm, isMiuix = false) }
            composable(Screen.Settings.route) { SettingsScreen(state, vm, isMiuix = false) }
        }
    }
}

// ─── Miuix root ──────────────────────────────────────────────────────────────

@Composable
private fun ShiftSaverMiuixRoot(
    state: com.shiftsaver.viewmodel.MainUiState,
    vm: MainViewModel
) {
    val navController = rememberNavController()
    val backEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backEntry?.destination?.route ?: Screen.Home.route

    var showSnackbar by remember { mutableStateOf(false) }
    var snackMsg by remember { mutableStateOf("") }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackMsg = it
            showSnackbar = true
            vm.clearSnackbar()
        }
    }

    MiuixScaffold(
        topBar = {
            MiuixTopAppBar(title = "ShiftSaver")
        },
        bottomBar = {
            MiuixNavigationBar {
                navItems.forEachIndexed { _, item ->
                    MiuixNavigationItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) navController.navigate(item.route) {
                                popUpTo(Screen.Home.route)
                                launchSingleTop = true
                            }
                        },
                        icon = { MiuixIcon(item.icon, contentDescription = item.label) },
                        label = item.label
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = Screen.Home.route, Modifier.padding(padding)) {
            composable(Screen.Home.route) { HomeScreen(state, vm, isMiuix = true) }
            composable(Screen.History.route) { HistoryScreen(state, vm, isMiuix = true) }
            composable(Screen.Settings.route) { SettingsScreen(state, vm, isMiuix = true) }
        }

        if (showSnackbar) {
            OverlayDialog(
                title = "ShiftSaver",
                summary = snackMsg,
                confirmButton = "OK",
                onConfirmButtonClick = { showSnackbar = false },
                onDismissRequest = { showSnackbar = false }
            )
        }
    }
}

// ─── shared nav item data ─────────────────────────────────────────────────────

private data class NavItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val navItems = listOf(
    NavItem(Screen.Home.route, "Download", Icons.Default.Download),
    NavItem(Screen.History.route, "History", Icons.Default.History),
    NavItem(Screen.Settings.route, "Settings", Icons.Default.Settings)
)
