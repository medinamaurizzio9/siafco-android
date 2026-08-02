package bo.org.siafco.app

import bo.org.siafco.app.core.network.UrlResolver
import org.junit.Assert.assertEquals
import org.junit.Test

class UrlResolverTest {
    @Test
    fun localhostUrlsAreAdaptedForDebugEmulator() {
        assertEquals(
            "http://10.0.2.2:8000/storage/affiliates/photo.jpg",
            UrlResolver.resolve("http://127.0.0.1:8000/storage/affiliates/photo.jpg")
        )
        assertEquals(
            "http://10.0.2.2:8000/storage/affiliates/photo.jpg",
            UrlResolver.resolve("http://localhost:8000/storage/affiliates/photo.jpg")
        )
    }

    @Test
    fun localhostVerificationUrlsKeepPortPathQueryAndFragment() {
        assertEquals(
            "http://10.0.2.2:8000/verificar/abc?source=mobile#qr",
            UrlResolver.resolve("http://127.0.0.1:8000/verificar/abc?source=mobile#qr")
        )
        assertEquals(
            "http://10.0.2.2:8000/verificar/abc?source=mobile#qr",
            UrlResolver.resolve("http://localhost:8000/verificar/abc?source=mobile#qr")
        )
    }

    @Test
    fun nonLocalDomainsAreNotChanged() {
        assertEquals(
            "https://siafco.example.bo/storage/affiliates/photo.jpg",
            UrlResolver.resolve("https://siafco.example.bo/storage/affiliates/photo.jpg")
        )
        assertEquals(
            "http://example.test/verificar/abc?source=mobile#qr",
            UrlResolver.resolve("http://example.test/verificar/abc?source=mobile#qr")
        )
    }
}
