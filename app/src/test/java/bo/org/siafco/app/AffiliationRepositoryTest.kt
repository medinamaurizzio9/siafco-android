package bo.org.siafco.app

import bo.org.siafco.app.core.data.TokenStore
import bo.org.siafco.app.data.remote.SiafcoApi
import bo.org.siafco.app.data.repository.AffiliationRepository
import bo.org.siafco.app.data.repository.AffiliationRepositoryResult
import bo.org.siafco.app.domain.AffiliationRegistrationForm
import bo.org.siafco.app.domain.PreparedPhoto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File

class AffiliationRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var tokenStore: MemoryTokenStore
    private lateinit var repository: AffiliationRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        tokenStore = MemoryTokenStore()
        repository = AffiliationRepository(api(server), tokenStore)
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun catalogsAreMappedFromLaravelContract() = runTest {
        server.enqueue(jsonResponse(CATALOGS_JSON))

        val result = repository.catalogs()

        assertTrue(result is AffiliationRepositoryResult.Success)
        val catalogs = (result as AffiliationRepositoryResult.Success).value
        assertEquals("Magisterio Rural", catalogs.sectors.first().name)
        assertEquals("AFILIACION INICIAL", catalogs.plans.first().name)
        assertEquals("2026.1", catalogs.institution.termsVersion)
    }

    @Test
    fun successfulRegistrationStoresTokenAndSendsMultipart() = runTest {
        server.enqueue(jsonResponse(REGISTRATION_JSON, 201))

        val result = repository.register(validForm())

        assertTrue(result is AffiliationRepositoryResult.Success)
        val success = (result as AffiliationRepositoryResult.Success).value
        assertEquals("SOL-1", success.requestCode)
        assertEquals("SOL-1", success.profile.requestCode)
        assertEquals("token-456", tokenStore.getToken())
        val request = server.takeRequest()
        assertEquals("/api/mobile/v1/affiliation-requests", request.url.encodedPath)
        assertTrue(request.headers["Content-Type"].orEmpty().startsWith("multipart/form-data"))
        assertNotNull(request.body)
        val body = request.body!!.utf8()
        assertTrue(body.contains("ANA PEREZ"))
        assertTrue(body.contains("CALLE"))
        assertTrue(body.contains("INSTITUCION"))
        assertTrue(body.contains("CARGO"))
        assertTrue(body.contains("ana@siafco.test"))
        assertTrue(body.contains("Secret123"))
    }

    @Test
    fun validationErrorsAreMappedByField() = runTest {
        server.enqueue(jsonResponse("""{"success":false,"message":"Error","errors":{"email":["Correo invalido."]}}""", 422))

        val result = repository.register(validForm())

        assertTrue(result is AffiliationRepositoryResult.ValidationError)
        assertEquals("Correo invalido.", (result as AffiliationRepositoryResult.ValidationError).errors["email"]?.first())
        assertNull(tokenStore.getToken())
    }

    @Test
    fun existingAccountIsReportedAsConflict() = runTest {
        server.enqueue(jsonResponse("""{"success":false,"message":"Ya existe una cuenta o solicitud asociada.","errors":[]}""", 409))

        val result = repository.register(validForm())

        assertTrue(result is AffiliationRepositoryResult.Conflict)
    }

    @Test
    fun rateLimitIsMapped() = runTest {
        server.enqueue(jsonResponse("""{"success":false,"message":"Demasiados intentos.","errors":[]}""", 429))

        val result = repository.register(validForm())

        assertTrue(result is AffiliationRepositoryResult.RateLimited)
    }

    @Test
    fun networkFailureDoesNotStoreToken() = runTest {
        server.close()

        val result = repository.register(validForm())

        assertTrue(result is AffiliationRepositoryResult.NetworkError)
        assertNull(tokenStore.getToken())
    }

    private fun validForm(): AffiliationRegistrationForm {
        val file = File.createTempFile("siafco-test-photo", ".jpg").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4))
            deleteOnExit()
        }
        return AffiliationRegistrationForm(
            fullName = "Ana Perez",
            ci = "123",
            issuedIn = "LP",
            birthDate = "1990-01-01",
            maritalStatus = "SOLTERO",
            phone = "70000001",
            email = "ana@siafco.test",
            address = "Calle",
            password = "Secret123",
            passwordConfirmation = "Secret123",
            sectorId = 1,
            planId = 1,
            regional = "LA PAZ",
            institution = "Institucion",
            position = "Cargo",
            photo = PreparedPhoto(file, "photo.jpg"),
            termsAccepted = true,
            privacyAccepted = true
        )
    }

    private fun api(server: MockWebServer): SiafcoApi {
        val json = Json { ignoreUnknownKeys = true }
        return Retrofit.Builder()
            .baseUrl(server.url("/api/mobile/v1/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SiafcoApi::class.java)
    }

    private fun jsonResponse(body: String, code: Int = 200): MockResponse {
        return MockResponse.Builder()
            .code(code)
            .addHeader("Content-Type", "application/json")
            .body(body)
            .build()
    }

    private class MemoryTokenStore : TokenStore {
        private val state = MutableStateFlow<String?>(null)
        override val token: Flow<String?> = state
        override suspend fun getToken(): String? = state.value
        override suspend fun saveToken(token: String) {
            state.value = token
        }
        override suspend fun clearToken() {
            state.value = null
        }
    }

    private companion object {
        private const val CATALOGS_JSON = """
            {
              "success": true,
              "message": "OK",
              "data": {
                "sectors": [{"id":1,"name":"Magisterio Rural","code":"MAG","regional":"La Paz","institution":"Coop"}],
                "plans": [{"id":1,"sector_id":null,"name":"AFILIACION INICIAL","type":"independiente","currency":"BOB","affiliation_fee":250,"credential_fee":0,"total_amount":250,"description":"Plan","payment_instructions":null}],
                "regionals": ["LA PAZ"],
                "issued_in": [{"value":"LP","label":"La Paz"}],
                "marital_statuses": ["SOLTERO"],
                "institution": {"name":"SIAFCO","email":null,"phone":null,"address":null,"payment_bank":null,"payment_holder":null,"payment_account":null,"payment_instructions":null,"terms_version":"2026.1","privacy_version":"2026.1"}
              }
            }
        """

        private const val REGISTRATION_JSON = """
            {
              "success": true,
              "message": "Solicitud registrada.",
              "data": {
                "token_type": "Bearer",
                "access_token": "token-456",
                "profile": {
                  "user": {"name":"Ana Perez","email":"ana@siafco.test"},
                  "affiliate": {"full_name":"Ana Perez","status":"pendiente_pago","status_label":"Pendiente de pago","access_level":"limited"}
                },
                "affiliation_request": {"request_code":"SOL-1","status":"pending_payment","status_label":"Pendiente","amount_due":250}
              }
            }
        """
    }
}
