package bo.org.siafco.app.feature.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.org.siafco.app.core.text.HumanTextInputNormalizer
import bo.org.siafco.app.core.text.TextInputNormalization
import bo.org.siafco.app.data.repository.StoreCatalogFilters
import bo.org.siafco.app.data.repository.StoreGateway
import bo.org.siafco.app.data.repository.StoreResult
import bo.org.siafco.app.data.store.StoreCartStore
import bo.org.siafco.app.data.store.StorePendingOrderStore
import bo.org.siafco.app.data.store.storePayloadSignature
import bo.org.siafco.app.domain.StoreCartLine
import bo.org.siafco.app.domain.StoreAvailability
import bo.org.siafco.app.domain.StoreCatalog
import bo.org.siafco.app.domain.StoreOrder
import bo.org.siafco.app.domain.StorePagination
import bo.org.siafco.app.domain.PreparedReceipt
import bo.org.siafco.app.domain.StoreProduct
import bo.org.siafco.app.domain.StoreQuote
import bo.org.siafco.app.domain.StoreQuoteRequestData
import bo.org.siafco.app.domain.StoreSettings
import bo.org.siafco.app.feature.UiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class StoreCatalogViewModel(
    private val store: StoreGateway
) : ViewModel() {
    private val _state = MutableStateFlow(StoreCatalogUiState())
    val state: StateFlow<StoreCatalogUiState> = _state.asStateFlow()

    fun load(force: Boolean = false) {
        if (_state.value.loading || (_state.value.catalog != null && !force)) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = null) }
            when (val result = store.catalog(_state.value.filters)) {
                is StoreResult.Success -> _state.update { it.copy(loading = false, catalog = result.value) }
                else -> _state.update { it.copy(loading = false, message = result.toUiMessage(), loggedOut = result is StoreResult.Unauthorized) }
            }
        }
    }

    fun search(value: String) {
        _state.update { it.copy(filters = it.filters.copy(search = value, page = 1)) }
    }

    fun selectCategory(slug: String?) {
        _state.update { it.copy(filters = it.filters.copy(categorySlug = slug, page = 1)) }
        load(force = true)
    }

    fun applySearch() = load(force = true)
}

data class StoreCatalogUiState(
    val loading: Boolean = false,
    val catalog: StoreCatalog? = null,
    val filters: StoreCatalogFilters = StoreCatalogFilters(perPage = 20),
    val message: UiMessage? = null,
    val loggedOut: Boolean = false
)

class StoreProductViewModel(
    private val store: StoreGateway,
    private val cart: StoreCartStore
) : ViewModel() {
    private val _state = MutableStateFlow(StoreProductUiState())
    val state: StateFlow<StoreProductUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            cart.lines.collectLatest { lines ->
                _state.update { it.copy(cartCount = lines.sumOf(StoreCartLine::quantity)) }
            }
        }
    }

    fun load(publicCode: String) {
        if (_state.value.loading || _state.value.product?.publicCode == publicCode) return
        viewModelScope.launch {
            _state.update { StoreProductUiState(loading = true, cartCount = it.cartCount) }
            when (val result = store.product(publicCode)) {
                is StoreResult.Success -> _state.update {
                    it.copy(
                        loading = false,
                        product = result.value,
                        selectedVariantPublicCode = null,
                        quantity = 1.coerceAtMost(result.value.maxQuantityPerOrder.coerceAtLeast(1)),
                        added = false,
                        adding = false,
                        message = null
                    )
                }
                else -> _state.update {
                    it.copy(loading = false, message = result.toUiMessage(), loggedOut = result is StoreResult.Unauthorized)
                }
            }
        }
    }

    fun selectVariant(publicCode: String?) {
        _state.update { it.copy(selectedVariantPublicCode = publicCode, added = false) }
    }

    fun setQuantity(quantity: Int) {
        _state.update { it.copy(quantity = quantity.coerceIn(1, it.product?.maxQuantityPerOrder ?: 99), added = false) }
    }

    fun addToCart() {
        val current = _state.value
        val product = current.product ?: return
        if (current.adding || !current.canAddToCart) return
        _state.update { it.copy(adding = true) }
        viewModelScope.launch {
            cart.add(StoreCartLine(product.publicCode, current.selectedVariantPublicCode, current.quantity))
            _state.update { it.copy(added = true, adding = false) }
        }
    }
}

