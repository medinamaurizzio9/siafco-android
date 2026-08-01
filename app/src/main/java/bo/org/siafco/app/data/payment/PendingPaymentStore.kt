package bo.org.siafco.app.data.payment

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import bo.org.siafco.app.domain.PaymentForm
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface PendingPaymentStore {
    suspend fun save(idempotencyKey: String, payloadSignature: String, form: PaymentForm)
    suspend fun clear()
}

class EncryptedPendingPaymentStore(context: Context) : PendingPaymentStore {
    private val directory = File(context.noBackupFilesDir, "payment-draft")
    private val metadataFile = File(directory, "draft.json.enc")
    private val receiptFile = File(directory, "receipt.bin.enc")
    private val json = Json { explicitNulls = false }
    private val cipher = DraftCipher()

    override suspend fun save(idempotencyKey: String, payloadSignature: String, form: PaymentForm) {
        val receipt = form.receipt ?: return
        directory.mkdirs()
        val metadata = PendingPaymentMetadata(
            idempotencyKey = idempotencyKey,
            payloadSignature = payloadSignature,
            transactionNumber = form.transactionNumber,
            paymentDate = form.paymentDate,
            paidAmount = form.paidAmount,
            payerName = form.payerName,
            bankName = form.bankName,
            receiptDisplayName = receipt.displayName,
            receiptMimeType = receipt.mimeType,
            receiptSha256 = receipt.sha256
        )
        metadataFile.writeBytes(cipher.encrypt(json.encodeToString(metadata).toByteArray()))
        receiptFile.writeBytes(cipher.encrypt(receipt.file.readBytes()))
    }

    override suspend fun clear() {
        metadataFile.delete()
        receiptFile.delete()
        directory.delete()
    }
}

@Serializable
private data class PendingPaymentMetadata(
    val idempotencyKey: String,
    val payloadSignature: String,
    val transactionNumber: String,
    val paymentDate: String,
    val paidAmount: String,
    val payerName: String,
    val bankName: String,
    val receiptDisplayName: String,
    val receiptMimeType: String,
    val receiptSha256: String
)

private class DraftCipher {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    fun encrypt(plain: ByteArray): ByteArray {
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(), GCMParameterSpec(128, iv))
        return iv + cipher.doFinal(plain)
    }

    private fun key(): SecretKey {
        val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        private const val KEY_ALIAS = "siafco_pending_payment"
    }
}
