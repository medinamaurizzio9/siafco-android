package bo.org.siafco.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface SiafcoApi {
    @GET("catalogs")
    suspend fun catalogs(): Response<ApiEnvelope<CatalogsPayload>>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiEnvelope<LoginPayload>>

    @GET("me")
    suspend fun me(): Response<ApiEnvelope<ProfilePayload>>

    @POST("auth/logout")
    suspend fun logout(): Response<ApiEnvelope<EmptyPayload>>
}
