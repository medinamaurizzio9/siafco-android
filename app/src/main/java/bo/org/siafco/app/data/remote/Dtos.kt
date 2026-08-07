package bo.org.siafco.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiEnvelope<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val errors: Map<String, List<String>> = emptyMap()
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    @SerialName("device_name") val deviceName: String = "Android"
)

@Serializable
data class LoginPayload(
    @SerialName("token_type") val tokenType: String,
    @SerialName("access_token") val accessToken: String,
    val profile: MobileProfileDto
)

@Serializable
data class AffiliationRegistrationPayload(
    @SerialName("token_type") val tokenType: String? = null,
    @SerialName("access_token") val accessToken: String? = null,
    val profile: MobileProfileDto? = null,
    @SerialName("affiliation_request") val affiliationRequest: AffiliationRequestDto? = null
)

@Serializable
data class AffiliationRequestDto(
    @SerialName("request_code") val requestCode: String? = null,
    val status: String? = null,
    @SerialName("status_label") val statusLabel: String? = null,
    @SerialName("amount_due") val amountDue: Double? = null
)

@Serializable
data class AffiliationRequestPayload(
    @SerialName("affiliation_request") val affiliationRequest: MobileAffiliationRequestDto? = null
)

@Serializable
data class PaymentSubmissionPayload(
    val idempotent: Boolean? = null,
    @SerialName("affiliation_request") val affiliationRequest: MobileAffiliationRequestDto? = null
)

@Serializable
data class CredentialPayload(
    val credential: MobileCredentialDto? = null
)

@Serializable
data class MobileCredentialDto(
    @SerialName("institution_name") val institutionName: String? = null,
    @SerialName("affiliate_name") val affiliateName: String? = null,
    @SerialName("registration_number") val registrationNumber: String? = null,
    val sector: String? = null,
    val regional: String? = null,
    val status: String? = null,
    @SerialName("status_label") val statusLabel: String? = null,
    @SerialName("issued_at") val issuedAt: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("verification_url") val verificationUrl: String? = null,
    @SerialName("qr_image") val qrImage: String? = null
)

@Serializable
data class MobileAffiliationRequestDto(
    @SerialName("request_code") val requestCode: String? = null,
    val status: String? = null,
    @SerialName("status_label") val statusLabel: String? = null,
    @SerialName("status_description") val statusDescription: String? = null,
    val observations: String? = null,
    @SerialName("amount_due") val amountDue: Double? = null,
    val currency: String? = null,
    val plan: MobileAffiliationRequestPlanDto? = null,
    val payment: MobileAffiliationPaymentDto? = null,
    @SerialName("payment_instructions") val paymentInstructions: MobilePaymentInstructionsDto? = null,
    val capabilities: MobileAffiliationCapabilitiesDto? = null
)

@Serializable
data class MobileAffiliationRequestPlanDto(
    val name: String? = null,
    val type: String? = null,
    @SerialName("affiliation_fee") val affiliationFee: Double? = null,
    @SerialName("credential_fee") val credentialFee: Double? = null,
    @SerialName("total_amount") val totalAmount: Double? = null,
    @SerialName("payment_instructions") val paymentInstructions: String? = null
)

@Serializable
data class MobileAffiliationPaymentDto(
    val status: String? = null,
    @SerialName("status_label") val statusLabel: String? = null,
    @SerialName("transaction_number") val transactionNumber: String? = null,
    @SerialName("payment_date") val paymentDate: String? = null,
    @SerialName("paid_amount") val paidAmount: Double? = null,
    @SerialName("submitted_at") val submittedAt: String? = null,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("has_receipt") val hasReceipt: Boolean? = null
)

@Serializable
data class MobilePaymentInstructionsDto(
    val bank: String? = null,
    val holder: String? = null,
    val account: String? = null,
    val instructions: String? = null,
    @SerialName("qr_url") val qrUrl: String? = null,
    @SerialName("support_phone") val supportPhone: String? = null
)

