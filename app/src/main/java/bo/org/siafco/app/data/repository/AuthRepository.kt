package bo.org.siafco.app.data.repository

import bo.org.siafco.app.core.data.TokenStore
import bo.org.siafco.app.core.debug.MobileDiagnostics
import bo.org.siafco.app.core.network.ApiResult
import bo.org.siafco.app.data.remote.ApiEnvelope
import bo.org.siafco.app.data.remote.AffiliationRequestPayload
import bo.org.siafco.app.data.remote.LoginRequest
import bo.org.siafco.app.data.remote.ProfilePayload
import bo.org.siafco.app.data.remote.SiafcoApi
import bo.org.siafco.app.domain.AffiliationRequestSummary
import bo.org.siafco.app.domain.SessionProfile
import kotlinx.coroutines.flow.Flow
import retrofit2.Response
import java.io.IOException

class AuthRepository(
    private val api: SiafcoApi,
    private val tokenStore: TokenStore
) : AuthGateway {
    val token: Flow<String?> = tokenStore.token

    override suspend fun login(email: String, password: String): ApiResult<SessionProfile> = safeCall {
        val response = api.login(LoginRequest(email = email, password = password))
        if (response.isSuccessful) {
            val body = response.body()
            val payload = body?.data
            if (body?.success == true && payload != null) {
                tokenStore.saveToken(payload.accessToken)
                ApiResult.Success(payload.profile.toSessionProfile("login"))
            } else {
                ApiResult.UnknownError
            }
        } else {
            response.toHttpError()
        }
    }

    override suspend fun validateSession(): ApiResult<SessionProfile> = safeCall {
        if (tokenStore.getToken().isNullOrBlank()) {
            return@safeCall ApiResult.HttpError(401, null)
        }
        val response = api.me()
        if (response.isSuccessful) {
            val payload = response.body()?.data
            if (response.body()?.success == true && payload != null) {
                ApiResult.Success(payload.profile.toSessionProfile("me"))
            } else {
                ApiResult.UnknownError
            }
        } else {
            if (response.code() == 401) tokenStore.clearToken()
            response.toHttpError()
        }
    }

    override suspend fun affiliationRequest(): ApiResult<AffiliationRequestSummary> = safeCall {
        val response = api.affiliationRequest()
        if (response.isSuccessful) {
            val request = response.body()?.data?.affiliationRequest
            MobileDiagnostics.home(
                "affiliation-request.dto",
                "status=${request?.status.orEmpty()} payment_status=${request?.payment?.status.orEmpty()} canSubmitPayment=${request?.capabilities?.canSubmitPayment == true} canViewCredential=${request?.capabilities?.canViewCredential == true}"
            )
            if (response.body()?.success == true && request?.requestCode != null) {
                val mapped = request.toDomain()
                MobileDiagnostics.home(
                    "affiliation-request.mapper",
                    "status=${mapped.status} payment_status=${mapped.paymentStatus.orEmpty()} canSubmitPayment=${mapped.canSubmitPayment} canViewCredential=${mapped.canViewCredential}"
                )
                ApiResult.Success(mapped)
            } else {
                ApiResult.UnknownError
            }
        } else {
            response.toHttpError()
        }
    }

    override suspend fun logout(): ApiResult<Unit> = safeCall {
        val response = api.logout()
        tokenStore.clearToken()
        if (response.isSuccessful) {
            ApiResult.Success(Unit)
        } else {
            response.toHttpError()
        }
    }

    override suspend fun clearLocalSession() {
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

    private fun bo.org.siafco.app.data.remote.MobileAffiliationRequestDto.toDomain(): AffiliationRequestSummary {
        return AffiliationRequestSummary(
            requestCode = requestCode.orEmpty(),
            status = status.orEmpty(),
            statusLabel = statusLabel ?: status.orEmpty(),
            statusDescription = statusDescription,
            planName = plan?.name,
            planType = plan?.type,
            planPaymentInstructions = plan?.paymentInstructions,
            amountDue = amountDue,
            currency = currency,
            observations = observations,
            paymentStatus = payment?.status,
            paymentStatusLabel = payment?.statusLabel,
            transactionNumber = payment?.transactionNumber,
            paymentDate = payment?.paymentDate,
            paidAmount = payment?.paidAmount,
            submittedAt = payment?.submittedAt,
            rejectionReason = payment?.rejectionReason,
            hasReceipt = payment?.hasReceipt == true,
            paymentBank = paymentInstructions?.bank,
            paymentHolder = paymentInstructions?.holder,
            paymentAccount = paymentInstructions?.account,
            paymentInstructions = paymentInstructions?.instructions,
            canSubmitPayment = capabilities?.canSubmitPayment == true,
            canLogin = capabilities?.canLogin == true,
            canViewCredential = capabilities?.canViewCredential == true
        )
    }
}

interface AuthGateway {
    suspend fun login(email: String, password: String): ApiResult<SessionProfile>
    suspend fun validateSession(): ApiResult<SessionProfile>
    suspend fun affiliationRequest(): ApiResult<AffiliationRequestSummary>
    suspend fun logout(): ApiResult<Unit>
    suspend fun clearLocalSession()
}
