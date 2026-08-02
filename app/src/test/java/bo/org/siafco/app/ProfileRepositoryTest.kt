package bo.org.siafco.app

import bo.org.siafco.app.core.data.TokenStore
import bo.org.siafco.app.data.remote.SiafcoApi
import bo.org.siafco.app.data.repository.ProfileRepository
import bo.org.siafco.app.data.repository.ProfileResult
import bo.org.siafco.app.domain.ProfileUpdateForm
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File

class ProfileRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var tokenStore: MemoryTokenStore
    private lateinit var repository: ProfileRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        tokenStore = MemoryTokenStore()
        repository = ProfileRepository(api(server), tokenStore)
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun updateProfileUsesPatchJsonWithOnlyEditableFields() = runTest {
        server.enqueue(jsonResponse(PROFILE_JSON))

        val result = repository.updateProfile(
            ProfileUpdateForm(
                phone = "76543210",
                email = "profile@siafco.test",
                address = "Nueva direccion",
                birthDate = "1990-05-15",
                maritalStatus = "CASADO"
            )
        )

        assertTrue(result is ProfileResult.Success)
        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/mobile/v1/me/profile", request.url.encodedPath)
        assertTrue(request.headers["Content-Type"].orEmpty().startsWith("application/json"))
        val body = request.body!!.utf8()
        assertTrue(body.contains("\"phone\":\"76543210\""))
        assertTrue(body.contains("\"email\":\"profile@siafco.test\""))
        assertTrue(body.contains("\"address\":\"Nueva direccion\""))
        assertTrue(body.contains("\"birth_date\":\"1990-05-15\""))
        assertTrue(body.contains("\"marital_status\":\"CASADO\""))
        assertFalse(body.contains("photo"))
        assertFalse(body.contains("_method"))
        assertFalse(request.headers["Content-Type"].orEmpty().startsWith("multipart/form-data"))
    }

    @Test
    fun updatePhotoUsesPostMultipartPhotoPart() = runTest {
        server.enqueue(jsonResponse(PROFILE_JSON))
        val photo = photo()

        val result = repository.updatePhoto(photo)

        assertTrue(result is ProfileResult.Success)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/mobile/v1/me/profile/photo", request.url.encodedPath)
        assertTrue(request.headers["Content-Type"].orEmpty().startsWith("multipart/form-data"))
        val body = request.body!!.utf8()
        assertTrue(body.contains("name=\"photo\""))
        assertTrue(body.contains("filename=\"${photo.file.name}\""))
        assertFalse(body.contains("_method"))
    }

    @Test
    fun loadPreservesAffiliatePhotoUrlFromBackend() = runTest {
        server.enqueue(jsonResponse(PROFILE_JSON_WITH_PHOTO))

        val result = repository.load()

        assertTrue(result is ProfileResult.Success)
        assertEquals(
            "http://127.0.0.1:8000/storage/affiliates/photos/profile-photo.jpg",
            (result as ProfileResult.Success).profile.photoUrl
        )
    }

    @Test
    fun profileValidationUnauthorizedForbiddenAndRateLimitAreMapped() = runTest {
        server.enqueue(jsonResponse("""{"success":false,"message":"Validacion","errors":{"email":["Correo repetido."]}}""", 422))
        server.enqueue(jsonResponse("""{"success":false,"message":"No autenticado.","errors":{}}""", 401))
        server.enqueue(jsonResponse("""{"success":false,"message":"Sin permiso.","errors":{}}""", 403))
        server.enqueue(jsonResponse("""{"success":false,"message":"Espera.","errors":{}}""", 429))

        assertTrue(repository.updateProfile(ProfileUpdateForm(email = "bad@siafco.test")) is ProfileResult.ValidationError)
        assertTrue(repository.updateProfile(ProfileUpdateForm(email = "bad@siafco.test")) is ProfileResult.Unauthorized)
        assertTrue(repository.updateProfile(ProfileUpdateForm(email = "bad@siafco.test")) is ProfileResult.Forbidden)
        assertTrue(repository.updateProfile(ProfileUpdateForm(email = "bad@siafco.test")) is ProfileResult.RateLimited)
    }

    @Test
    fun http200WithSuccessFalseIsNotTreatedAsProfileSuccess() = runTest {
        server.enqueue(jsonResponse("""{"success":false,"message":"No actualizado.","errors":{"phone":["Invalido."]}}"""))

        val result = repository.updateProfile(ProfileUpdateForm(email = "profile@siafco.test", phone = "bad"))

        assertTrue(result is ProfileResult.UnknownError)
    }

    @Test
    fun logoutAllClearsSessionScopedStoreData() = runTest {
        var cleared = false
        repository = ProfileRepository(api(server), tokenStore, onSessionBoundary = { cleared = true })
        tokenStore.saveToken("token")
        server.enqueue(jsonResponse("""{"success":true,"message":"OK","data":{}}"""))

        val result = repository.logoutAll()

        assertTrue(result is ProfileResult.LoggedOut)
        assertTrue(cleared)
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

    private fun photo(): PreparedPhoto {
        val file = File.createTempFile("profile", ".jpg").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4))
            deleteOnExit()
        }
        return PreparedPhoto(file, "profile.jpg")
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
        private const val PROFILE_JSON = """
            {
              "success": true,
              "message": "Perfil actualizado.",
              "data": {
                "profile": {
                  "user": {
                    "name": "Afiliado Demo",
                    "email": "profile@siafco.test",
                    "role": "afiliado",
                    "user_type": "affiliate",
                    "must_change_password": false,
                    "is_active": true,
                    "last_login_at": null
                  },
                  "affiliate": {
                    "full_name": "Afiliado Demo",
                    "ci": "90010001",
                    "phone": "76543210",
                    "email": "profile@siafco.test",
                    "address": "Nueva direccion",
                    "birth_date": "1990-05-15",
                    "marital_status": "CASADO",
                    "photo_url": null,
                    "registration_number": "MAG-1",
                    "status": "activo",
                    "status_label": "Activo",
                    "sector": {"name":"Magisterio","code":"MAG","regional":"LA PAZ","institution":"Institucion"},
                    "plan": {"name":"Plan base","type":"regular","currency":"BOB","affiliation_fee":100,"credential_fee":30,"total_amount":130}
                  },
                  "allowed_profile_fields": ["phone","email","address","birth_date","marital_status"]
                }
              }
            }
        """

        private const val PROFILE_JSON_WITH_PHOTO = """
            {
              "success": true,
              "message": "Perfil cargado.",
              "data": {
                "profile": {
                  "user": {
                    "name": "Afiliado Demo",
                    "email": "profile@siafco.test",
                    "role": "afiliado",
                    "user_type": "affiliate",
                    "must_change_password": false,
                    "is_active": true,
                    "last_login_at": null
                  },
                  "affiliate": {
                    "full_name": "Afiliado Demo",
                    "ci": "90010001",
                    "phone": "76543210",
                    "email": "profile@siafco.test",
                    "address": "Nueva direccion",
                    "birth_date": "1990-05-15",
                    "marital_status": "CASADO",
                    "photo_url": "http://127.0.0.1:8000/storage/affiliates/photos/profile-photo.jpg",
                    "registration_number": "MAG-1",
                    "status": "activo",
                    "status_label": "Activo",
                    "sector": {"name":"Magisterio","code":"MAG","regional":"LA PAZ","institution":"Institucion"},
                    "plan": {"name":"Plan base","type":"regular","currency":"BOB","affiliation_fee":100,"credential_fee":30,"total_amount":130}
                  },
                  "allowed_profile_fields": ["phone","email","address","birth_date","marital_status","photo"]
                }
              }
            }
        """
    }
}
