package com.dmax.nexus.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class NexusPalette(
    val background: Color,
    val surface: Color,
    val elevated: Color,
    val accent: Color,
    val text: Color,
    val muted: Color,
    val border: Color
)

private val EmeraldPalette = NexusPalette(
    background = Color(0xFF080D0B), surface = Color(0xFF111A16),
    elevated = Color(0xFF17241D), accent = Color(0xFF36F1AE),
    text = Color(0xFFEAF4EF), muted = Color(0xFF91A39A), border = Color(0xFF293831)
)
private val PurplePalette = NexusPalette(
    background = Color(0xFF0B0810), surface = Color(0xFF17121F),
    elevated = Color(0xFF241A32), accent = Color(0xFFB99AFF),
    text = Color(0xFFF2ECFF), muted = Color(0xFFAAA0B8), border = Color(0xFF3A2D4A)
)
private val CrimsonPalette = NexusPalette(
    background = Color(0xFF10090B), surface = Color(0xFF201316),
    elevated = Color(0xFF301A20), accent = Color(0xFFFF7185),
    text = Color(0xFFFFEFF1), muted = Color(0xFFBBA0A5), border = Color(0xFF482A31)
)
private val MonoPalette = NexusPalette(
    background = Color(0xFF090909), surface = Color(0xFF171717),
    elevated = Color(0xFF242424), accent = Color(0xFFE5E7EB),
    text = Color(0xFFF5F5F5), muted = Color(0xFFA3A3A3), border = Color(0xFF363636)
)

val LocalNexusPalette = staticCompositionLocalOf { EmeraldPalette }

@Composable
fun NexusTheme(themeName: String, content: @Composable () -> Unit) {
    val palette = when (themeName.lowercase()) {
        "purple cyber" -> PurplePalette
        "crimson" -> CrimsonPalette
        "monochrome" -> MonoPalette
        else -> EmeraldPalette
    }
    CompositionLocalProvider(LocalNexusPalette provides palette) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = palette.accent,
                onPrimary = palette.background,
                secondary = palette.accent,
                background = palette.background,
                surface = palette.surface,
                onSurface = palette.text,
                onBackground = palette.text,
                outline = palette.border
            ),
            content = content
        )
    }
}
