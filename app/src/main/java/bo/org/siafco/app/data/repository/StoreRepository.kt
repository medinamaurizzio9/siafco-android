package bo.org.siafco.app.data.repository

import bo.org.siafco.app.core.network.UrlResolver
import bo.org.siafco.app.data.remote.ApiEnvelope
import bo.org.siafco.app.data.remote.SiafcoApi
import bo.org.siafco.app.data.remote.StoreCatalogPayload
import bo.org.siafco.app.data.remote.StoreCategoryDto
import bo.org.siafco.app.data.remote.StoreCouponDto
import bo.org.siafco.app.data.remote.StoreDeliveryDto
import bo.org.siafco.app.data.remote.StoreImageDto
import bo.org.siafco.app.data.remote.StoreOrderCapabilitiesDto
import bo.org.siafco.app.data.remote.StoreOrderDto
import bo.org.siafco.app.data.remote.StoreOrderItemDto
import bo.org.siafco.app.data.remote.StoreOrderPaymentDto
import bo.org.siafco.app.data.remote.StorePaginationDto
import bo.org.siafco.app.data.remote.StorePaymentSettingsDto
import bo.org.siafco.app.data.remote.StoreProductDto
import bo.org.siafco.app.data.remote.StoreQuoteDto
import bo.org.siafco.app.data.remote.StoreQuoteItemDto
import bo.org.siafco.app.data.remote.StoreQuoteItemRequest
import bo.org.siafco.app.data.remote.StoreQuoteRequest
import bo.org.siafco.app.data.remote.StoreReceiptDto
import bo.org.siafco.app.data.remote.StoreSettingsDto
import bo.org.siafco.app.data.remote.StoreShippingDto
import bo.org.siafco.app.data.remote.StoreStatusHistoryDto
import bo.org.siafco.app.data.remote.StoreVariantDto
import bo.org.siafco.app.data.remote.StoreWhatsappDto
import bo.org.siafco.app.domain.PreparedReceipt
import bo.org.siafco.app.domain.StoreCatalog
import bo.org.siafco.app.domain.StoreCategory
import bo.org.siafco.app.domain.StoreCoupon
import bo.org.siafco.app.domain.StoreDelivery
import bo.org.siafco.app.domain.StoreImage
import bo.org.siafco.app.domain.StoreOrder
import bo.org.siafco.app.domain.StoreOrderCapabilities
import bo.org.siafco.app.domain.StoreOrderItem
import bo.org.siafco.app.domain.StoreOrderPayment
import bo.org.siafco.app.domain.StorePagination
import bo.org.siafco.app.domain.StorePaymentSettings
import bo.org.siafco.app.domain.StoreProduct
import bo.org.siafco.app.domain.StoreQuote
import bo.org.siafco.app.domain.StoreQuoteItem
import bo.org.siafco.app.domain.StoreQuoteRequestData
import bo.org.siafco.app.domain.StoreReceipt
import bo.org.siafco.app.domain.StoreSettings
import bo.org.siafco.app.domain.StoreShipping
import bo.org.siafco.app.domain.StoreStatusHistory
import bo.org.siafco.app.domain.StoreVariant
import bo.org.siafco.app.domain.StoreWhatsapp
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import java.io.IOException