@Serializable
data class MobileAffiliationCapabilitiesDto(
    @SerialName("can_submit_payment") val canSubmitPayment: Boolean? = null,
    @SerialName("can_login") val canLogin: Boolean? = null,
    @SerialName("can_view_credential") val canViewCredential: Boolean? = null
)

@Serializable
data class ProfilePayload(
    val profile: MobileProfileDto
)

@Serializable
data class UpdateProfileRequest(
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    @SerialName("birth_date") val birthDate: String? = null,
    @SerialName("marital_status") val maritalStatus: String? = null
)

@Serializable
data class UpdatePasswordRequest(
    @SerialName("current_password") val currentPassword: String,
    val password: String,
    @SerialName("password_confirmation") val passwordConfirmation: String
)

@Serializable
data class CatalogsPayload(
    val sectors: List<SectorDto>,
    val plans: List<AffiliationPlanDto>,
    val regionals: List<String>,
    @SerialName("issued_in") val issuedIn: List<IssuedInDto>,
    @SerialName("marital_statuses") val maritalStatuses: List<String>,
    val institution: InstitutionDto
)

@Serializable
data class SectorDto(
    val id: Long,
    val name: String,
    val code: String? = null,
    val regional: String? = null,
    val institution: String? = null
)

@Serializable
data class AffiliationPlanDto(
    val id: Long,
    @SerialName("sector_id") val sectorId: Long? = null,
    val name: String,
    val type: String? = null,
    val currency: String,
    @SerialName("affiliation_fee") val affiliationFee: Double,
    @SerialName("credential_fee") val credentialFee: Double,
    @SerialName("total_amount") val totalAmount: Double,
    val description: String? = null,
    @SerialName("payment_instructions") val paymentInstructions: String? = null
)

@Serializable
data class IssuedInDto(
    val value: String,
    val label: String
)

@Serializable
data class InstitutionDto(
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    @SerialName("payment_bank") val paymentBank: String? = null,
    @SerialName("payment_holder") val paymentHolder: String? = null,
    @SerialName("payment_account") val paymentAccount: String? = null,
    @SerialName("payment_instructions") val paymentInstructions: String? = null,
    @SerialName("terms_version") val termsVersion: String,
    @SerialName("privacy_version") val privacyVersion: String
)

@Serializable
class EmptyPayload

@Serializable
data class MobileProfileDto(
    val user: UserDto,
    val affiliate: AffiliateDto? = null,
    @SerialName("allowed_profile_fields") val allowedProfileFields: List<String> = emptyList()
)

@Serializable
data class UserDto(
    val name: String,
    val email: String,
    val role: String? = null,
    @SerialName("user_type") val userType: String? = null,
    @SerialName("must_change_password") val mustChangePassword: Boolean? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
    @SerialName("last_login_at") val lastLoginAt: String? = null
)

@Serializable
data class AffiliateDto(
    @SerialName("full_name") val fullName: String? = null,
    val ci: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    @SerialName("birth_date") val birthDate: String? = null,
    @SerialName("marital_status") val maritalStatus: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("registration_number") val registrationNumber: String? = null,
    val status: String? = null,
    @SerialName("status_label") val statusLabel: String? = null,
    @SerialName("status_description") val statusDescription: String? = null,
    @SerialName("access_level") val accessLevel: String? = null,
    val sector: AffiliateSectorDto? = null,
    val plan: AffiliatePlanDto? = null
)

@Serializable
data class AffiliateSectorDto(
    val name: String? = null,
    val code: String? = null,
    val regional: String? = null,
    val institution: String? = null
)

@Serializable
data class AffiliatePlanDto(
    val name: String? = null,
    val type: String? = null,
    val currency: String? = null,
    @SerialName("affiliation_fee") val affiliationFee: Double? = null,
    @SerialName("credential_fee") val credentialFee: Double? = null,
    @SerialName("total_amount") val totalAmount: Double? = null
)

@Serializable
data class StoreCatalogPayload(
    val settings: StoreSettingsDto,
    val featured: List<StoreProductDto> = emptyList(),
    val categories: List<StoreCategoryDto> = emptyList(),
    val products: List<StoreProductDto> = emptyList(),
    val pagination: StorePaginationDto
)

