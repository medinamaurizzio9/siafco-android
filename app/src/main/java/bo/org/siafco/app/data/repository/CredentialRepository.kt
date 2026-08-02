package bo.org.siafco.app.data.repository

import bo.org.siafco.app.BuildConfig
import bo.org.siafco.app.core.data.TokenStore
import bo.org.siafco.app.core.network.UrlResolver
import bo.org.siafco.app.data.remote.MobileCredentialDto
import bo.org.siafco.app.data.remote.SiafcoApi
import java.io.IOException
import java.net.URI
import java.util.Base64

private const val QR_DATA_URI_PREFIX = "data:image/png;base64,"
private const val MAX_QR_BASE64_CHARS = 700_000
private const val MAX_QR_BYTES = 512_000

class CredentialRepository(
    private val api: SiafcoApi,
    private val tokenStore: TokenStore
) : CredentialGateway {
    override suspend fun loadCredential(): CredentialResult {
        return try {
            val response = api.credential()
            if (response.isSuccessful) {
                val body = response.body()
                val dto = body?.data?.credential
                if (body?.success == true && dto != null) {
                    dto.toDomain()?.let(CredentialResult::Success) ?: CredentialResult.InvalidPayload
                } else {
                    CredentialResult.InvalidPayload
                }
            } else {
                if (response.code() == 401) tokenStore.clearToken()
                when (response.code()) {
                    401 -> CredentialResult.Unauthorized
                    403 -> CredentialResult.Forbidden
                    404 -> CredentialResult.NotFound
                    429 -> CredentialResult.RateLimited
                    else -> CredentialResult.HttpError(response.code())
                }
            }
        } catch (_: IOException) {
            CredentialResult.NetworkError
        } catch (_: RuntimeException) {
            CredentialResult.InvalidPayload
        }
    }

    private fun MobileCredentialDto.toDomain(): MobileCredential? {
        val resolvedUrl = UrlResolver.resolve(verificationUrl)
        val verifiedUrl = CredentialUrlPolicy.validate(
            url = resolvedUrl,
            isDebug = BuildConfig.DEBUG
        ) ?: return null
        val resolvedPhotoUrl = UrlResolver.resolve(photoUrl)
        val safePhotoUrl = CredentialUrlPolicy.validate(
            url = resolvedPhotoUrl,
            isDebug = BuildConfig.DEBUG
        )
        val qrBytes = CredentialQrDecoder.decode(qrImage) ?: return null

        return MobileCredential(
            institutionName = institutionName.orEmpty(),
            affiliateName = affiliateName.orEmpty(),
            registrationNumber = registrationNumber.orEmpty(),
            sector = sector.orEmpty(),
            regional = regional.orEmpty(),
            status = status.orEmpty(),
            statusLabel = statusLabel.orEmpty(),
            issuedAt = issuedAt.orEmpty(),
            photoUrl = safePhotoUrl,
            verificationUrl = verifiedUrl,
            qrPngBytes = qrBytes
        )
    }
}

interface CredentialGateway {
    suspend fun loadCredential(): CredentialResult
}

data class MobileCredential(
    val institutionName: String,
    val affiliateName: String,
    val registrationNumber: String,
    val sector: String,
    val regional: String,
    val status: String,
    val statusLabel: String,
    val issuedAt: String,
    val photoUrl: String?,
    val verificationUrl: String,
    val qrPngBytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MobileCredential
        return institutionName == other.institutionName &&
            affiliateName == other.affiliateName &&
            registrationNumber == other.registrationNumber &&
            sector == other.sector &&
            regional == other.regional &&
            status == other.status &&
            statusLabel == other.statusLabel &&
            issuedAt == other.issuedAt &&
            photoUrl == other.photoUrl &&
            verificationUrl == other.verificationUrl &&
            qrPngBytes.contentEquals(other.qrPngBytes)
    }

    override fun hashCode(): Int {
        var result = institutionName.hashCode()
        result = 31 * result + affiliateName.hashCode()
        result = 31 * result + registrationNumber.hashCode()
        result = 31 * result + sector.hashCode()
        result = 31 * result + regional.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + statusLabel.hashCode()
        result = 31 * result + issuedAt.hashCode()
        result = 31 * result + (photoUrl?.hashCode() ?: 0)
        result = 31 * result + verificationUrl.hashCode()
        result = 31 * result + qrPngBytes.contentHashCode()
        return result
    }
}

sealed interface CredentialResult {
    data class Success(val credential: MobileCredential) : CredentialResult
    data class HttpError(val code: Int) : CredentialResult
    data object Unauthorized : CredentialResult
    data object Forbidden : CredentialResult
    data object NotFound : CredentialResult
    data object RateLimited : CredentialResult
    data object NetworkError : CredentialResult
    data object InvalidPayload : CredentialResult
}

object CredentialQrDecoder {
    fun decode(dataUri: String?): ByteArray? {
        if (dataUri.isNullOrBlank()) return null
        if (!dataUri.startsWith(QR_DATA_URI_PREFIX)) return null

        val encoded = dataUri.removePrefix(QR_DATA_URI_PREFIX)
        if (encoded.isBlank() || encoded.length > MAX_QR_BASE64_CHARS) return null

        val bytes = runCatching { Base64.getDecoder().decode(encoded) }.getOrNull() ?: return null
        if (bytes.isEmpty() || bytes.size > MAX_QR_BYTES) return null
        if (!bytes.hasPngSignature()) return null

        return bytes
    }

    private fun ByteArray.hasPngSignature(): Boolean {
        val signature = byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)
        return size >= signature.size && signature.indices.all { this[it] == signature[it] }
    }
}

object CredentialUrlPolicy {
    private val localDebugHosts = setOf("10.0.2.2", "127.0.0.1", "localhost")

    fun validate(url: String?, isDebug: Boolean): String? {
        if (url.isNullOrBlank()) return null
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        val host = uri.host?.lowercase() ?: return null

        if (scheme == "https") return uri.toString()
        if (isDebug && scheme == "http" && host in localDebugHosts) return uri.toString()

        return null
    }
}
