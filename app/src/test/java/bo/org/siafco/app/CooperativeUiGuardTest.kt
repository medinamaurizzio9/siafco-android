package bo.org.siafco.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CooperativeUiGuardTest {
    @Test
    fun designSystemComponentsAreCentralized() {
        val components = projectFile("src/main/java/bo/org/siafco/app/core/ui/CooperativeComponents.kt").readText(Charsets.UTF_8)
        val theme = projectFile("src/main/java/bo/org/siafco/app/core/ui/SiafcoTheme.kt").readText(Charsets.UTF_8)

        listOf(
            "CooperativeTopBar",
            "CooperativeBottomBar",
            "PrimaryButton",
            "SecondaryButton",
            "InstitutionalCard",
            "SectionHeader",
            "StatusBadge",
            "LoadingSkeleton",
            "EmptyState",
            "MoneyText",
            "SecureTextField",
            "OrderSummaryCard",
            "ProductCard"
        ).forEach { assertTrue("$it should exist", components.contains(it)) }
        listOf("0xFF0A2342", "0xFF123A63", "0xFFD9AE2B", "0xFFF3D675", "0xFFF5F7FA").forEach {
            assertTrue("Theme should contain $it", theme.contains(it))
        }
    }

    @Test
    fun authenticatedBottomNavigationUsesFiveDestinationsAndTopLevelNavigation() {
        val components = projectFile("src/main/java/bo/org/siafco/app/core/ui/CooperativeComponents.kt").readText(Charsets.UTF_8)
        val navigation = projectFile("src/main/java/bo/org/siafco/app/navigation/SiafcoAppRoot.kt").readText(Charsets.UTF_8)

        listOf("Home", "Store", "Orders", "Credential", "Profile").forEach {
            assertTrue("Bottom destination $it should exist", components.contains("$it("))
        }
        assertTrue(navigation.contains("navigateTopLevel"))
        assertTrue(navigation.contains("launchSingleTop = true"))
        assertTrue(navigation.contains("restoreState = true"))
        assertFalse(navigation.contains("WebView"))
    }

    @Test
    fun storeScreensKeepTechnicalCodesOutOfVisibleTextAndConfirmCartClear() {
        val source = projectFile("src/main/java/bo/org/siafco/app/feature/store/StoreScreens.kt").readText(Charsets.UTF_8)

        assertFalse(source.contains("Text(line.productPublicCode"))
        assertFalse(source.contains("Text(productPublicCode"))
        assertTrue(source.contains("quoteItem?.productName"))
        assertTrue(source.contains("AlertDialog("))
        assertTrue(source.contains("Paso 1"))
        assertTrue(source.contains("Paso 4"))
    }

    @Test
    fun credentialKeepsLandscapeFullscreenPolicy() {
        val source = projectFile("src/main/java/bo/org/siafco/app/feature/credential/CredentialScreen.kt").readText(Charsets.UTF_8)

        assertTrue(source.contains("SCREEN_ORIENTATION_SENSOR_LANDSCAPE"))
        assertTrue(source.contains("requestedOrientation = previousOrientation"))
        assertTrue(source.contains("WindowInsetsCompat.Type.systemBars()"))
        assertFalse(source.contains("WebView"))
    }

    private fun projectFile(path: String): File =
        sequenceOf(File(path), File("app/$path")).first { it.exists() }
}
