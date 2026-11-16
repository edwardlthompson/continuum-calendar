package org.continuumcalendar.ui.theme

import androidx.compose.ui.graphics.Color

/** Continuum `--cc-*` tokens from apps/desktop/src/styles/brand.css. */
object ContinuumBrand {
    val Teal = Color(0xFF0F6E8C)
    val TealBright = Color(0xFF4EB6D4)
    val TealDeep = Color(0xFF0A4F66)
    val Mist = Color(0xFFD7EEF5)
    val Ink = Color(0xFF0F172A)
    val Slate = Color(0xFF5B6B82)
    val Open = Color(0xFF0F6E8C)
    val Now = Color(0xFFD32F2F)
    val Warn = Color(0xFFFACC15)
    val OnAccent = Color(0xFFFFFFFF)
    val OnWarn = Color(0xFF1C1917)

    val LightBg = Color(0xFFF4F7FB)
    val LightSurface = Color(0xFFFFFFFF)
    val LightBorder = Color(0xFFD5DEE8)
    val LightGrid = Color(0xFFE8EEF5)

    val DarkBg = Color(0xFF0B1220)
    val DarkSurface = Color(0xFF121A2B)
    val DarkText = Color(0xFFE8EEF8)
    val DarkMuted = Color(0xFF9AABBF)
    val DarkBorder = Color(0xFF243044)
    val DarkGrid = Color(0xFF1A2436)
    val DarkAccentSoft = Color(0xFF163447)
}

data class ContinuumPalette(
    val background: Color,
    val surface: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val border: Color,
    val grid: Color,
    val today: Color,
    val open: Color,
    val now: Color,
    val warn: Color,
)

fun continuumPalette(dark: Boolean): ContinuumPalette =
    if (dark) {
        ContinuumPalette(
            background = ContinuumBrand.DarkBg,
            surface = ContinuumBrand.DarkSurface,
            text = ContinuumBrand.DarkText,
            muted = ContinuumBrand.DarkMuted,
            accent = ContinuumBrand.TealBright,
            onAccent = ContinuumBrand.DarkBg,
            accentSoft = ContinuumBrand.DarkAccentSoft,
            border = ContinuumBrand.DarkBorder,
            grid = ContinuumBrand.DarkGrid,
            today = ContinuumBrand.TealBright,
            open = ContinuumBrand.Open,
            now = ContinuumBrand.Now,
            warn = ContinuumBrand.Warn,
        )
    } else {
        ContinuumPalette(
            background = ContinuumBrand.LightBg,
            surface = ContinuumBrand.LightSurface,
            text = ContinuumBrand.Ink,
            muted = ContinuumBrand.Slate,
            accent = ContinuumBrand.Teal,
            onAccent = ContinuumBrand.OnAccent,
            accentSoft = ContinuumBrand.Mist,
            border = ContinuumBrand.LightBorder,
            grid = ContinuumBrand.LightGrid,
            today = ContinuumBrand.Teal,
            open = ContinuumBrand.Open,
            now = ContinuumBrand.Now,
            warn = ContinuumBrand.Warn,
        )
    }
