package bo.org.siafco.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
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

    @GET("me/credential")
    suspend fun credential(): Response<ApiEnvelope<CredentialPayload>>

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

    @GET("store")
    suspend fun storeCatalog(
        @Query("search") search: String? = null,
        @Query("category") category: String? = null,
        @Query("featured") featured: Boolean? = null,
        @Query("availability") availability: String? = null,
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null
    ): Response<ApiEnvelope<StoreCatalogPayload>>

    @GET("store/products/{publicCode}")
    suspend fun storeProduct(@Path("publicCode") publicCode: String): Response<ApiEnvelope<StoreProductPayload>>

    @POST("store/quote")
    suspend fun storeQuote(@Body request: StoreQuoteRequest): Response<ApiEnvelope<StoreQuotePayload>>

    @POST("store/orders")
    suspend fun createStoreOrder(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: StoreQuoteRequest
    ): Response<ApiEnvelope<StoreOrderPayload>>

    @GET("store/orders")
    suspend fun storeOrders(
        @Query("status") status: String? = null,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null,
        @Query("code") code: String? = null,
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null
    ): Response<ApiEnvelope<StoreOrdersPayload>>

    @GET("store/orders/{orderCode}")
    suspend fun storeOrder(@Path("orderCode") orderCode: String): Response<ApiEnvelope<StoreOrderPayload>>

    @Multipart
    @POST("store/orders/{orderCode}/receipt")
    suspend fun submitStoreReceipt(
        @Path("orderCode") orderCode: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Part receipt: MultipartBody.Part
    ): Response<ApiEnvelope<StoreOrderPayload>>

    @POST("store/orders/{orderCode}/whatsapp")
    suspend fun storeWhatsapp(@Path("orderCode") orderCode: String): Response<ApiEnvelope<StoreWhatsappPayload>>
}
