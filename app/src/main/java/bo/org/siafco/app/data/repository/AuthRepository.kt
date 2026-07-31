package bo.org.siafco.app.data.repository

import bo.org.siafco.app.core.data.TokenStore
import bo.org.siafco.app.core.network.ApiResult
import bo.org.siafco.app.data.remote.ApiEnvelope
import bo.org.siafco.app.data.remote.LoginRequest
import bo.org.siafco.app.data.remote.ProfilePayload
import bo.org.siafco.app.data.remote.SiafcoApi
import bo.org.siafco.app.domain.AccessLevel
import bo.org.siafco.app.domain.SessionProfile
import kotlinx.coroutines.flow.Flow
import retrofit2.Response
import java.io.IOException

class AuthRepository(
    private val api: SiafcoApi,
    private val tokenStore: TokenStore
) {
    val token: Flow<String?> = tokenStore.token

    suspend fun login(email: String, password: String): ApiResult<SessionProfile> = safeCall {
        val response = api.login(LoginRequest(email = email, password = password))
        if (response.isSuccessful) {
            val body = response.body()
            val payload = body?.data
            if (body?.success == true && payload != null) {
                tokenStore.saveToken(payload.accessToken)
                ApiResult.Success(payload.profile.toDomain())
            } else {
                ApiResult.UnknownError
            }
        } else {
            response.toHttpError()
        }
    }

    suspend fun validateSession(): ApiResult<SessionProfile> = safeCall {
        if (tokenStore.getToken().isNullOrBlank()) {
            return@safeCall ApiResult.HttpError(401, null)
        }
        val response = api.me()
        if (response.isSuccessful) {
            val payload = response.body()?.data
            if (response.body()?.success == true && payload != null) {
                ApiResult.Success(payload.profile.toDomain())
            } else {
                ApiResult.UnknownError
            }
        } else {
            if (response.code() == 401) tokenStore.clearToken()
            response.toHttpError()
        }
    }

    suspend fun logout(): ApiResult<Unit> = safeCall {
        val response = api.logout()
        tokenStore.clearToken()
        if (response.isSuccessful) {
            ApiResult.Success(Unit)
        } else {
            response.toHttpError()
        }
    }

    suspend fun clearLocalSession() {
        tokenStore.clearToken()
    }

    private suspend fun <T> safeCall(block: suspend () -> ApiResult<T>): ApiResult<T> {
        return try {
            block()
        } catch (_: IOException) {
            ApiResult.NetworkError
        } catch (_: RuntimeException) {
            ApiResult.UnknownError
        }
    }

    private fun <T> Response<ApiEnvelope<T>>.toHttpError(): ApiResult.HttpError {
        return ApiResult.HttpError(code = code(), message = message())
    }

    private fun bo.org.siafco.app.data.remote.MobileProfileDto.toDomain(): SessionProfile {
        val status = affiliate?.status.orEmpty()
        return SessionProfile(
            name = affiliate?.fullName ?: user.name,
            email = user.email,
            affiliateStatus = status,
            affiliateStatusLabel = affiliate?.statusLabel ?: status.ifBlank { "Sin estado" },
            accessLevel = if (status == "activo" || affiliate?.accessLevel == "full") {
                AccessLevel.Active
            } else {
                AccessLevel.Pending
            }
        )
    }
}
