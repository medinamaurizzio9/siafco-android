package bo.org.siafco.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ResourceIntegrityTest {
    @Test
    fun visibleStringResourcesDoNotContainMojibake() {
        val text = projectFile("src/main/res/values/strings.xml").readText(Charsets.UTF_8)

        listOf(
            "\u00C3\u0192",
            "\u00C3\u201A",
            "\u00EF\u00BF\u00BD",
            "\uFFFD"
        ).forEach { marker ->
            assertFalse("strings.xml contains mojibake marker $marker", text.contains(marker))
        }
        assertTrue(text.contains("Tierra Bendita"))
        assertTrue(text.contains("Sistema Integral de Afiliaciones"))
        assertTrue(text.contains("Cotización"))
        assertTrue(text.contains("Cupón"))
        assertTrue(text.contains("Envío"))
        assertTrue(text.contains("Pedido N.º %1\$s"))
    }

    @Test
    fun brandingSplashAndLauncherResourcesExist() {
        listOf(
            "src/main/res/drawable-nodpi/brand_logo.png",
            "src/main/res/drawable-nodpi/splash_logo.png",
            "src/main/res/values-v31/styles.xml",
            "src/main/res/mipmap-anydpi-v26/ic_launcher.xml",
            "src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml",
            "src/main/res/mipmap-mdpi/ic_launcher.png",
            "src/main/res/mipmap-hdpi/ic_launcher.png",
            "src/main/res/mipmap-xhdpi/ic_launcher.png",
            "src/main/res/mipmap-xxhdpi/ic_launcher.png",
            "src/main/res/mipmap-xxxhdpi/ic_launcher.png"
        ).forEach { path ->
            assertTrue("$path should exist", projectFile(path).isFile)
        }
    }

    private fun projectFile(path: String): File =
        sequenceOf(File(path), File("app/$path")).first { it.exists() }
}