data class StoreProductUiState(
    val loading: Boolean = false,
    val product: StoreProduct? = null,
    val selectedVariantPublicCode: String? = null,
    val quantity: Int = 1,
    val added: Boolean = false,
    val adding: Boolean = false,
    val cartCount: Int = 0,
    val message: UiMessage? = null,
    val loggedOut: Boolean = false
) {
    val canAddToCart: Boolean
        get() = product?.canAddToCart(selectedVariantPublicCode, quantity) == true && !adding

    val disabledReason: StoreProductDisabledReason?
        get() {
            val product = product ?: return null
            return when {
                product.availabilityStatus == StoreAvailability.SoldOut -> StoreProductDisabledReason.SoldOut
                product.availabilityStatus == StoreAvailability.ComingSoon -> StoreProductDisabledReason.ComingSoon
                product.availabilityStatus != StoreAvailability.Available || !product.canOrder -> StoreProductDisabledReason.Unavailable
                product.hasVariants && selectedVariantPublicCode.isNullOrBlank() -> StoreProductDisabledReason.SelectVariant
                quantity >= product.maxQuantityPerOrder -> StoreProductDisabledReason.MaxQuantityReached
                else -> null
            }
        }
}

enum class StoreProductDisabledReason {
    SoldOut,
    ComingSoon,
    SelectVariant,
    Unavailable,
    MaxQuantityReached
}

class StoreCartViewModel(
    private val store: StoreGateway,
    private val cart: StoreCartStore
) : ViewModel() {
    private val _state = MutableStateFlow(StoreCartUiState())
    val state: StateFlow<StoreCartUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            cart.lines.collectLatest { lines ->
                _state.update { it.copy(lines = lines) }
                if (lines.isNotEmpty()) quote()
            }
        }
    }

    fun updateQuantity(line: StoreCartLine, quantity: Int) {
        viewModelScope.launch { cart.updateQuantity(line.productPublicCode, line.variantPublicCode, quantity) }
    }

    fun remove(line: StoreCartLine) {
        viewModelScope.launch { cart.remove(line.productPublicCode, line.variantPublicCode) }
    }

    fun clear() {
        viewModelScope.launch { cart.clear() }
    }

    fun quote() {
        val lines = _state.value.lines
        if (lines.isEmpty() || _state.value.loadingQuote) return
        viewModelScope.launch {
            _state.update { it.copy(loadingQuote = true, message = null) }
            val request = StoreQuoteRequestData(lines = lines, deliveryMethod = _state.value.deliveryMethod)
            when (val result = store.quote(request)) {
                is StoreResult.Success -> _state.update { it.copy(loadingQuote = false, quote = result.value) }
                else -> _state.update { it.copy(loadingQuote = false, message = result.toUiMessage(), loggedOut = result is StoreResult.Unauthorized) }
            }
        }
    }
}

data class StoreCartUiState(
    val lines: List<StoreCartLine> = emptyList(),
    val deliveryMethod: String = "pickup",
    val loadingQuote: Boolean = false,
    val quote: StoreQuote? = null,
    val message: UiMessage? = null,
    val loggedOut: Boolean = false
)

