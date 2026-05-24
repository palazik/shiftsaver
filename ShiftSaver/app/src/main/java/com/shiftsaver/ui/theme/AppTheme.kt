package com.shiftsaver.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.ColorSchemeMode
import top.yukonga.miuix.kmp.utils.ThemeController
import androidx.compose.runtime.remember

/**
 * ShiftSaver unified theme wrapper.
 *
 * [themeChoice] = "md3"   → Material Design 3
 * [themeChoice] = "miuix" → Xiaomi HyperOS / Miuix
 * [darkMode]    = "system" | "light" | "dark"
 */
@Composable
fun ShiftSaverTheme(
    themeChoice: String = "md3",
    darkMode: String = "system",
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (darkMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }

    when (themeChoice) {
        "miuix" -> {
            val mode = when (darkMode) {
                "dark" -> ColorSchemeMode.Dark
                "light" -> ColorSchemeMode.Light
                else -> ColorSchemeMode.System
            }
            val controller = remember(mode) { ThemeController(mode) }
            MiuixTheme(controller = controller, content = content)
        }
        else -> {
            val context = LocalContext.current
            val colorScheme = try {
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } catch (_: Exception) {
                if (isDark) darkColorScheme() else lightColorScheme()
            }
            MaterialTheme(colorScheme = colorScheme, content = content)
        }
    }
}