class StoreRepository(
    private val api: SiafcoApi,
    private val onUnauthorized: suspend () -> Unit = {}
) : StoreGateway {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun catalog(filters: StoreCatalogFilters): StoreResult<StoreCatalog> = safeCall {
        api.storeCatalog(
            search = filters.search.takeIf { it.isNotBlank() },
            category = filters.categorySlug,
            featured = filters.featured,
            availability = filters.availability,
            page = filters.page,
            perPage = filters.perPage
        ).toResult { it.toDomain() }
    }

    override suspend fun product(publicCode: String): StoreResult<StoreProduct> = safeCall {
        api.storeProduct(publicCode).toResult { it.product.toDomain() }
    }

    override suspend fun quote(request: StoreQuoteRequestData): StoreResult<StoreQuote> = safeCall {
        api.storeQuote(request.toDto()).toResult { it.quote.toDomain() }
    }

    override suspend fun createOrder(idempotencyKey: String, request: StoreQuoteRequestData): StoreResult<StoreOrder> = safeCall {
        api.createStoreOrder(idempotencyKey, request.toDto()).toResult { it.order.toDomain() }
    }

    override suspend fun orders(filters: StoreOrderFilters): StoreResult<StoreOrderList> = safeCall {
        api.storeOrders(
            status = filters.status,
            dateFrom = filters.dateFrom,
            dateTo = filters.dateTo,
            code = filters.code.takeIf { it.isNotBlank() },
            page = filters.page,
            perPage = filters.perPage
        ).toResult { StoreOrderList(it.orders.map { order -> order.toDomain() }, it.pagination.toDomain()) }
    }

    override suspend fun order(code: String): StoreResult<StoreOrder> = safeCall {
        api.storeOrder(code).toResult { it.order.toDomain() }
    }

    override suspend fun submitReceipt(
        orderCode: String,
        idempotencyKey: String,
        receipt: PreparedReceipt
    ): StoreResult<StoreOrder> = safeCall {
        val part = MultipartBody.Part.createFormData(
            name = "receipt",
            filename = "receipt-${receipt.sha256.take(12)}.${receipt.extension()}",
            body = receipt.file.asRequestBody(receipt.mimeType.toMediaType())
        )
        api.submitStoreReceipt(orderCode, idempotencyKey, part).toResult { it.order.toDomain() }
    }

    override suspend fun whatsapp(orderCode: String): StoreResult<StoreWhatsapp> = safeCall {
        api.storeWhatsapp(orderCode).toResult { it.whatsapp.toDomain() }
    }

    private suspend fun <T> safeCall(block: suspend () -> StoreResult<T>): StoreResult<T> {
        return try {
            block()
        } catch (_: IOException) {
            StoreResult.NetworkError
        } catch (_: RuntimeException) {
            StoreResult.InvalidPayload
        }
    }

    private suspend fun <T, R> Response<ApiEnvelope<T>>.toResult(mapper: (T) -> R): StoreResult<R> {
        if (isSuccessful) {
            val payload = body()?.data
            return if (body()?.success == true && payload != null) {
                runCatching { StoreResult.Success(mapper(payload)) }.getOrElse { StoreResult.InvalidPayload }
            } else {
                StoreResult.InvalidPayload
            }
        }

        val error = parseError()
        return when (code()) {
            401 -> {
                onUnauthorized()
                StoreResult.Unauthorized
            }
            403 -> StoreResult.Forbidden(error?.message)
            404 -> StoreResult.NotFound(error?.message)
            409 -> StoreResult.Conflict(error?.message)
            422 -> StoreResult.ValidationError(error?.errors.orEmpty())
            429 -> StoreResult.RateLimited(error?.message)
            else -> StoreResult.HttpError(code(), error?.message)
        }
    }

    private fun Response<*>.parseError(): ApiEnvelope<Unit>? {
        val raw = errorBody()?.string().orEmpty()
        if (raw.isBlank()) return null
        return runCatching { json.decodeFromString<ApiEnvelope<Unit>>(raw) }.getOrNull()
    }
}

interface StoreGateway {
    suspend fun catalog(filters: StoreCatalogFilters = StoreCatalogFilters()): StoreResult<StoreCatalog>
    suspend fun product(publicCode: String): StoreResult<StoreProduct>
    suspend fun quote(request: StoreQuoteRequestData): StoreResult<StoreQuote>
    suspend fun createOrder(idempotencyKey: String, request: StoreQuoteRequestData): StoreResult<StoreOrder>
    suspend fun orders(filters: StoreOrderFilters = StoreOrderFilters()): StoreResult<StoreOrderList>
    suspend fun order(code: String): StoreResult<StoreOrder>
    suspend fun submitReceipt(orderCode: String, idempotencyKey: String, receipt: PreparedReceipt): StoreResult<StoreOrder>
    suspend fun whatsapp(orderCode: String): StoreResult<StoreWhatsapp>
}