class StoreCheckoutViewModel(
    private val store: StoreGateway,
    private val cart: StoreCartStore,
    private val pendingOrderStore: StorePendingOrderStore
) : ViewModel() {
    private val _state = MutableStateFlow(StoreCheckoutUiState())
    val state: StateFlow<StoreCheckoutUiState> = _state.asStateFlow()
    private var quoteVersion = 0

    init {
        viewModelScope.launch {
            cart.lines.collectLatest { lines ->
                _state.update { it.copy(lines = lines) }
                if (lines.isNotEmpty()) quote()
            }
        }
        viewModelScope.launch {
            when (val result = store.catalog(StoreCatalogFilters(perPage = 1))) {
                is StoreResult.Success -> _state.update { it.copy(settings = result.value.settings) }
                else -> Unit
            }
        }
    }

    fun updateForm(block: StoreCheckoutForm.() -> StoreCheckoutForm) {
        _state.update { it.copy(form = it.form.block(), fieldErrors = emptyMap(), message = null) }
        quote()
    }

    fun quote() {
        val current = _state.value
        if (current.lines.isEmpty()) return
        val request = current.form.toRequest(current.lines)
        val version = ++quoteVersion
        viewModelScope.launch {
            _state.update { it.copy(loadingQuote = true, message = null) }
            when (val result = store.quote(request)) {
                is StoreResult.Success -> if (version == quoteVersion) _state.update { it.copy(loadingQuote = false, quote = result.value) }
                is StoreResult.ValidationError -> _state.update {
                    if (version == quoteVersion) {
                        it.copy(
                            loadingQuote = false,
                            quote = null,
                            fieldErrors = result.errors.mapValues { entry -> entry.value.firstOrNull().orEmpty() },
                            message = UiMessage.Validation
                        )
                    } else {
                        it
                    }
                }
                else -> if (version == quoteVersion) {
                    _state.update { it.copy(loadingQuote = false, quote = null, message = result.toUiMessage(), loggedOut = result is StoreResult.Unauthorized) }
                }
            }
        }
    }

    fun submit() {
        val current = _state.value
        if (current.submitting || current.lines.isEmpty() || current.quote == null || current.loadingQuote) return
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, message = null, fieldErrors = emptyMap()) }
            val request = current.form.toRequest(current.lines)
            val key = pendingOrderStore.keyFor(request.signature())
            when (val result = store.createOrder(key, request)) {
                is StoreResult.Success -> {
                    pendingOrderStore.clear()
                    cart.clear()
                    _state.value = StoreCheckoutUiState(createdOrder = result.value)
                }
                is StoreResult.ValidationError -> _state.update {
                    it.copy(submitting = false, fieldErrors = result.errors.mapValues { entry -> entry.value.firstOrNull().orEmpty() }, message = UiMessage.Validation)
                }
                else -> _state.update { it.copy(submitting = false, message = result.toUiMessage(), loggedOut = result is StoreResult.Unauthorized) }
            }
        }
    }
}

class StoreOrdersViewModel(private val store: StoreGateway) : ViewModel() {
    private val _state = MutableStateFlow(StoreOrdersUiState())
    val state: StateFlow<StoreOrdersUiState> = _state.asStateFlow()

    fun load(force: Boolean = false) {
        if (_state.value.loading || (_state.value.loaded && !force)) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = null) }
            when (val result = store.orders()) {
                is StoreResult.Success -> _state.value = StoreOrdersUiState(
                    loaded = true,
                    orders = result.value.orders,
                    pagination = result.value.pagination
                )
                else -> _state.update { it.copy(loading = false, loaded = true, message = result.toUiMessage(), loggedOut = result is StoreResult.Unauthorized) }
            }
        }
    }
}

data class StoreOrdersUiState(
    val loaded: Boolean = false,
    val loading: Boolean = false,
    val orders: List<StoreOrder> = emptyList(),
    val pagination: StorePagination? = null,
    val message: UiMessage? = null,
    val loggedOut: Boolean = false
)

class StoreOrderDetailViewModel(private val store: StoreGateway) : ViewModel() {
    private val _state = MutableStateFlow(StoreOrderDetailUiState())
    val state: StateFlow<StoreOrderDetailUiState> = _state.asStateFlow()

    fun load(code: String, force: Boolean = false) {
        if (_state.value.loading || (_state.value.order?.code == code && !force)) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = null) }
            when (val result = store.order(code)) {
                is StoreResult.Success -> _state.value = StoreOrderDetailUiState(order = result.value)
                else -> _state.update { it.copy(loading = false, message = result.toUiMessage(), loggedOut = result is StoreResult.Unauthorized) }
            }
        }
    }

    fun requestWhatsapp() {
        val orderCode = _state.value.order?.code ?: return
        if (_state.value.openingWhatsapp) return
        viewModelScope.launch {
            _state.update { it.copy(openingWhatsapp = true, message = null, whatsappUrl = null) }
            when (val result = store.whatsapp(orderCode)) {
                is StoreResult.Success -> _state.update { it.copy(openingWhatsapp = false, whatsappUrl = result.value.url) }
                else -> _state.update { it.copy(openingWhatsapp = false, message = result.toUiMessage(), loggedOut = result is StoreResult.Unauthorized) }
            }
        }
    }

    fun whatsappConsumed() {
        _state.update { it.copy(whatsappUrl = null) }
    }
}

data class StoreOrderDetailUiState(
    val loading: Boolean = false,
    val order: StoreOrder? = null,
    val openingWhatsapp: Boolean = false,
    val whatsappUrl: String? = null,
    val message: UiMessage? = null,
    val loggedOut: Boolean = false
)

class StoreReceiptViewModel(private val store: StoreGateway) : ViewModel() {
    private val _state = MutableStateFlow(StoreReceiptUiState())
    val state: StateFlow<StoreReceiptUiState> = _state.asStateFlow()