@Serializable
data class StoreProductPayload(val product: StoreProductDto)

@Serializable
data class StoreQuotePayload(val quote: StoreQuoteDto)

@Serializable
data class StoreOrderPayload(val order: StoreOrderDto, val receipt: StoreReceiptDto? = null)

@Serializable
data class StoreOrdersPayload(
    val orders: List<StoreOrderDto> = emptyList(),
    val pagination: StorePaginationDto
)

@Serializable
data class StoreWhatsappPayload(val whatsapp: StoreWhatsappDto)

@Serializable
data class StoreSettingsDto(
    val currency: String = "BOB",
    @SerialName("pickup_enabled") val pickupEnabled: Boolean = false,
    @SerialName("shipping_enabled") val shippingEnabled: Boolean = false,
    @SerialName("pickup_instructions") val pickupInstructions: String? = null,
    @SerialName("shipping_instructions") val shippingInstructions: String? = null,
    val payment: StorePaymentSettingsDto? = null,
    @SerialName("whatsapp_enabled") val whatsappEnabled: Boolean = false
)

@Serializable
data class StorePaymentSettingsDto(
    @SerialName("qr_url") val qrUrl: String? = null,
    val bank: String? = null,
    val holder: String? = null,
    val account: String? = null,
    val instructions: String? = null
)

@Serializable
data class StoreCategoryDto(val slug: String, val name: String)

@Serializable
data class StoreProductDto(
    @SerialName("public_code") val publicCode: String,
    val slug: String? = null,
    val sku: String? = null,
    val name: String,
    @SerialName("short_description") val shortDescription: String? = null,
    val description: String? = null,
    @SerialName("regular_price") val regularPrice: String,
    @SerialName("affiliate_price") val affiliatePrice: String,
    @SerialName("effective_price") val effectivePrice: String,
    @SerialName("promo_price") val promoPrice: String? = null,
    val currency: String = "BOB",
    @SerialName("availability_status") val availabilityStatus: String,
    @SerialName("delivery_modes") val deliveryModes: List<String> = emptyList(),
    val featured: Boolean = false,
    @SerialName("max_quantity_per_order") val maxQuantityPerOrder: Int = 1,
    @SerialName("primary_image_url") val primaryImageUrl: String? = null,
    val category: StoreCategoryDto? = null,
    val capabilities: StoreProductCapabilitiesDto? = null,
    val images: List<StoreImageDto> = emptyList(),
    val variants: List<StoreVariantDto> = emptyList()
)

@Serializable
data class StoreProductCapabilitiesDto(@SerialName("can_order") val canOrder: Boolean = false)

@Serializable
data class StoreImageDto(
    val url: String? = null,
    val alt: String? = null,
    @SerialName("is_primary") val isPrimary: Boolean = false
)

@Serializable
data class StoreVariantDto(
    @SerialName("public_code") val publicCode: String,
    val name: String,
    val type: String,
    @SerialName("price_delta") val priceDelta: String,
    @SerialName("effective_price") val effectivePrice: String
)

@Serializable
data class StorePaginationDto(
    @SerialName("current_page") val currentPage: Int = 1,
    @SerialName("per_page") val perPage: Int = 15,
    @SerialName("last_page") val lastPage: Int = 1,
    val total: Int = 0
)

@Serializable
data class StoreQuoteRequest(
    val items: List<StoreQuoteItemRequest>,
    @SerialName("delivery_method") val deliveryMethod: String,
    val department: String? = null,
    val city: String? = null,
    val zone: String? = null,
    @SerialName("delivery_address") val deliveryAddress: String? = null,
    @SerialName("coupon_code") val couponCode: String? = null
)

@Serializable
data class StoreQuoteItemRequest(
    @SerialName("product_public_code") val productPublicCode: String,
    @SerialName("variant_public_code") val variantPublicCode: String? = null,
    val quantity: Int
)

