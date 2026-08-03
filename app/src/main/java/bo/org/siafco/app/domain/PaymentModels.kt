package bo.org.siafco.app.domain

import bo.org.siafco.app.core.text.HumanTextInputNormalizer
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest
import java.time.LocalDate
import java.util.Locale

private const val RECEIPT_MAX_BYTES = 6144L * 1024L

data class PreparedReceipt(
    val file: File,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val sha256: String,
    val canPreviewImage: Boolean
)

data class PaymentForm(
    val transactionNumber: String = "",
    val paymentDate: String = "",
    val paidAmount: String = "",
    val payerName: String = "",
    val bankName: String = "",
    val receipt: PreparedReceipt? = null
)

data class NormalizedPaymentPayload(
    val transactionNumber: String,
    val paymentDate: String,
    val paidAmount: String,
    val payerName: String,
    val bankName: String,
    val receiptSha256: String
) {
    val signature: String
        get() = sha256(
            listOf(
                transactionNumber.squishUpper(),
                paymentDate,
                paidAmount,
                payerName.squishUpper(),
                bankName.squishUpper(),
                receiptSha256
            ).joinToString("|")
        )
}

data class PaymentValidationResult(
    val payload: NormalizedPaymentPayload? = null,
    val errors: Map<String, String> = emptyMap()
) {
    val isValid: Boolean = errors.isEmpty() && payload != null
}

object PaymentValidator {
    private val amountPattern = Regex("""^\d{1,8}([,.]\d{1,2})?$""")
    private val allowedReceiptTypes = setOf("image/jpeg", "image/png", "image/webp", "application/pdf")

    fun validate(form: PaymentForm, today: LocalDate = LocalDate.now()): PaymentValidationResult {
        val errors = linkedMapOf<String, String>()
        val transactionNumber = form.transactionNumber.trim()
        val paymentDate = form.paymentDate.trim()
        val payerName = HumanTextInputNormalizer.forSubmit(form.payerName)
        val bankName = HumanTextInputNormalizer.forSubmit(form.bankName)
        val receipt = form.receipt

        if (transactionNumber.isBlank()) {
            errors["transaction_number"] = "Ingresa el numero de transaccion."
        } else if (transactionNumber.length > 120) {
            errors["transaction_number"] = "El numero de transaccion no debe exceder 120 caracteres."
        }

        val normalizedAmount = normalizeAmount(form.paidAmount)
        if (normalizedAmount == null) {
            errors["paid_amount"] = "Ingresa un monto valido con maximo dos decimales."
        } else if (normalizedAmount <= BigDecimal.ZERO) {
            errors["paid_amount"] = "El monto debe ser mayor a cero."
        }

        val parsedDate = runCatching { LocalDate.parse(paymentDate) }.getOrNull()
        if (parsedDate == null) {
            errors["payment_date"] = "Ingresa una fecha valida."
        } else if (parsedDate.isAfter(today)) {
            errors["payment_date"] = "La fecha de pago no puede ser futura."
        }

        if (payerName.isBlank()) {
            errors["payer_name"] = "Ingresa el nombre de quien realizo el pago."
        } else if (payerName.length > 255) {
            errors["payer_name"] = "El nombre no debe exceder 255 caracteres."
        }
        if (bankName.length > 120) {
            errors["bank_name"] = "El banco no debe exceder 120 caracteres."
        }

        if (receipt == null) {
            errors["receipt"] = "Selecciona el comprobante de pago."
        } else {
            if (receipt.mimeType !in allowedReceiptTypes) {
                errors["receipt"] = "El comprobante debe ser JPG, PNG, WEBP o PDF."
            } else if (receipt.sizeBytes <= 0L || receipt.sizeBytes > RECEIPT_MAX_BYTES) {
                errors["receipt"] = "El comprobante no debe superar 6144 KB."
            }
        }

        val payload = if (errors.isEmpty() && receipt != null && normalizedAmount != null) {
            NormalizedPaymentPayload(
                transactionNumber = transactionNumber,
                paymentDate = paymentDate,
                paidAmount = normalizedAmount.setScale(2, RoundingMode.UNNECESSARY).toPlainString(),
                payerName = payerName,
                bankName = bankName,
                receiptSha256 = receipt.sha256
            )
        } else {
            null
        }

        return PaymentValidationResult(payload = payload, errors = errors)
    }

    fun normalizeAmount(input: String): BigDecimal? {
        val trimmed = input.trim()
        if (!amountPattern.matches(trimmed)) return null
        val amount = runCatching { BigDecimal(trimmed.replace(',', '.')).setScale(2, RoundingMode.UNNECESSARY) }
            .getOrNull() ?: return null
        return amount.takeIf { it > BigDecimal.ZERO }
    }
}

fun String.squishUpper(): String = trim()
    .replace(Regex("""\s+"""), " ")
    .uppercase(Locale.ROOT)

fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray())
    .joinToString("") { "%02x".format(it) }

fun File.sha256(): String = inputStream().use { input ->
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        if (read > 0) digest.update(buffer, 0, read)
    }
    digest.digest().joinToString("") { "%02x".format(it) }
}