    fun setReceipt(receipt: PreparedReceipt?) {
        _state.update {
            if (receipt == null) {
                it.copy(fieldError = "El comprobante debe ser JPG, PNG, WEBP o PDF.")
            } else {
                it.copy(receipt = receipt, fieldError = null, message = null, submittedOrder = null)
            }
        }
    }

    fun submit(orderCode: String) {
        val receipt = _state.value.receipt ?: run {
            _state.update { it.copy(fieldError = "Selecciona un comprobante.") }
            return
        }
        if (_state.value.submitting) return
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, message = null, fieldError = null) }
            when (val result = store.submitReceipt(orderCode, UUID.randomUUID().toString(), receipt)) {
                is StoreResult.Success -> {
                    receipt.file.delete()
                    _state.value = StoreReceiptUiState(submittedOrder = result.value)
                }
                is StoreResult.ValidationError -> _state.update {
                    it.copy(
                        submitting = false,
                        fieldError = result.errors["receipt"]?.firstOrNull(),
                        message = UiMessage.Validation
                    )
                }
                StoreResult.NetworkError -> _state.update { it.copy(submitting = false, message = UiMessage.Network) }
                else -> _state.update { it.copy(submitting = false, message = result.toUiMessage(), loggedOut = result is StoreResult.Unauthorized) }
            }
        }
    }
}

data class StoreReceiptUiState(
    val receipt: PreparedReceipt? = null,
    val submitting: Boolean = false,
    val submittedOrder: StoreOrder? = null,
    val fieldError: String? = null,
    val message: UiMessage? = null,
    val loggedOut: Boolean = false
)

data class StoreCheckoutForm(
    val deliveryMethod: String = "pickup",
    val department: String = "",
    val city: String = "",
    val zone: String = "",
    val deliveryAddress: String = "",
    val couponCode: String = ""
) {
    fun toRequest(lines: List<StoreCartLine>): StoreQuoteRequestData = StoreQuoteRequestData(
        lines = lines,
        deliveryMethod = deliveryMethod,
        department = HumanTextInputNormalizer.optionalForSubmit(department),
        city = HumanTextInputNormalizer.optionalForSubmit(city),
        zone = HumanTextInputNormalizer.optionalForSubmit(zone),
        deliveryAddress = HumanTextInputNormalizer.optionalForSubmit(deliveryAddress),
        couponCode = HumanTextInputNormalizer.optionalForSubmit(couponCode, TextInputNormalization.Coupon)
    )
}

private fun StoreQuoteRequestData.signature(): String = storePayloadSignature(
    buildString {
        lines.sortedBy { it.key }.forEach {
            append(it.productPublicCode).append('|')
            append(it.variantPublicCode.orEmpty()).append('|')
            append(it.quantity).append(';')
        }
        append(deliveryMethod).append('|')
        append(HumanTextInputNormalizer.forSubmit(department.orEmpty())).append('|')
        append(HumanTextInputNormalizer.forSubmit(city.orEmpty())).append('|')
        append(HumanTextInputNormalizer.forSubmit(zone.orEmpty())).append('|')
        append(HumanTextInputNormalizer.forSubmit(deliveryAddress.orEmpty())).append('|')
        append(HumanTextInputNormalizer.forSubmit(couponCode.orEmpty(), TextInputNormalization.Coupon))
    }
)

data class StoreCheckoutUiState(
    val lines: List<StoreCartLine> = emptyList(),
    val form: StoreCheckoutForm = StoreCheckoutForm(),
    val settings: StoreSettings? = null,
    val quote: StoreQuote? = null,
    val loadingQuote: Boolean = false,
    val submitting: Boolean = false,
    val createdOrder: StoreOrder? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    val message: UiMessage? = null,
    val loggedOut: Boolean = false
) {
    val canCreateOrder: Boolean get() = lines.isNotEmpty() && quote != null && !loadingQuote && !submitting
}

private fun StoreResult<*>.toUiMessage(): UiMessage = when (this) {
    StoreResult.Unauthorized -> UiMessage.Unauthorized
    is StoreResult.Forbidden -> UiMessage.Forbidden
    is StoreResult.NotFound -> UiMessage.RequestNotFound
    is StoreResult.RateLimited -> UiMessage.RateLimited
    is StoreResult.ValidationError -> UiMessage.Validation
    StoreResult.NetworkError -> UiMessage.Network
    else -> UiMessage.Unknown
}
