package bo.org.siafco.app.feature.payment

import java.net.URLEncoder

object PaymentSupportWhatsapp {
    fun normalizedBolivianNumber(phone: String?): String? {
        val raw = phone?.trim().orEmpty()
        if (raw.isBlank()) return null

        val compact = raw.replace(Regex("""[\s().-]+"""), "")
        val digits = compact.removePrefix("+")

        return when {
            digits.matches(Regex("""\d{8}""")) -> "591$digits"
            digits.matches(Regex("""591\d{8}""")) -> digits
            else -> null
        }
    }

    fun displayPhone(phone: String?): String? = normalizedBolivianNumber(phone)?.let { normalized ->
        "+${normalized.take(3)} ${normalized.drop(3).chunked(4).joinToString(" ")}"
    }

    fun url(phone: String?, requestCode: String): String? {
        val number = normalizedBolivianNumber(phone) ?: return null
        val message = """
            Hola SIAFCO. Acabo de enviar mi comprobante de pago.

            Ticket/Solicitud: $requestCode
            Estado: Pago en revisión.

            Quedo atento a la validación de mi afiliación. Gracias.
        """.trimIndent()

        return "https://wa.me/$number?text=${URLEncoder.encode(message, Charsets.UTF_8.name())}"
    }
}
