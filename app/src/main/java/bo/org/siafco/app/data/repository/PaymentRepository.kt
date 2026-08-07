package bo.org.siafco.app.data.repository

import bo.org.siafco.app.data.remote.ApiEnvelope
import bo.org.siafco.app.data.remote.MobileAffiliationRequestDto
import bo.org.siafco.app.data.remote.SiafcoApi
import bo.org.siafco.app.core.network.UrlResolver
import bo.org.siafco.app.domain.AffiliationRequestSummary
import bo.org.siafco.app.domain.NormalizedPaymentPayload
import bo.org.siafco.app.domain.PreparedReceipt
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.IOException

class PaymentRepository(private val api: SiafcoApi) : PaymentGateway {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun submit(
        idempotencyKey: String,
        payload: NormalizedPaymentPayload,
        receipt: PreparedReceipt
    ): PaymentRepositoryResult {
        return try {
            val response = api.submitAffiliationPayment(
                idempotencyKey = idempotencyKey,
                parts = payload.toParts(receipt)
            )
            response.toResult()
        } catch (_: IOException) {
            PaymentRepositoryResult.NetworkError
        } catch (_: RuntimeException) {
            PaymentRepositoryResult.UnknownError
        }
    }

    private fun NormalizedPaymentPayload.toParts(receipt: PreparedReceipt): List<MultipartBody.Part> {
        return buildList {
            addText("transaction_number", transactionNumber)
            addText("payment_date", paymentDate)
            addText("paid_amount", paidAmount)
            addText("payer_name", payerName)
            addText("bank_name", bankName)
            add(
                MultipartBody.Part.createFormData(
                    name = "receipt",
                    filename = safeReceiptName(receipt),
                    body = receipt.file.asRequestBody(receipt.mimeType.toMediaType())
                )
            )
        }
    }

    private fun MutableList<MultipartBody.Part>.addText(name: String, value: String) {
        add(MultipartBody.Part.createFormData(name, null, value.toRequestBody("text/plain".toMediaType())))
    }

    private fun safeReceiptName(receipt: PreparedReceipt): String {
        val extension = when (receipt.mimeType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "application/pdf" -> "pdf"
            else -> "bin"
        }
        return "receipt-${receipt.sha256.take(12)}.$extension"
    }

    private fun Response<ApiEnvelope<bo.org.siafco.app.data.remote.PaymentSubmissionPayload>>.toResult(): PaymentRepositoryResult {
        if (isSuccessful) {
            val request = body()?.data?.affiliationRequest
            return if (body()?.success == true && request?.requestCode != null) {
                PaymentRepositoryResult.Success(request.toDomain())
            } else {
                PaymentRepositoryResult.UnknownError
            }
        }

        val error = parseError()
        return when (code()) {
            401 -> PaymentRepositoryResult.Unauthorized
            403 -> PaymentRepositoryResult.Forbidden(error?.message)
            409 -> PaymentRepositoryResult.Conflict(error?.message)
            422 -> PaymentRepositoryResult.ValidationError(error?.errors.orEmpty())
            429 -> PaymentRepositoryResult.RateLimited(error?.message)
            else -> PaymentRepositoryResult.HttpError(code(), error?.message)
        }
    }

    private fun Response<*>.parseError(): ApiEnvelope<Unit>? {
        val raw = errorBody()?.string().orEmpty()
        if (raw.isBlank()) return null
        return runCatching { json.decodeFromString<ApiEnvelope<Unit>>(raw) }.getOrNull()
    }

    private fun MobileAffiliationRequestDto.toDomain(): AffiliationRequestSummary {
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
            paymentQrUrl = paymentInstructions?.qrUrl?.let(UrlResolver::resolve),
            supportPhone = paymentInstructions?.supportPhone,
            canSubmitPayment = capabilities?.canSubmitPayment == true,
            canLogin = capabilities?.canLogin == true,
            canViewCredential = capabilities?.canViewCredential == true
        )
    }
}

interface PaymentGateway {
    suspend fun submit(
        idempotencyKey: String,
        payload: NormalizedPaymentPayload,
        receipt: PreparedReceipt
    ): PaymentRepositoryResult
}

sealed interface PaymentRepositoryResult {
    data class Success(val request: AffiliationRequestSummary) : PaymentRepositoryResult
    data class ValidationError(val errors: Map<String, List<String>>) : PaymentRepositoryResult
    data class Conflict(val message: String?) : PaymentRepositoryResult
    data class Forbidden(val message: String?) : PaymentRepositoryResult
    data class RateLimited(val message: String?) : PaymentRepositoryResult
    data class HttpError(val code: Int, val message: String?) : PaymentRepositoryResult
    data object Unauthorized : PaymentRepositoryResult
    data object NetworkError : PaymentRepositoryResult
    data object UnknownError : PaymentRepositoryResult
}