data class StoreCatalogFilters(
    val search: String = "",
    val categorySlug: String? = null,
    val featured: Boolean? = null,
    val availability: String? = null,
    val page: Int? = null,
    val perPage: Int? = null
)

data class StoreOrderFilters(
    val status: String? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val code: String = "",
    val page: Int? = null,
    val perPage: Int? = null
)

data class StoreOrderList(val orders: List<StoreOrder>, val pagination: StorePagination)

sealed interface StoreResult<out T> {
    data class Success<T>(val value: T) : StoreResult<T>
    data class ValidationError(val errors: Map<String, List<String>>) : StoreResult<Nothing>
    data class Forbidden(val message: String?) : StoreResult<Nothing>
    data class NotFound(val message: String?) : StoreResult<Nothing>
    data class Conflict(val message: String?) : StoreResult<Nothing>
    data class RateLimited(val message: String?) : StoreResult<Nothing>
    data class HttpError(val code: Int, val message: String?) : StoreResult<Nothing>
    data object Unauthorized : StoreResult<Nothing>
    data object NetworkError : StoreResult<Nothing>
    data object InvalidPayload : StoreResult<Nothing>
}

private fun StoreQuoteRequestData.toDto(): StoreQuoteRequest = StoreQuoteRequest(
    items = lines.map {
        StoreQuoteItemRequest(
            productPublicCode = it.productPublicCode,
            variantPublicCode = it.variantPublicCode,
            quantity = it.quantity
        )
    },
    deliveryMethod = deliveryMethod,
    department = department,
    city = city,
    zone = zone,
    deliveryAddress = deliveryAddress,
    couponCode = couponCode
)

private fun StoreCatalogPayload.toDomain(): StoreCatalog = StoreCatalog(
    settings = settings.toDomain(),
    featured = featured.map { it.toDomain() },
    categories = categories.map { it.toDomain() },
    products = products.map { it.toDomain() },
    pagination = pagination.toDomain()
)

private fun StoreSettingsDto.toDomain(): StoreSettings = StoreSettings(
    currency = currency,
    pickupEnabled = pickupEnabled,
    shippingEnabled = shippingEnabled,
    pickupInstructions = pickupInstructions,
    shippingInstructions = shippingInstructions,
    payment = payment?.toDomain(),
    whatsappEnabled = whatsappEnabled
)

private fun StorePaymentSettingsDto.toDomain(): StorePaymentSettings = StorePaymentSettings(
    qrUrl = qrUrl?.let(UrlResolver::resolve),
    bank = bank,
    holder = holder,
    account = account,
    instructions = instructions
)

private fun StoreCategoryDto.toDomain(): StoreCategory = StoreCategory(slug = slug, name = name)

private fun StoreProductDto.toDomain(): StoreProduct = StoreProduct(
    publicCode = publicCode,
    slug = slug,
    sku = sku,
    name = name,
    shortDescription = shortDescription,
    description = description,
    regularPrice = regularPrice,
    affiliatePrice = affiliatePrice,
    effectivePrice = effectivePrice,
    promoPrice = promoPrice,
    currency = currency,
    availabilityStatus = availabilityStatus,
    deliveryModes = deliveryModes,
    featured = featured,
    maxQuantityPerOrder = maxQuantityPerOrder,
    primaryImageUrl = primaryImageUrl?.let(UrlResolver::resolve),
    category = category?.toDomain(),
    canOrder = capabilities?.canOrder == true,
    images = images.map { it.toDomain() },
    variants = variants.map { it.toDomain() }
)

