package bo.org.siafco.app

import bo.org.siafco.app.core.ui.backendDateFromParts
import org.junit.Assert.assertEquals
import org.junit.Test

class FigmaDateFieldTest {
    @Test
    fun calendarSelectionKeepsBackendDateFormat() {
        assertEquals("2026-08-07", backendDateFromParts(2026, 7, 7))
        assertEquals("1990-01-05", backendDateFromParts(1990, 0, 5))
    }
}
