package bo.org.siafco.app.data.repository

import bo.org.siafco.app.core.data.TokenStore
import bo.org.siafco.app.data.remote.ApiEnvelope
import bo.org.siafco.app.data.remote.MobileProfileDto
import bo.org.siafco.app.data.remote.SiafcoApi
import bo.org.siafco.app.data.remote.UpdatePasswordRequest
import bo.org.siafco.app.data.remote.UpdateProfileRequest
import bo.org.siafco.app.domain.ChangePasswordForm
import bo.org.siafco.app.domain.MobileProfile
import bo.org.siafco.app.domain.PreparedPhoto
import bo.org.siafco.app.domain.ProfileUpdateForm
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import java.io.IOException

class ProfileRepository(
    private val api: SiafcoApi,
    private val tokenStore: TokenStore
) : ProfileGateway {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun load(): ProfileResult = safeCall {
        api.me().profileResult()
    }

    override suspend fun loadMaritalStatuses(): List<String> {
        return try {
            val response = api.catalogs()
            if (response.isSuccessful) {
                response.body()?.data?.maritalStatuses.orEmpty()
            } else {
                emptyList()
            }
        } catch (_: IOException) {
            emptyList()
        } catch (_: RuntimeException) {
            emptyList()
        }
    }

    override suspend fun updateProfile(changes: ProfileUpdateForm): ProfileResult = safeCall {
        api.updateProfile(
            UpdateProfileRequest(
                phone = changes.phone.ifBlank { null },
                email = changes.email.ifBlank { null },
                address = changes.address.ifBlank { null },
                birthDate = changes.birthDate.ifBlank { null },
                maritalStatus = changes.maritalStatus.ifBlank { null }
            )
        ).profileResult()
    }

    override suspend fun updatePhoto(photo: PreparedPhoto): ProfileResult = safeCall {
        val part = MultipartBody.Part.createFormData(
            name = "photo",
            filename = photo.file.name,
            body = photo.file.asRequestBody("image/jpeg".toMediaType())
        )
        api.updateProfilePhoto(part).profileResult()
    }

    override suspend fun updatePassword(form: ChangePasswordForm): ProfileResult = safeCall {
        api.updatePassword(
            UpdatePasswordRequest(
                currentPassword = form.currentPassword,
                password = form.password,
                passwordConfirmation = form.passwordConfirmation
            )
        ).profileResult()
    }

    override suspend fun logoutAll(): ProfileResult {
        return try {
            val response = api.logoutAll()
            tokenStore.clearToken()
            if (response.isSuccessful) {
                ProfileResult.LoggedOut
            } else {
                response.toProfileResult()
            }
        } catch (_: IOException) {
            ProfileResult.NetworkError
        } catch (_: RuntimeException) {
            ProfileResult.UnknownError
        }
    }

    override suspend fun clearLocalSession() {
        tokenStore.clearToken()
    }

    private suspend fun safeCall(block: suspend () -> ProfileResult): ProfileResult {
        return try {
            block()
        } catch (_: IOException) {
            ProfileResult.NetworkError
        } catch (_: RuntimeException) {
            ProfileResult.UnknownError
        }
    }

    private fun Response<ApiEnvelope<bo.org.siafco.app.data.remote.ProfilePayload>>.profileResult(): ProfileResult {
        if (isSuccessful) {
            val profile = body()?.data?.profile
            return if (body()?.success == true && profile != null) {
                ProfileResult.Success(profile.toDomain())
            } else {
                ProfileResult.UnknownError
            }
        }

        return toProfileResult()
    }

    private fun Response<*>.toProfileResult(): ProfileResult {
        val error = parseError()
        return when (code()) {
            401 -> ProfileResult.Unauthorized
            403 -> ProfileResult.Forbidden(error?.message)
            422 -> ProfileResult.ValidationError(error?.errors.orEmpty())
            429 -> ProfileResult.RateLimited(error?.message)
            else -> ProfileResult.HttpError(code(), error?.message)
        }
    }

    private fun Response<*>.parseError(): ApiEnvelope<Unit>? {
        val raw = errorBody()?.string().orEmpty()
        if (raw.isBlank()) return null
        return runCatching { json.decodeFromString<ApiEnvelope<Unit>>(raw) }.getOrNull()
    }
}

interface ProfileGateway {
    suspend fun load(): ProfileResult
    suspend fun loadMaritalStatuses(): List<String>
    suspend fun updateProfile(changes: ProfileUpdateForm): ProfileResult
    suspend fun updatePhoto(photo: PreparedPhoto): ProfileResult
    suspend fun updatePassword(form: ChangePasswordForm): ProfileResult
    suspend fun logoutAll(): ProfileResult
    suspend fun clearLocalSession()
}

sealed interface ProfileResult {
    data class Success(val profile: MobileProfile) : ProfileResult
    data class ValidationError(val errors: Map<String, List<String>>) : ProfileResult
    data class Forbidden(val message: String?) : ProfileResult
    data class RateLimited(val message: String?) : ProfileResult
    data class HttpError(val code: Int, val message: String?) : ProfileResult
    data object Unauthorized : ProfileResult
    data object LoggedOut : ProfileResult
    data object NetworkError : ProfileResult
    data object UnknownError : ProfileResult
}

fun MobileProfileDto.toDomain(): MobileProfile {
    val affiliate = affiliate
    return MobileProfile(
        fullName = affiliate?.fullName ?: user.name,
        ci = affiliate?.ci,
        email = affiliate?.email ?: user.email,
        phone = affiliate?.phone,
        address = affiliate?.address,
        birthDate = affiliate?.birthDate,
        maritalStatus = affiliate?.maritalStatus,
        photoUrl = affiliate?.photoUrl,
        registrationNumber = affiliate?.registrationNumber,
        sectorName = affiliate?.sector?.name,
        planName = affiliate?.plan?.name,
        regional = affiliate?.sector?.regional,
        institution = affiliate?.sector?.institution,
        position = null,
        status = affiliate?.status,
        statusLabel = affiliate?.statusLabel,
        allowedFields = allowedProfileFields.toSet(),
        mustChangePassword = user.mustChangePassword == true
    )
}
