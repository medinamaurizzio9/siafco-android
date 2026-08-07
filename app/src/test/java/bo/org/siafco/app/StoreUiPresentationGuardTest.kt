package bo.org.siafco.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class StoreUiPresentationGuardTest {
    @Test
    fun cartUiUsesQuoteDataInsteadOfShowingProductPublicCode() {
        val source = projectFile("src/main/java/bo/org/siafco/app/feature/store/StoreScreens.kt").readText(Charsets.UTF_8)

        assertTrue(source.contains("quoteItem?.productName"))
        assertTrue(source.contains("quoteItem?.variantName"))
        assertTrue(source.contains("it.unitPrice"))
        assertTrue(source.contains("it.lineTotal"))
        assertFalse(source.contains("Text(line.productPublicCode"))
        assertFalse(source.contains("Text(productPublicCode"))
        assertFalse(source.contains("Text(item.productPublicCode"))
    }

    @Test
    fun checkoutAndOrdersUseQuoteAndOrderCodePresentation() {
        val screens = projectFile("src/main/java/bo/org/siafco/app/feature/store/StoreScreens.kt").readText(Charsets.UTF_8)
        val viewModels = projectFile("src/main/java/bo/org/siafco/app/feature/store/StoreViewModels.kt").readText(Charsets.UTF_8)

        assertTrue(screens.contains("QuoteSummary(it, compact = true, lines = state.lines)"))
        assertTrue(screens.contains("QuoteSummary(it, lines = state.lines)"))
        assertTrue(screens.contains("stringResource(R.string.store_order_number, order.code)"))
        assertTrue(screens.contains("stringResource(R.string.store_receipt_order, orderCode)"))
        assertTrue(viewModels.contains("val canCreateOrder: Boolean get() = lines.isNotEmpty() && quote != null"))
    }

    @Test
    fun storeCriticalSubmitsShowBlockingProcessingOverlay() {
        val screens = projectFile("src/main/java/bo/org/siafco/app/feature/store/StoreScreens.kt").readText(Charsets.UTF_8)
        val strings = projectFile("src/main/res/values/strings.xml").readText(Charsets.UTF_8)

        assertTrue(screens.contains("BackHandler(enabled = state.submitting)"))
        assertTrue(screens.contains("StoreProcessingOverlay("))
        assertTrue(screens.contains("store_processing_order_message"))
        assertTrue(screens.contains("store_processing_receipt_message"))
        assertTrue(strings.contains("Estamos creando tu pedido"))
        assertTrue(strings.contains("Estamos enviando tu comprobante"))
    }

    @Test
    fun splashAndLoginUseFigmaBrandingButHomeStaysCompact() {
        val splash = projectFile("src/main/java/bo/org/siafco/app/feature/splash/SplashScreen.kt").readText(Charsets.UTF_8)
        val login = projectFile("src/main/java/bo/org/siafco/app/feature/auth/LoginScreen.kt").readText(Charsets.UTF_8)

        assertTrue(splash.contains("R.drawable.splash_logo"))
        assertTrue(splash.contains("FigmaGold"))
        assertTrue(login.contains("FigmaBrandRow"))
        assertTrue(login.contains("FigmaPasswordField"))

        val home = projectFile("src/main/java/bo/org/siafco/app/feature/home/HomeScreen.kt").readText(Charsets.UTF_8)
        assertFalse(home.contains("BrandHeader("))
        assertFalse(home.contains("AffiliationSummaryCard"))
        assertFalse(home.contains("home_request_amount"))
        assertFalse(home.contains("home_request_capabilities"))
        assertTrue(home.contains("AffiliatePhoto"))
        assertTrue(home.contains("PendingOrdersSection"))
        assertTrue(home.contains("ServicesSection"))
        assertTrue(home.contains("No pudimos actualizar tus pedidos"))
        assertTrue(home.indexOf("SectionHeader(title = \"Últimas actividades\"") > home.indexOf("if (orders.isNotEmpty())"))
        assertTrue(home.contains("formatOrderDate"))
        listOf("\"PER\"", "\"CRE\"", "\"AFI\"", "\"TIE\"", "\"PED\"").forEach { marker ->
            assertFalse(home.contains(marker))
        }
        val components = projectFile("src/main/java/bo/org/siafco/app/core/ui/CooperativeComponents.kt").readText(Charsets.UTF_8)
        listOf("\"IN\"", "\"TI\"", "\"PE\"", "\"CR\"", "\"PF\"").forEach { marker ->
            assertFalse(components.contains(marker))
        }
    }

    private fun projectFile(path: String): File =
        sequenceOf(File(path), File("app/$path")).first { it.exists() }
}
