package bo.org.siafco.app

import bo.org.siafco.app.core.data.TokenStore
import bo.org.siafco.app.core.network.ApiResult
import bo.org.siafco.app.data.remote.SiafcoApi
import bo.org.siafco.app.data.repository.AuthRepository
import bo.org.siafco.app.domain.AccessLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AuthRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var tokenStore: MemoryTokenStore
    private lateinit var repository: AuthRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        tokenStore = MemoryTokenStore()
        repository = AuthRepository(api = api(server), tokenStore = tokenStore)
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun loginSuccessStoresTokenAndPendingProfile() = runTest {
        server.enqueue(jsonResponse(loginJson(status = "pendiente_pago", accessLevel = "limited")))

        val result = repository.login("ana@example.test", "Secret1234")

        assertTrue(result is ApiResult.Success)
        assertEquals("token-123", tokenStore.getToken())
        assertEquals(AccessLevel.Pending, (result as ApiResult.Success).value.accessLevel)
        assertEquals(setOf("phone", "email"), result.value.allowedProfileFields)
    }

    @Test
    fun successfulLoginClearsSessionScopedStoreDataBeforeSavingNewToken() = runTest {
        var cleared = false
        repository = AuthRepository(api = api(server), tokenStore = tokenStore, onSessionBoundary = { cleared = true })
        server.enqueue(jsonResponse(loginJson(status = "activo", accessLevel = "full")))

        val result = repository.login("otra@example.test", "Secret1234")

        assertTrue(result is ApiResult.Success)
        assertTrue(cleared)
        assertEquals("token-123", tokenStore.getToken())
    }

    @Test
    fun activeProfileIsMappedAsFullAccess() = runTest {
        server.enqueue(jsonResponse(loginJson(status = "activo", accessLevel = "full")))

        val result = repository.login("ana@example.test", "Secret1234")

        assertEquals(AccessLevel.Active, (result as ApiResult.Success).value.accessLevel)
        assertTrue(result.value.hasAffiliateProfile)
    }

    @Test
    fun wrongCredentialsReturnUnauthorized() = runTest {
        server.enqueue(jsonResponse(errorJson(), code = 401))

        val result = repository.login("ana@example.test", "bad")

        assertEquals(401, (result as ApiResult.HttpError).code)
        assertNull(tokenStore.getToken())
    }

    @Test
    fun rateLimitIsReported() = runTest {
        server.enqueue(jsonResponse(errorJson(), code = 429))

        val result = repository.login("ana@example.test", "bad")

        assertEquals(429, (result as ApiResult.HttpError).code)
    }

    @Test
    fun networkErrorIsSeparatedFromExpiredSession() = runTest {
        server.close()

        val result = repository.login("ana@example.test", "Secret1234")

        assertTrue(result is ApiResult.NetworkError)
    }

    @Test
    fun validTokenAtStartupLoadsProfile() = runTest {
        tokenStore.saveToken("existing")
        server.enqueue(jsonResponse(meJson(status = "activo", accessLevel = "full")))

        val result = repository.validateSession()

        assertEquals(AccessLevel.Active, (result as ApiResult.Success).value.accessLevel)
    }

    @Test
    fun expiredTokenAtStartupIsCleared() = runTest {
        tokenStore.saveToken("expired")
        server.enqueue(jsonResponse(errorJson(), code = 401))

        val result = repository.validateSession()

        assertEquals(401, (result as ApiResult.HttpError).code)
        assertNull(tokenStore.getToken())
    }

    @Test
    fun expiredTokenAtStartupClearsSessionScopedStoreData() = runTest {
        var cleared = false
        repository = AuthRepository(api = api(server), tokenStore = tokenStore, onSessionBoundary = { cleared = true })
        tokenStore.saveToken("expired")
        server.enqueue(jsonResponse(errorJson(), code = 401))

        val result = repository.validateSession()

        assertEquals(401, (result as ApiResult.HttpError).code)
        assertNull(tokenStore.getToken())
        assertTrue(cleared)
    }

    @Test
    fun logoutCallsApiAndClearsToken() = runTest {
        tokenStore.saveToken("token")
        server.enqueue(jsonResponse("""{"success":true,"message":"OK","data":{}}"""))

        val result = repository.logout()

        assertTrue(result is ApiResult.Success)
        assertNull(tokenStore.getToken())
    }

    @Test
    fun logoutClearsSessionScopedStoreData() = runTest {
        var cleared = false
        repository = AuthRepository(api = api(server), tokenStore = tokenStore, onSessionBoundary = { cleared = true })
        tokenStore.saveToken("token")
        server.enqueue(jsonResponse("""{"success":true,"message":"OK","data":{}}"""))

        val result = repository.logout()

        assertTrue(result is ApiResult.Success)
        assertNull(tokenStore.getToken())
        assertTrue(cleared)
    }

    @Test
    fun affiliationRequestMapsRealContractWithoutInternalIds() = runTest {
        server.enqueue(jsonResponse(AFFILIATION_REQUEST_JSON))

        val result = repository.affiliationRequest()

        assertTrue(result is ApiResult.Success)
        val request = (result as ApiResult.Success).value
        assertEquals("SOL-TEST-0001", request.requestCode)
        assertEquals("pending_payment", request.status)
        assertEquals("Pendiente de pago", request.statusLabel)
        assertEquals("AFILIACION INICIAL", request.planName)
        assertEquals(250.0, request.amountDue!!, 0.0)
        assertTrue(request.canSubmitPayment)
        assertTrue(request.canLogin)
    }

    @Test
    fun affiliationRequestNotFoundIsReported() = runTest {
        server.enqueue(jsonResponse(errorJson(), code = 404))

        val result = repository.affiliationRequest()

        assertEquals(404, (result as ApiResult.HttpError).code)
    }

    @Test
    fun affiliationRequestUnauthorizedDoesNotClearTokenLocally() = runTest {
        tokenStore.saveToken("still-local")
        server.enqueue(jsonResponse(errorJson(), code = 401))

        val result = repository.affiliationRequest()

        assertEquals(401, (result as ApiResult.HttpError).code)
        assertEquals("still-local", tokenStore.getToken())
    }

    @Test
    fun affiliationRequestNetworkErrorDoesNotClearToken() = runTest {
        tokenStore.saveToken("still-local")
        server.close()

        val result = repository.affiliationRequest()

        assertTrue(result is ApiResult.NetworkError)
        assertEquals("still-local", tokenStore.getToken())
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

    private fun loginJson(status: String, accessLevel: String) = """
        {
          "success": true,
          "message": "OK",
          "data": {
            "token_type": "Bearer",
            "access_token": "token-123",
            "profile": ${profileJson(status, accessLevel)}
          }
        }
    """.trimIndent()

    private fun meJson(status: String, accessLevel: String) = """
        {
          "success": true,
          "message": "OK",
          "data": {
            "profile": ${profileJson(status, accessLevel)}
          }
        }
    """.trimIndent()

    private fun profileJson(status: String, accessLevel: String) = """
        {
          "user": {"name":"Ana Perez","email":"ana@example.test"},
          "affiliate": {
            "full_name":"Ana Perez",
            "status":"$status",
            "status_label":"$status",
            "access_level":"$accessLevel"
          },
          "allowed_profile_fields":["phone","email"]
        }
    """.trimIndent()

    private fun errorJson() = """{"success":false,"message":"Error","errors":{}}"""

    private companion object {
        private const val AFFILIATION_REQUEST_JSON = """
            {
              "success": true,
              "message": "OK",
              "data": {
                "affiliation_request": {
                  "request_code": "SOL-TEST-0001",
                  "status": "pending_payment",
                  "status_label": "Pendiente de pago",
                  "status_description": "Tu solicitud fue registrada correctamente.",
                  "observations": null,
                  "amount_due": 250,
                  "currency": "BOB",
                  "plan": {
                    "name": "AFILIACION INICIAL",
                    "type": "independiente",
                    "affiliation_fee": 250,
                    "credential_fee": 0,
                    "total_amount": 250,
                    "payment_instructions": null
                  },
                  "payment": null,
                  "payment_instructions": {
                    "bank": null,
                    "holder": null,
                    "account": null,
                    "instructions": null
                  },
                  "capabilities": {
                    "can_submit_payment": true,
                    "can_login": true,
                    "can_view_credential": false
                  }
                }
              }
            }
        """
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
}
