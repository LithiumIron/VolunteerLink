package com.example.volunteerlink.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = VolunteerLinkPrimaryGreen,
    onPrimary = Color.White,
    secondary = VolunteerLinkSecondaryGreen,
    onSecondary = Color.White,
    tertiary = VolunteerLinkAccentGreen,
    onTertiary = Color.White,
    background = VolunteerLinkBackground,
    onBackground = VolunteerLinkTextPrimary,
    surface = VolunteerLinkSurface,
    onSurface = VolunteerLinkTextPrimary,
    surfaceVariant = VolunteerLinkSoftGreenSurface,
    onSurfaceVariant = VolunteerLinkTextSecondary,
    outline = VolunteerLinkBorderColour,
    error = VolunteerLinkError,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = VolunteerLinkAccentGreen,
    onPrimary = VolunteerLinkTextPrimary,
    secondary = VolunteerLinkSecondaryGreen,
    onSecondary = Color.White,
    tertiary = VolunteerLinkSoftGreenSurface,
    onTertiary = VolunteerLinkTextPrimary,
    background = Color(0xFF142010),
    onBackground = Color(0xFFE6ECE4),
    surface = Color(0xFF1B2A17),
    onSurface = Color(0xFFE6ECE4),
    surfaceVariant = Color(0xFF25341F),
    onSurfaceVariant = Color(0xFFC3D0BF),
    outline = Color(0xFF3D4E37),
    error = Color(0xFFEF9A9A),
    onError = Color(0xFF3A0A0A)
)

@Composable
fun VolunteerLinkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Off by default — Material You's dynamic color pulls its palette from
    // the phone's wallpaper on Android 12+, which is exactly what was
    // overriding the green scheme with purple on some devices. Flip this
    // on only if you deliberately want the app to adapt to each user's
    // wallpaper instead of using VolunteerLink's own brand colours.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}