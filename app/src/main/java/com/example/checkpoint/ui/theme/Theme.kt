package com.example.checkpoint.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ------------------------------------------------------------------------------------------------
// Configurazione Tavolozze Colori (Tema Scuro e Chiaro)
// ------------------------------------------------------------------------------------------------

private val DarkColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = TextPrimary,
    background = DeepDark,
    surface = SurfaceDark,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    secondary = TextSecondary,
    tertiaryContainer = DeepNavy,
    onTertiaryContainer = CyanAccent,
    error = ErrorRed,
    onError = Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary = CyanDark,
    onPrimary = Color.White,
    background = IceWhite,
    onBackground = DeepNavy,
    surface = LightBlueGray,
    onSurface = DeepNavy,
    secondary = TextSecondary,
    tertiaryContainer = Color(0xFFD1E8EA),
    onTertiaryContainer = CyanDark,
    surfaceVariant = Color.White,
    onSurfaceVariant = DeepNavy,
    error = ErrorRed,
    onError = Color.White,
)


// ------------------------------------------------------------------------------------------------
// Tema Radice dell'Applicazione
// ------------------------------------------------------------------------------------------------

/**
 * Composable radice che applica la configurazione del tema (colori, tipografia, forme e spaziature).
 */
@Composable
fun CheckPointTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalSpacing provides Spacing()) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content,
        )
    }
}
