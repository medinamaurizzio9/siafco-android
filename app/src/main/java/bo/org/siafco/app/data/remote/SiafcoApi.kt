package bo.org.siafco.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import okhttp3.MultipartBody
import okhttp3.RequestBody

interface SiafcoApi {
    @GET("catalogs")
    suspend fun catalogs(): Response<ApiEnvelope<CatalogsPayload>>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiEnvelope<LoginPayload>>

    @Multipart
    @POST("affiliation-requests")
    suspend fun storeAffiliationRequest(
        @Part parts: List<MultipartBody.Part>,
        @Part("device_name") deviceName: RequestBody
    ): Response<ApiEnvelope<AffiliationRegistrationPayload>>

    @GET("me")
    suspend fun me(): Response<ApiEnvelope<ProfilePayload>>

    @PATCH("me/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<ApiEnvelope<ProfilePayload>>

    @Multipart
    @POST("me/profile/photo")
    suspend fun updateProfilePhoto(@Part photo: MultipartBody.Part): Response<ApiEnvelope<ProfilePayload>>

    @PATCH("me/password")
    suspend fun updatePassword(@Body request: UpdatePasswordRequest): Response<ApiEnvelope<ProfilePayload>>

    @GET("me/affiliation-request")
    suspend fun affiliationRequest(): Response<ApiEnvelope<AffiliationRequestPayload>>

    @Multipart
    @POST("me/affiliation-request/payment")
    suspend fun submitAffiliationPayment(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Part parts: List<MultipartBody.Part>
    ): Response<ApiEnvelope<PaymentSubmissionPayload>>

    @POST("auth/logout")
    suspend fun logout(): Response<ApiEnvelope<EmptyPayload>>

    @POST("auth/logout-all")
    suspend fun logoutAll(): Response<ApiEnvelope<EmptyPayload>>
}
