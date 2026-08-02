package bo.org.siafco.app.domain

data class StoreCatalog(
    val settings: StoreSettings,
    val featured: List<StoreProduct>,
    val categories: List<StoreCategory>,
    val products: List<StoreProduct>,
    val pagination: StorePagination
)

data class StoreSettings(
    val currency: String,
    val pickupEnabled: Boolean,
    val shippingEnabled: Boolean,
    val pickupInstructions: String?,
    val shippingInstructions: String?,
    val payment: StorePaymentSettings?,
    val whatsappEnabled: Boolean
)

data class StorePaymentSettings(
    val qrUrl: String?,
    val bank: String?,
    val holder: String?,
    val account: String?,
    val instructions: String?
)

data class StoreCategory(val slug: String, val name: String)

data class StoreProduct(
    val publicCode: String,
    val slug: String?,
    val sku: String?,
    val name: String,
    val shortDescription: String?,
    val description: String?,
    val regularPrice: String,
    val affiliatePrice: String,
    val effectivePrice: String,
    val promoPrice: String?,
    val currency: String,
    val availabilityStatus: String,
    val deliveryModes: List<String>,
    val featured: Boolean,
    val maxQuantityPerOrder: Int,
    val primaryImageUrl: String?,
    val category: StoreCategory?,
    val canOrder: Boolean,
    val images: List<StoreImage>,
    val variants: List<StoreVariant>
) {
    val isAvailable: Boolean get() = availabilityStatus == "available" && canOrder
}

data class StoreImage(val url: String?, val alt: String?, val isPrimary: Boolean)

data class StoreVariant(
    val publicCode: String,
    val name: String,
    val type: String,
    val priceDelta: String,
    val effectivePrice: String
)

data class StorePagination(
    val currentPage: Int,
    val perPage: Int,
    val lastPage: Int,
    val total: Int
)

data class StoreCartLine(
    val productPublicCode: String,
    val variantPublicCode: String? = null,
    val quantity: Int = 1
) {
    val key: String get() = listOf(productPublicCode, variantPublicCode.orEmpty()).joinToString(":")
}

data class StoreQuoteRequestData(
    val lines: List<StoreCartLine>,
    val deliveryMethod: String,
    val department: String? = null,
    val city: String? = null,
    val zone: String? = null,
    val deliveryAddress: String? = null,
    val couponCode: String? = null
)

data class StoreQuote(
    val items: List<StoreQuoteItem>,
    val subtotal: String,
    val discountTotal: String,
    val shippingTotal: String,
    val total: String,
    val currency: String,
    val coupon: StoreCoupon?,
    val shipping: StoreShipping?,
    val expiresAt: String?
)

data class StoreQuoteItem(
    val productPublicCode: String,
    val productName: String,
    val variantPublicCode: String?,
    val variantName: String?,
    val variantType: String?,
    val quantity: Int,
    val unitPrice: String,
    val lineTotal: String,
    val priceReason: String?
)

data class StoreCoupon(val applied: Boolean, val hint: String?)

data class StoreShipping(
    val method: String?,
    val amount: String?,
    val currency: String?,
    val scope: String?,
    val department: String?,
    val city: String?,
    val zone: String?
)

data class StoreOrder(
    val code: String,
    val date: String?,
    val status: String,
    val statusLabel: String,
    val createdAt: String?,
    val updatedAt: String?,
    val total: String,
    val currency: String,
    val deliveryMethod: String,
    val itemSummary: String?,
    val capabilities: StoreOrderCapabilities,
    val delivery: StoreDelivery?,
    val items: List<StoreOrderItem>,
    val subtotal: String?,
    val discountTotal: String?,
    val shippingTotal: String?,
    val payment: StoreOrderPayment?,
    val receipts: List<StoreReceipt>,
    val statusHistory: List<StoreStatusHistory>
)

data class StoreOrderCapabilities(
    val canUploadReceipt: Boolean,
    val canOpenWhatsapp: Boolean,
    val canCancel: Boolean,
    val canViewReceipt: Boolean
)

data class StoreDelivery(
    val method: String?,
    val department: String?,
    val city: String?,
    val zone: String?,
    val address: String?
)

data class StoreOrderItem(
    val sku: String?,
    val name: String,
    val variant: String?,
    val unitPrice: String,
    val quantity: Int,
    val discountTotal: String?,
    val lineTotal: String
)

data class StoreOrderPayment(val status: String?, val message: String?)

data class StoreReceipt(
    val publicCode: String?,
    val status: String?,
    val submittedAt: String?,
    val reviewedAt: String?,
    val rejectionReason: String?,
    val mimeType: String?,
    val sizeBytes: Long?
)

data class StoreStatusHistory(
    val fromStatus: String?,
    val toStatus: String,
    val changedAt: String?
)

data class StoreWhatsapp(val url: String, val openedAt: String?, val messagePreview: String?)
