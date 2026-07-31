package bo.org.siafco.app.data.repository

import android.os.Build
import bo.org.siafco.app.core.data.TokenStore
import bo.org.siafco.app.data.remote.AffiliationRegistrationPayload
import bo.org.siafco.app.data.remote.ApiEnvelope
import bo.org.siafco.app.data.remote.CatalogsPayload
import bo.org.siafco.app.data.remote.SiafcoApi
import bo.org.siafco.app.domain.AffiliationCatalogs
import bo.org.siafco.app.domain.AffiliationRegistrationForm
import bo.org.siafco.app.domain.AffiliationRegistrationSuccess
import bo.org.siafco.app.domain.AccessLevel
import bo.org.siafco.app.domain.CatalogInstitution
import bo.org.siafco.app.domain.CatalogOption
import bo.org.siafco.app.domain.CatalogPlan
import bo.org.siafco.app.domain.CatalogSector
import bo.org.siafco.app.domain.SessionProfile
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.IOException

class AffiliationRepository(
    private val api: SiafcoApi,
    private val tokenStore: TokenStore
) : AffiliationRegistrationGateway {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun catalogs(): AffiliationRepositoryResult<AffiliationCatalogs> {
        return safeCall {
            val response = api.catalogs()
            if (response.isSuccessful) {
                response.body()?.data?.let { AffiliationRepositoryResult.Success(it.toDomain()) }
                    ?: AffiliationRepositoryResult.UnknownError
            } else {
                response.toError()
            }
        }
    }

    override suspend fun register(form: AffiliationRegistrationForm): AffiliationRepositoryResult<AffiliationRegistrationSuccess> {
        return safeCall {
            val photo = form.photo?.file ?: return@safeCall AffiliationRepositoryResult.ValidationError(
                mapOf("photo" to listOf("Selecciona una fotografia."))
            )
            val parts = form.toMultipartParts(photo.name)
            val response = api.storeAffiliationRequest(
                parts = parts,
                deviceName = deviceName().toTextRequestBody()
            )
            if (response.isSuccessful) {
                val payload = response.body()?.data
                val token = payload?.accessToken
                val profile = payload?.profile
                if (!token.isNullOrBlank() && profile != null) {
                    tokenStore.saveToken(token)
                    AffiliationRepositoryResult.Success(
                        AffiliationRegistrationSuccess(
                            profile = profile.toDomain().copy(
                                requestCode = payload.affiliationRequest?.requestCode
                            ),
                            requestCode = payload.affiliationRequest?.requestCode
                        )
                    )
                } else {
                    AffiliationRepositoryResult.UnknownError
                }
            } else {
                response.toError()
            }
        }
    }

    private fun AffiliationRegistrationForm.toMultipartParts(photoName: String): List<MultipartBody.Part> {
        val values = linkedMapOf(
            "full_name" to fullName,
            "ci" to ci,
            "ci_complement" to ciComplement,
            "issued_in" to issuedIn,
            "birth_date" to birthDate,
            "marital_status" to maritalStatus,
            "phone" to phone,
            "email" to email,
            "password" to password,
            "password_confirmation" to passwordConfirmation,
            "address" to address,
            "sector_id" to sectorId.toString(),
            "affiliation_plan_id" to planId.toString(),
            "regional" to regional,
            "institution" to institution,
            "position" to position,
            "terms_accepted" to if (termsAccepted) "1" else "0",
            "privacy_accepted" to if (privacyAccepted) "1" else "0"
        )
        val textParts = values
            .filterNot { (key, value) -> key == "ci_complement" && value.isBlank() }
            .map { (key, value) -> MultipartBody.Part.createFormData(key, value) }
        val photoPart = MultipartBody.Part.createFormData(
            "photo",
            photoName,
            photo!!.file.asRequestBody("image/jpeg".toMediaType())
        )
        return textParts + photoPart
    }

    private fun deviceName(): String {
        return "SIAFCO Android ${Build.MODEL}".take(120)
    }

    private suspend fun <T> safeCall(block: suspend () -> AffiliationRepositoryResult<T>): AffiliationRepositoryResult<T> {
        return try {
            block()
        } catch (_: IOException) {
            AffiliationRepositoryResult.NetworkError
        } catch (_: RuntimeException) {
            AffiliationRepositoryResult.UnknownError
        }
    }

    private fun <T> Response<ApiEnvelope<T>>.toError(): AffiliationRepositoryResult.Error {
        val parsed = runCatching {
            errorBody()?.string()?.let { json.decodeFromString<ApiEnvelope<Unit>>(it) }
        }.getOrNull()
        val errors = parsed?.errors.orEmpty()
        return when (code()) {
            409 -> AffiliationRepositoryResult.Conflict(parsed?.message ?: "Ya existe una cuenta o solicitud asociada.")
            422 -> AffiliationRepositoryResult.ValidationError(errors)
            429 -> AffiliationRepositoryResult.RateLimited
            else -> AffiliationRepositoryResult.HttpError(code(), parsed?.message ?: message())
        }
    }

    private fun String.toTextRequestBody() = toRequestBody("text/plain".toMediaType())

    private fun CatalogsPayload.toDomain() = AffiliationCatalogs(
        sectors = sectors.map { CatalogSector(it.id, it.name, it.code, it.regional, it.institution) },
        plans = plans.map {
            CatalogPlan(
                id = it.id,
                sectorId = it.sectorId,
                name = it.name,
                type = it.type,
                currency = it.currency,
                affiliationFee = it.affiliationFee,
                credentialFee = it.credentialFee,
                totalAmount = it.totalAmount,
                description = it.description,
                paymentInstructions = it.paymentInstructions
            )
        },
        regionals = regionals,
        issuedIn = issuedIn.map { CatalogOption(it.value, it.label) },
        maritalStatuses = maritalStatuses,
        institution = CatalogInstitution(institution.name, institution.termsVersion, institution.privacyVersion)
    )

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

interface AffiliationRegistrationGateway {
    suspend fun catalogs(): AffiliationRepositoryResult<AffiliationCatalogs>
    suspend fun register(form: AffiliationRegistrationForm): AffiliationRepositoryResult<AffiliationRegistrationSuccess>
}

sealed interface AffiliationRepositoryResult<out T> {
    data class Success<T>(val value: T) : AffiliationRepositoryResult<T>
    data class HttpError(val code: Int, val message: String?) : Error
    data class ValidationError(val errors: Map<String, List<String>>) : Error
    data class Conflict(val message: String) : Error
    data object RateLimited : Error
    data object NetworkError : Error
    data object UnknownError : Error

    sealed interface Error : AffiliationRepositoryResult<Nothing>
}
