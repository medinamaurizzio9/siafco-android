package bo.org.siafco.app.core.network

import bo.org.siafco.app.BuildConfig
import java.net.URI

object UrlResolver {
    fun resolve(url: String?): String? {
        if (url.isNullOrBlank()) return null
        if (url.startsWith("/")) return absoluteFromBase(url)

        return runCatching {
            val uri = URI(url)
            val host = uri.host?.lowercase()
            if (BuildConfig.DEBUG && (host == "127.0.0.1" || host == "localhost")) {
                URI(
                    uri.scheme,
                    uri.userInfo,
                    "10.0.2.2",
                    uri.port,
                    uri.path,
                    uri.query,
                    uri.fragment
                ).toString()
            } else {
                url
            }
        }.getOrDefault(url)
    }

    fun absoluteFromBase(path: String): String {
        val base = BuildConfig.BASE_URL.substringBefore("/api/mobile/v1/")
            .trimEnd('/')
        return base + "/" + path.trimStart('/')
    }
}
