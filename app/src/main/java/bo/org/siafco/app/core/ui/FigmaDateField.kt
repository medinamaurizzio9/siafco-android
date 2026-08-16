package bo.org.siafco.app.core.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import bo.org.siafco.app.R
import java.time.LocalDate
import java.util.Locale

@Composable
fun FigmaDateField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    maxSelectableDate: LocalDate? = null
) {
    val context = LocalContext.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        isError = error != null,
        supportingText = { error?.let { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        trailingIcon = {
            IconButton(
                onClick = {
                    val initial = parseBackendDate(value)
                        ?: maxSelectableDate
                        ?: LocalDate.now()
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            onValueChange(backendDateFromParts(year, month, day))
                        },
                        initial.year,
                        initial.monthValue - 1,
                        initial.dayOfMonth
                    ).apply {
                        maxSelectableDate?.let { datePicker.maxDate = it.toEpochDay() * 86_400_000L }
                    }.show()
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_profile_calendar),
                    contentDescription = label,
                    tint = FigmaNavy
                )
            }
        },
        shape = SiafcoTextFieldShape,
        colors = siafcoOutlinedTextFieldColors()
    )
}

fun backendDateFromParts(year: Int, month: Int, day: Int): String =
    String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)

private fun parseBackendDate(value: String): LocalDate? =
    runCatching { LocalDate.parse(value) }.getOrNull()
