package bo.org.siafco.app

import bo.org.siafco.app.feature.profile.backendBirthDateFromParts
import bo.org.siafco.app.feature.profile.formatBirthDateForDisplay
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileFormattersTest {
    @Test
    fun birthDateIsShownLocalAndSentAsBackendFormat() {
        assertEquals("15/05/1990", formatBirthDateForDisplay("1990-05-15"))
        assertEquals("1990-05-15", backendBirthDateFromParts(1990, 4, 15))
    }
}