@Serializable
data class StoreQuoteDto(
    val items: List<StoreQuoteItemDto> = emptyList(),
    val subtotal: String,
    @SerialName("discount_total") val discountTotal: String,
    @SerialName("shipping_total") val shippingTotal: String,
    val total: String,
    val currency: String = "BOB",
    val coupon: StoreCouponDto? = null,
    val shipping: StoreShippingDto? = null,
    @SerialName("expires_at") val expiresAt: String? = null
)

@Serializable
data class StoreQuoteItemDto(
    val product: StoreQuoteProductDto,
    val variant: StoreQuoteVariantDto? = null,
    val quantity: Int,
    @SerialName("unit_price") val unitPrice: String,
    @SerialName("line_total") val lineTotal: String,
    @SerialName("price_reason") val priceReason: String? = null
)

@Serializable
data class StoreQuoteProductDto(@SerialName("public_code") val publicCode: String, val name: String)

@Serializable
data class StoreQuoteVariantDto(@SerialName("public_code") val publicCode: String, val name: String, val type: String)

@Serializable
data class StoreCouponDto(val applied: Boolean = false, val hint: String? = null)

@Serializable
data class StoreShippingDto(
    val method: String? = null,
    val amount: String? = null,
    val currency: String? = null,
    val scope: String? = null,
    val department: String? = null,
    val city: String? = null,
    val zone: String? = null
)

@Serializable
data class StoreOrderDto(
    val code: String,
    val date: String? = null,
    val status: String,
    @SerialName("status_label") val statusLabel: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val total: String,
    val currency: String = "BOB",
    @SerialName("delivery_method") val deliveryMethod: String,
    @SerialName("item_summary") val itemSummary: String? = null,
    val capabilities: StoreOrderCapabilitiesDto,
    val delivery: StoreDeliveryDto? = null,
    val items: List<StoreOrderItemDto> = emptyList(),
    val subtotal: String? = null,
    @SerialName("discount_total") val discountTotal: String? = null,
    @SerialName("shipping_total") val shippingTotal: String? = null,
    val payment: StoreOrderPaymentDto? = null,
    val receipts: List<StoreReceiptDto> = emptyList(),
    @SerialName("status_history") val statusHistory: List<StoreStatusHistoryDto> = emptyList()
)

@Serializable
data class StoreOrderCapabilitiesDto(
    @SerialName("can_upload_receipt") val canUploadReceipt: Boolean = false,
    @SerialName("can_open_whatsapp") val canOpenWhatsapp: Boolean = false,
    @SerialName("can_cancel") val canCancel: Boolean = false,
    @SerialName("can_view_receipt") val canViewReceipt: Boolean = false
)

@Serializable
data class StoreDeliveryDto(
    val method: String? = null,
    val department: String? = null,
    val city: String? = null,
    val zone: String? = null,
    val address: String? = null
)

@Serializable
data class StoreOrderItemDto(
    val sku: String? = null,
    val name: String,
    val variant: String? = null,
    @SerialName("unit_price") val unitPrice: String,
    val quantity: Int,
    @SerialName("discount_total") val discountTotal: String? = null,
    @SerialName("line_total") val lineTotal: String
)

@Serializable
data class StoreOrderPaymentDto(val status: String? = null, val message: String? = null)

@Serializable
data class StoreReceiptDto(
    @SerialName("public_code") val publicCode: String? = null,
    val status: String? = null,
    @SerialName("submitted_at") val submittedAt: String? = null,
    @SerialName("reviewed_at") val reviewedAt: String? = null,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("mime_type") val mimeType: String? = null,
    @SerialName("size_bytes") val sizeBytes: Long? = null
)

@Serializable
data class StoreStatusHistoryDto(
    @SerialName("from_status") val fromStatus: String? = null,
    @SerialName("to_status") val toStatus: String,
    @SerialName("changed_at") val changedAt: String? = null
)

@Serializable
data class StoreWhatsappDto(
    val url: String,
    @SerialName("opened_at") val openedAt: String? = null,
    @SerialName("message_preview") val messagePreview: String? = null
)
