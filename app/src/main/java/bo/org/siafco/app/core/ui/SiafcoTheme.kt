package bo.org.siafco.app.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val CooperativeNavy = Color(0xFF0A2342)
val CooperativeNavySecondary = Color(0xFF123A63)
val CooperativeGold = Color(0xFFD9AE2B)
val CooperativeGoldSoft = Color(0xFFF3D675)
val CooperativeBackground = Color(0xFFF5F7FA)
val CooperativeSurface = Color(0xFFFFFFFF)
val CooperativeTextPrimary = Color(0xFF101828)
val CooperativeTextSecondary = Color(0xFF667085)
val CooperativeSuccess = Color(0xFF15803D)
val CooperativeWarning = Color(0xFFB7791F)
val CooperativeError = Color(0xFFB42318)
val FigmaNavy = Color(0xFF071F3F)
val FigmaNavyDeep = Color(0xFF041A36)
val FigmaGold = Color(0xFFDDBA35)
val FigmaInputBackground = Color(0xFFF7F9FC)
val FigmaMuted = Color(0xFF8C98A8)

private val LightColors = lightColorScheme(
    primary = CooperativeNavy,
    onPrimary = Color.White,
    secondary = CooperativeGold,
    onSecondary = CooperativeNavy,
    tertiary = CooperativeNavySecondary,
    background = CooperativeBackground,
    onBackground = CooperativeTextPrimary,
    surface = CooperativeSurface,
    onSurface = CooperativeTextPrimary,
    surfaceVariant = Color(0xFFE8EDF3),
    onSurfaceVariant = CooperativeTextSecondary,
    error = CooperativeError
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9EC5FF),
    onPrimary = CooperativeNavy,
    secondary = CooperativeGoldSoft,
    onSecondary = CooperativeNavy,
    tertiary = Color(0xFF8FC8FF),
    background = Color(0xFF07111F),
    onBackground = Color(0xFFE8EDF3),
    surface = Color(0xFF101B2C),
    onSurface = Color(0xFFE8EDF3),
    surfaceVariant = Color(0xFF1A2A42),
    onSurfaceVariant = Color(0xFFB9C2D0),
    error = Color(0xFFFFB4AB)
)

@Composable
fun SiafcoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
