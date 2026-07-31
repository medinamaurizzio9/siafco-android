package bo.org.siafco.app

import bo.org.siafco.app.data.remote.ApiEnvelope
import bo.org.siafco.app.data.remote.CatalogsPayload
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogsSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun catalogsEnvelopeMatchesLaravelMobileContract() {
        val envelope = json.decodeFromString<ApiEnvelope<CatalogsPayload>>(CATALOGS_JSON)

        assertTrue(envelope.success)
        assertEquals("OK", envelope.message)
        assertEquals("Magisterio Rural", envelope.data?.sectors?.first()?.name)
        assertEquals(250.0, envelope.data?.plans?.first()?.totalAmount ?: 0.0, 0.0)
        assertEquals("LP", envelope.data?.issuedIn?.first()?.value)
        assertEquals("2026.1", envelope.data?.institution?.termsVersion)
    }

    private companion object {
        private const val CATALOGS_JSON = """
            {
              "success": true,
              "message": "OK",
              "data": {
                "sectors": [
                  {
                    "id": 41,
                    "name": "Magisterio Rural",
                    "code": "MAG-RUR",
                    "regional": "La Paz",
                    "institution": "Cooperativa Tierra Bendita"
                  }
                ],
                "plans": [
                  {
                    "id": 41,
                    "sector_id": null,
                    "name": "AFILIACION INICIAL",
                    "type": "independiente",
                    "currency": "BOB",
                    "affiliation_fee": 250,
                    "credential_fee": 0,
                    "total_amount": 250,
                    "description": "PAGO INICIAL DE AFILIACION Y CREDENCIAL DIGITAL.",
                    "payment_instructions": null
                  }
                ],
                "regionals": ["LA PAZ"],
                "issued_in": [{"value": "LP", "label": "La Paz"}],
                "marital_statuses": ["SOLTERO"],
                "institution": {
                  "name": "COOPERATIVA TIERRA BENDITA",
                  "email": "no-reply@siafco.test",
                  "phone": null,
                  "address": null,
                  "payment_bank": null,
                  "payment_holder": null,
                  "payment_account": null,
                  "payment_instructions": null,
                  "terms_version": "2026.1",
                  "privacy_version": "2026.1"
                }
              }
            }
        """
    }
}
