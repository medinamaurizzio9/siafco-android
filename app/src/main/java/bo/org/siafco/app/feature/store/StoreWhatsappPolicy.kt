package bo.org.siafco.app.feature.store

import java.net.URI

object StoreWhatsappPolicy {
    private const val MAX_URL_LENGTH = 2_048
    private val blockedSchemes = setOf("file", "content", "javascript", "data", "intent")

    fun validate(url: String): String? {
        if (url.length > MAX_URL_LENGTH) return null
        val parsed = runCatching { URI(url) }.getOrNull() ?: return null
        val scheme = parsed.scheme?.lowercase() ?: return null
        val host = parsed.host?.lowercase() ?: return null
        if (scheme in blockedSchemes) return null
        if (scheme != "https") return null
        if (host != "wa.me") return null
        if (!parsed.userInfo.isNullOrBlank()) return null
        if (parsed.port != -1) return null
        return url
    }
}
