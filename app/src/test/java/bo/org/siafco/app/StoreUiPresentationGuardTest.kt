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

        assertTrue(screens.contains("QuoteSummary(it, compact = true)"))
        assertTrue(screens.contains("stringResource(R.string.store_order_number, order.code)"))
        assertTrue(screens.contains("stringResource(R.string.store_receipt_order, orderCode)"))
        assertTrue(viewModels.contains("val canCreateOrder: Boolean get() = lines.isNotEmpty() && quote != null"))
    }

    @Test
    fun homeSplashAndLoginUseOfficialBrandHeader() {
        listOf(
            "src/main/java/bo/org/siafco/app/feature/splash/SplashScreen.kt",
            "src/main/java/bo/org/siafco/app/feature/auth/LoginScreen.kt",
            "src/main/java/bo/org/siafco/app/feature/home/HomeScreen.kt"
        ).forEach { path ->
            assertTrue("$path should use BrandHeader", projectFile(path).readText(Charsets.UTF_8).contains("BrandHeader("))
        }
    }

    private fun projectFile(path: String): File =
        sequenceOf(File(path), File("app/$path")).first { it.exists() }
}
