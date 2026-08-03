package bo.org.siafco.app.core.text

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import java.util.Locale

enum class TextInputNormalization {
    None,
    Human,
    Coupon
}

object HumanTextInputNormalizer {
    private val humanLocale = Locale.forLanguageTag("es-BO")
    private val whitespace = Regex("""\s+""")

    fun visual(value: String, mode: TextInputNormalization = TextInputNormalization.Human): String =
        when (mode) {
            TextInputNormalization.None -> value
            TextInputNormalization.Human -> value.trimStart().uppercase(humanLocale)
            TextInputNormalization.Coupon -> value.trimStart().uppercase(Locale.ROOT)
        }

    fun forSubmit(value: String, mode: TextInputNormalization = TextInputNormalization.Human): String =
        when (mode) {
            TextInputNormalization.None -> value
            TextInputNormalization.Human -> value.trim().replace(whitespace, " ").uppercase(humanLocale)
            TextInputNormalization.Coupon -> value.trim().replace(whitespace, " ").uppercase(Locale.ROOT)
        }

    fun optionalForSubmit(value: String, mode: TextInputNormalization = TextInputNormalization.Human): String? =
        forSubmit(value, mode).takeIf { it.isNotBlank() }

    fun visual(value: TextFieldValue, mode: TextInputNormalization = TextInputNormalization.Human): TextFieldValue {
        if (mode == TextInputNormalization.None) return value
        val leadingSpaces = value.text.length - value.text.trimStart().length
        val normalizedText = visual(value.text, mode)
        val normalizedSelection = TextRange(
            start = (value.selection.start - leadingSpaces).coerceIn(0, normalizedText.length),
            end = (value.selection.end - leadingSpaces).coerceIn(0, normalizedText.length)
        )
        return value.copy(text = normalizedText, selection = normalizedSelection, composition = null)
    }
}
