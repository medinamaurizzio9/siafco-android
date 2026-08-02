package bo.org.siafco.app

import bo.org.siafco.app.core.data.TokenStore
import bo.org.siafco.app.data.remote.SiafcoApi
import bo.org.siafco.app.data.repository.CredentialQrDecoder
import bo.org.siafco.app.data.repository.CredentialRepository
import bo.org.siafco.app.data.repository.CredentialResult
import bo.org.siafco.app.data.repository.CredentialUrlPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.Base64

class CredentialRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var tokenStore: MemoryTokenStore
    private lateinit var repository: CredentialRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        tokenStore = MemoryTokenStore()
        repository = CredentialRepository(api(server, tokenStore), tokenStore)
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun deserializesLaravelCredentialJsonThroughAuthenticatedRoute() = runTest {
        tokenStore.saveToken("secret-token")
        server.enqueue(jsonResponse(successJson()))

        val result = repository.loadCredential()

        assertTrue(result is CredentialResult.Success)
        val credential = (result as CredentialResult.Success).credential
        assertEquals("Cooperativa Tierra Bendita", credential.institutionName)
        assertEquals("AFILIADO MOVIL", credential.affiliateName)
        assertEquals("REG-A", credential.registrationNumber)
        assertEquals("MAGISTERIO RURAL", credential.sector)
        assertEquals("LA PAZ", credential.regional)
        assertEquals("activo", credential.status)
        assertEquals("AFILIADO ACTIVO", credential.statusLabel)
        assertEquals("02/08/2026", credential.issuedAt)
        assertEquals("https://siafco.test/storage/affiliates/photos/a.jpg?v=123", credential.photoUrl)
        assertEquals("https://siafco.test/verificar/token-publico", credential.verificationUrl)
        assertTrue(credential.qrPngBytes.isNotEmpty())

        val request = server.takeRequest()
        assertEquals("/api/mobile/v1/me/credential", request.url.encodedPath)
        assertEquals("Bearer secret-token", request.headers["Authorization"])
    }

    @Test
    fun localVerificationUrlFromLaravelIsResolvedForEmulatorWithoutChangingQr() = runTest {
        tokenStore.saveToken("secret-token")
        val qr = pngDataUri()
        server.enqueue(jsonResponse(successJson(verificationUrl = "http://127.0.0.1:8000/verificar/abc?source=mobile#qr", qrImage = qr)))
        server.enqueue(jsonResponse(successJson(verificationUrl = "http://localhost:8000/verificar/abc?source=mobile#qr", qrImage = qr)))

        val first = repository.loadCredential()
        val second = repository.loadCredential()

        assertTrue(first is CredentialResult.Success)
        assertEquals(
            "http://10.0.2.2:8000/verificar/abc?source=mobile#qr",
            (first as CredentialResult.Success).credential.verificationUrl
        )
        assertTrue(second is CredentialResult.Success)
        assertEquals(
            "http://10.0.2.2:8000/verificar/abc?source=mobile#qr",
            (second as CredentialResult.Success).credential.verificationUrl
        )
        assertEquals(
            CredentialQrDecoder.decode(qr)?.toList(),
            second.credential.qrPngBytes.toList()
        )
    }

    @Test
    fun localPhotoUrlFromLaravelIsResolvedForEmulator() = runTest {
        server.enqueue(jsonResponse(successJson(photoUrl = "http://127.0.0.1:8000/storage/affiliates/photos/a.jpg?v=123")))

        val result = repository.loadCredential()

        assertTrue(result is CredentialResult.Success)
        assertEquals(
            "http://10.0.2.2:8000/storage/affiliates/photos/a.jpg?v=123",
            (result as CredentialResult.Success).credential.photoUrl
        )
    }

    @Test
    fun unsafePhotoUrlIsDroppedWithoutRejectingCredential() = runTest {
        server.enqueue(jsonResponse(successJson(photoUrl = "javascript:alert(1)")))

        val result = repository.loadCredential()

        assertTrue(result is CredentialResult.Success)
        assertNull((result as CredentialResult.Success).credential.photoUrl)
    }

    @Test
    fun externalHttpVerificationUrlIsNotRewrittenAndIsRejected() = runTest {
        server.enqueue(jsonResponse(successJson(verificationUrl = "http://example.test/verificar/abc?source=mobile#qr")))

        assertTrue(repository.loadCredential() is CredentialResult.InvalidPayload)
    }

    @Test
    fun mapsCredentialErrorsAndClearsOnlyExpiredToken() = runTest {
        tokenStore.saveToken("expired")
        server.enqueue(jsonResponse("""{"success":false,"message":"No autenticado.","errors":{}}""", 401))
        server.enqueue(jsonResponse("""{"success":false,"message":"No disponible.","errors":{}}""", 403))
        server.enqueue(jsonResponse("""{"success":false,"message":"No generada.","errors":{}}""", 404))
        server.enqueue(jsonResponse("""{"success":false,"message":"Limite.","errors":{}}""", 429))

        assertTrue(repository.loadCredential() is CredentialResult.Unauthorized)
        assertNull(tokenStore.getToken())
        assertTrue(repository.loadCredential() is CredentialResult.Forbidden)
        assertTrue(repository.loadCredential() is CredentialResult.NotFound)
        assertTrue(repository.loadCredential() is CredentialResult.RateLimited)
    }

    @Test
    fun networkErrorDoesNotClearToken() = runTest {
        tokenStore.saveToken("still-valid")
        server.close()

        val result = repository.loadCredential()

        assertTrue(result is CredentialResult.NetworkError)
        assertEquals("still-valid", tokenStore.getToken())
    }

    @Test
    fun invalidJsonOrCredentialPayloadIsControlled() = runTest {
        server.enqueue(jsonResponse("""{"success":true,"message":"OK","data":{}}"""))
        server.enqueue(jsonResponse("""not-json"""))

        assertTrue(repository.loadCredential() is CredentialResult.InvalidPayload)
        assertTrue(repository.loadCredential() is CredentialResult.InvalidPayload)
    }

    @Test
    fun qrDecoderAcceptsOnlyPngDataUriWithinLimits() {
        assertNotNull(CredentialQrDecoder.decode(pngDataUri()))
        assertNull(CredentialQrDecoder.decode("data:image/jpeg;base64,${Base64.getEncoder().encodeToString(pngBytes())}"))
        assertNull(CredentialQrDecoder.decode("data:image/png;base64,not-valid"))
        assertNull(CredentialQrDecoder.decode("data:image/png;base64,${Base64.getEncoder().encodeToString("no-png".toByteArray())}"))
        assertNull(CredentialQrDecoder.decode("data:image/png;base64,${"A".repeat(700_001)}"))
    }

    @Test
    fun verificationUrlPolicyAllowsOnlyExpectedSchemes() {
        assertEquals("https://siafco.test/verificar/a", CredentialUrlPolicy.validate("https://siafco.test/verificar/a", isDebug = false))
        assertEquals("http://10.0.2.2:8000/verificar/a", CredentialUrlPolicy.validate("http://10.0.2.2:8000/verificar/a", isDebug = true))
        assertEquals("http://localhost:8000/verificar/a", CredentialUrlPolicy.validate("http://localhost:8000/verificar/a", isDebug = true))
        assertEquals("http://127.0.0.1:8000/verificar/a", CredentialUrlPolicy.validate("http://127.0.0.1:8000/verificar/a", isDebug = true))
        assertNull(CredentialUrlPolicy.validate("http://siafco.test/verificar/a", isDebug = true))
        assertNull(CredentialUrlPolicy.validate("http://10.0.2.2:8000/verificar/a", isDebug = false))
        assertNull(CredentialUrlPolicy.validate("javascript:alert(1)", isDebug = true))
        assertNull(CredentialUrlPolicy.validate("file:///tmp/a", isDebug = true))
        assertNull(CredentialUrlPolicy.validate("content://provider/a", isDebug = true))
        assertNull(CredentialUrlPolicy.validate("data:text/plain,a", isDebug = true))
        assertNull(CredentialUrlPolicy.validate("intent://verify#Intent;scheme=https;end", isDebug = true))
    }

    private fun api(server: MockWebServer, tokenStore: TokenStore): SiafcoApi {
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val token = runBlocking { tokenStore.getToken() }
                val request = if (token.isNullOrBlank()) {
                    chain.request()
                } else {
                    chain.request().newBuilder().header("Authorization", "Bearer $token").build()
                }
                chain.proceed(request)
            }
            .build()
        return Retrofit.Builder()
            .baseUrl(server.url("/api/mobile/v1/"))
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SiafcoApi::class.java)
    }

    private fun successJson(
        verificationUrl: String = "https://siafco.test/verificar/token-publico",
        qrImage: String = pngDataUri(),
        photoUrl: String? = "https://siafco.test/storage/affiliates/photos/a.jpg?v=123"
    ): String = """
        {
          "success": true,
          "message": "Credencial movil disponible.",
          "data": {
            "credential": {
              "institution_name": "Cooperativa Tierra Bendita",
              "affiliate_name": "AFILIADO MOVIL",
              "registration_number": "REG-A",
              "sector": "MAGISTERIO RURAL",
              "regional": "LA PAZ",
              "status": "activo",
              "status_label": "AFILIADO ACTIVO",
              "issued_at": "02/08/2026",
              "photo_url": ${photoUrl?.let { "\"$it\"" } ?: "null"},
              "verification_url": "$verificationUrl",
              "qr_image": "$qrImage"
            }
          }
        }
    """.trimIndent()

    private fun jsonResponse(body: String, code: Int = 200): MockResponse =
        MockResponse.Builder()
            .code(code)
            .addHeader("Content-Type", "application/json")
            .body(body)
            .build()

    private fun pngDataUri(): String = "data:image/png;base64,${Base64.getEncoder().encodeToString(pngBytes())}"

    private fun pngBytes(): ByteArray =
        Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=")

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
