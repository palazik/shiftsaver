package com.shiftsaver.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import top.yukonga.miuix.kmp.miuix.MiuixTheme
import top.yukonga.miuix.kmp.miuix.darkColorScheme as miuixDarkColorScheme
import top.yukonga.miuix.kmp.miuix.lightColorScheme as miuixLightColorScheme

@Composable
fun ShiftSaverTheme(
    themeChoice: String = "miuix",
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
            val colors = if (isDark) miuixDarkColorScheme() else miuixLightColorScheme()
            MiuixTheme(colors = colors, content = content)
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