private fun StoreImageDto.toDomain(): StoreImage = StoreImage(url = url?.let(UrlResolver::resolve), alt = alt, isPrimary = isPrimary)

private fun StoreVariantDto.toDomain(): StoreVariant = StoreVariant(
    publicCode = publicCode,
    name = name,
    type = type,
    priceDelta = priceDelta,
    effectivePrice = effectivePrice
)

private fun StorePaginationDto.toDomain(): StorePagination = StorePagination(currentPage, perPage, lastPage, total)

private fun StoreQuoteDto.toDomain(): StoreQuote = StoreQuote(
    items = items.map { it.toDomain() },
    subtotal = subtotal,
    discountTotal = discountTotal,
    shippingTotal = shippingTotal,
    total = total,
    currency = currency,
    coupon = coupon?.toDomain(),
    shipping = shipping?.toDomain(),
    expiresAt = expiresAt
)

private fun StoreQuoteItemDto.toDomain(): StoreQuoteItem = StoreQuoteItem(
    productPublicCode = product.publicCode,
    productName = product.name,
    variantPublicCode = variant?.publicCode,
    variantName = variant?.name,
    variantType = variant?.type,
    quantity = quantity,
    unitPrice = unitPrice,
    lineTotal = lineTotal,
    priceReason = priceReason
)

private fun StoreCouponDto.toDomain(): StoreCoupon = StoreCoupon(applied, hint)

private fun StoreShippingDto.toDomain(): StoreShipping = StoreShipping(method, amount, currency, scope, department, city, zone)

private fun StoreOrderDto.toDomain(): StoreOrder = StoreOrder(
    code = code,
    date = date,
    status = status,
    statusLabel = statusLabel,
    createdAt = createdAt,
    updatedAt = updatedAt,
    total = total,
    currency = currency,
    deliveryMethod = deliveryMethod,
    itemSummary = itemSummary,
    capabilities = capabilities.toDomain(),
    delivery = delivery?.toDomain(),
    items = items.map { it.toDomain() },
    subtotal = subtotal,
    discountTotal = discountTotal,
    shippingTotal = shippingTotal,
    payment = payment?.toDomain(),
    receipts = receipts.map { it.toDomain() },
    statusHistory = statusHistory.map { it.toDomain() }
)

private fun StoreOrderCapabilitiesDto.toDomain(): StoreOrderCapabilities = StoreOrderCapabilities(
    canUploadReceipt = canUploadReceipt,
    canOpenWhatsapp = canOpenWhatsapp,
    canCancel = canCancel,
    canViewReceipt = canViewReceipt
)

private fun StoreDeliveryDto.toDomain(): StoreDelivery = StoreDelivery(method, department, city, zone, address)

private fun StoreOrderItemDto.toDomain(): StoreOrderItem = StoreOrderItem(
    sku = sku,
    name = name,
    variant = variant,
    unitPrice = unitPrice,
    quantity = quantity,
    discountTotal = discountTotal,
    lineTotal = lineTotal
)

private fun StoreOrderPaymentDto.toDomain(): StoreOrderPayment = StoreOrderPayment(status, message)

private fun StoreReceiptDto.toDomain(): StoreReceipt = StoreReceipt(
    publicCode = publicCode,
    status = status,
    submittedAt = submittedAt,
    reviewedAt = reviewedAt,
    rejectionReason = rejectionReason,
    mimeType = mimeType,
    sizeBytes = sizeBytes
)

private fun StoreStatusHistoryDto.toDomain(): StoreStatusHistory = StoreStatusHistory(fromStatus, toStatus, changedAt)

private fun StoreWhatsappDto.toDomain(): StoreWhatsapp = StoreWhatsapp(url = url, openedAt = openedAt, messagePreview = messagePreview)

private fun PreparedReceipt.extension(): String = when (mimeType) {
    "image/jpeg" -> "jpg"
    "image/png" -> "png"
    "image/webp" -> "webp"
    "application/pdf" -> "pdf"
    else -> "bin"
}
