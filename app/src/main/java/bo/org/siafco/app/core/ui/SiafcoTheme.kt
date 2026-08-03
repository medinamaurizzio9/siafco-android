package bo.org.siafco.app.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF071B3A),
    onPrimary = Color.White,
    secondary = Color(0xFFD9A323),
    tertiary = Color(0xFF0B5A9E),
    background = Color(0xFFF7F9FC),
    surface = Color(0xFFFFFFFF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FB7FF),
    onPrimary = Color(0xFF071B3A),
    secondary = Color(0xFFE7C35F),
    tertiary = Color(0xFF8FC8FF),
    background = Color(0xFF07111F),
    surface = Color(0xFF101B2C)
)

@Composable
fun SiafcoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
