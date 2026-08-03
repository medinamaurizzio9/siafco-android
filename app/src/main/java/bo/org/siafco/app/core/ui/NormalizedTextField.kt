package bo.org.siafco.app.core.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import bo.org.siafco.app.core.text.HumanTextInputNormalizer
import bo.org.siafco.app.core.text.TextInputNormalization

@Composable
fun NormalizedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    normalization: TextInputNormalization = TextInputNormalization.None
) {
    var fieldValue by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    LaunchedEffect(value) {
        if (value != fieldValue.text) {
            fieldValue = fieldValue.copy(text = value, selection = TextRange(value.length), composition = null)
        }
    }

    val normalizedKeyboardOptions = when (normalization) {
        TextInputNormalization.Human,
        TextInputNormalization.Coupon -> keyboardOptions.copy(capitalization = KeyboardCapitalization.Characters)
        TextInputNormalization.None -> keyboardOptions
    }

    OutlinedTextField(
        value = fieldValue,
        onValueChange = { incoming ->
            val normalized = HumanTextInputNormalizer.visual(incoming, normalization)
            fieldValue = normalized
            if (normalized.text != value) onValueChange(normalized.text)
        },
        modifier = modifier,
        label = label,
        isError = isError,
        supportingText = supportingText,
        singleLine = singleLine,
        keyboardOptions = normalizedKeyboardOptions
    )
}
