package bo.org.siafco.app.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B3D3A),
    onPrimary = Color.White,
    secondary = Color(0xFF8B6F2A),
    tertiary = Color(0xFF4E6E5D),
    background = Color(0xFFF8FAF8),
    surface = Color(0xFFFFFFFF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF93D1C8),
    onPrimary = Color(0xFF003733),
    secondary = Color(0xFFD8C78F),
    tertiary = Color(0xFFB8D8C7),
    background = Color(0xFF101615),
    surface = Color(0xFF18211F)
)

@Composable
fun SiafcoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
