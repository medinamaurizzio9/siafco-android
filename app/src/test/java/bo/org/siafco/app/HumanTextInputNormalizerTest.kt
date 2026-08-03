package bo.org.siafco.app

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import bo.org.siafco.app.core.text.HumanTextInputNormalizer
import bo.org.siafco.app.core.text.TextInputNormalization
import org.junit.Assert.assertEquals
import org.junit.Test

class HumanTextInputNormalizerTest {
    @Test
    fun uppercasesHumanTextWithSpanishCharacters() {
        assertEquals("MAURIZZIO MEDINA", HumanTextInputNormalizer.visual("Maurizzio Medina"))
        assertEquals("LA PAZ", HumanTextInputNormalizer.visual("la paz"))
        assertEquals("ÑUÑOA", HumanTextInputNormalizer.visual("Ñuñoa"))
        assertEquals("ÁÉÍÓÚ Ü", HumanTextInputNormalizer.visual("áéíóú ü"))
    }

    @Test
    fun removesOnlyUnnecessaryLeadingSpacesWhileKeepingCursorStable() {
        val normalized = HumanTextInputNormalizer.visual(
            TextFieldValue("  la paz", selection = TextRange(5))
        )

        assertEquals("LA PAZ", normalized.text)
        assertEquals(TextRange(3), normalized.selection)
    }

    @Test
    fun noneModeLeavesTechnicalAndSensitiveValuesUnchanged() {
        val values = listOf(
            "afiliado.demo@siafco.test",
            "Clave Secreta 123",
            "0f2ca9d6-9646-42fa-9217-6ddf3ce8553c",
            "https://siafco.test/verificar/abc",
            "PED-2026-0001",
            "7845123",
            "250.50"
        )

        values.forEach { value ->
            assertEquals(value, HumanTextInputNormalizer.visual(value, TextInputNormalization.None))
            assertEquals(value, HumanTextInputNormalizer.forSubmit(value, TextInputNormalization.None))
        }
    }

    @Test
    fun couponIsUppercasedWithoutOtherPersistenceConcern() {
        assertEquals("CUPON VERANO", HumanTextInputNormalizer.visual(" cupon verano", TextInputNormalization.Coupon))
        assertEquals("CUPON VERANO", HumanTextInputNormalizer.forSubmit(" cupon   verano ", TextInputNormalization.Coupon))
    }
}
