package bo.org.siafco.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import bo.org.siafco.app.navigation.SiafcoAppRoot
import bo.org.siafco.app.core.ui.SiafcoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as SiafcoApp
        setContent {
            SiafcoTheme {
                SiafcoAppRoot(app.container)
            }
        }
    }
}
