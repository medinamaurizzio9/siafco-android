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
    }

    @Test
    fun activeProfileIsMappedAsFullAccess() = runTest {
        server.enqueue(jsonResponse(loginJson(status = "activo", accessLevel = "full")))

        val result = repository.login("ana@example.test", "Secret1234")

        assertEquals(AccessLevel.Active, (result as ApiResult.Success).value.accessLevel)
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
    fun logoutCallsApiAndClearsToken() = runTest {
        tokenStore.saveToken("token")
        server.enqueue(jsonResponse("""{"success":true,"message":"OK","data":{}}"""))

        val result = repository.logout()

        assertTrue(result is ApiResult.Success)
        assertNull(tokenStore.getToken())
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
          }
        }
    """.trimIndent()

    private fun errorJson() = """{"success":false,"message":"Error","errors":{}}"""

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
