package bo.org.siafco.app.core.network

import bo.org.siafco.app.BuildConfig
import bo.org.siafco.app.core.data.TokenStore
import bo.org.siafco.app.data.remote.SiafcoApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun createApi(tokenStore: TokenStore): SiafcoApi {
        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val token = runBlocking { tokenStore.getToken() }
                val request = if (token.isNullOrBlank()) {
                    chain.request()
                } else {
                    chain.request().newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                }
                chain.proceed(request)
            }

        if (BuildConfig.ENABLE_NETWORK_LOGGING) {
            clientBuilder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    redactHeader("Authorization")
                    redactHeader("Idempotency-Key")
                    redactHeader("Cookie")
                    redactHeader("Set-Cookie")
                    level = HttpLoggingInterceptor.Level.HEADERS
                }
            )
        }

        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(clientBuilder.build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SiafcoApi::class.java)
    }
}
