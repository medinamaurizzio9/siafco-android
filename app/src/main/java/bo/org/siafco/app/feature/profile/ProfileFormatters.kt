package bo.org.siafco.app.feature.profile

import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val backendDate = DateTimeFormatter.ISO_LOCAL_DATE
private val localDate = DateTimeFormatter.ofPattern("dd/MM/yyyy")

fun formatBirthDateForDisplay(value: String): String {
    return runCatching { LocalDate.parse(value, backendDate).format(localDate) }.getOrDefault(value)
}

fun backendBirthDateFromParts(year: Int, monthZeroBased: Int, day: Int): String {
    return LocalDate.of(year, monthZeroBased + 1, day).format(backendDate)
}
