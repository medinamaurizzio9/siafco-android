package bo.org.siafco.app.feature.store

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import bo.org.siafco.app.core.text.HumanTextInputNormalizer
import bo.org.siafco.app.R
import bo.org.siafco.app.core.text.TextInputNormalization
import bo.org.siafco.app.core.ui.CooperativeBottomBar
import bo.org.siafco.app.core.ui.CooperativeDestination
import bo.org.siafco.app.core.ui.CooperativeSpacing
import bo.org.siafco.app.core.ui.CooperativeTextSecondary
import bo.org.siafco.app.core.ui.CooperativeTopBar
import bo.org.siafco.app.core.ui.EmptyState
import bo.org.siafco.app.core.ui.FigmaGold
import bo.org.siafco.app.core.ui.FigmaNavy
import bo.org.siafco.app.core.ui.FigmaNavyDeep
import bo.org.siafco.app.core.ui.InstitutionalCard
import bo.org.siafco.app.core.ui.LoadingSkeleton
import bo.org.siafco.app.core.ui.MoneyText
import bo.org.siafco.app.core.ui.NormalizedTextField
import bo.org.siafco.app.core.ui.PrimaryButton
import bo.org.siafco.app.core.ui.SectionHeader
import bo.org.siafco.app.core.ui.SecondaryButton
import bo.org.siafco.app.core.ui.StatusBadge
import bo.org.siafco.app.data.receipt.ReceiptPreparer
import bo.org.siafco.app.domain.StoreCartLine
import bo.org.siafco.app.domain.StoreCategory
import bo.org.siafco.app.domain.StoreOrder
import bo.org.siafco.app.domain.StoreOrderItem
import bo.org.siafco.app.domain.StoreProduct
import bo.org.siafco.app.domain.StoreQuote
import bo.org.siafco.app.domain.StoreQuoteItem
import bo.org.siafco.app.feature.UiMessage
import bo.org.siafco.app.feature.store.StoreProductDisabledReason.ComingSoon
import bo.org.siafco.app.feature.store.StoreProductDisabledReason.MaxQuantityReached
import bo.org.siafco.app.feature.store.StoreProductDisabledReason.SelectVariant
import bo.org.siafco.app.feature.store.StoreProductDisabledReason.SoldOut
import bo.org.siafco.app.feature.store.StoreProductDisabledReason.Unavailable
import coil3.compose.AsyncImage
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreCatalogScreen(
    viewModel: StoreCatalogViewModel,
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    onOpenProduct: (String) -> Unit,
    onOpenCart: () -> Unit,
    onBottomDestination: (CooperativeDestination) -> Unit
) {
    val state by viewModel.state.collectAsState()
    var confirmLogout by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(state.loggedOut) { if (state.loggedOut) onLoggedOut() }
    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text(stringResource(R.string.store_logout_confirm_title), fontWeight = FontWeight.Black) },
            text = { Text(stringResource(R.string.store_logout_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmLogout = false
                        viewModel.logout()
                    }
                ) {
                    Text(stringResource(R.string.home_logout))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    Scaffold(
        containerColor = StoreDarkBackground,
        bottomBar = {
            CooperativeBottomBar(
                selected = CooperativeDestination.Store,
                canOpenStore = true,
                canOpenCredential = true,
                onSelect = onBottomDestination,
                dark = true
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(StoreDarkBackground, StoreDarkBackground, StoreDarkSurface)
                    )
                )
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 118.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                StoreCatalogHeader(
                    cartCount = state.cartCount,
                    loggingOut = state.loggingOut,
                    onBack = onBack,
                    onOpenCart = onOpenCart,
                    onLogout = { confirmLogout = true }
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                StoreSearchField(
                    value = state.filters.search,
                    onValueChange = viewModel::search,
                    onSearch = viewModel::applySearch,
                    enabled = !state.loading
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                StoreCategoryChips(
                    selectedSlug = state.filters.categorySlug,
                    categories = state.catalog?.categories.orEmpty(),
                    onSelect = viewModel::selectCategory
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Message(state.message)
            }
            if (state.loading && state.catalog == null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LoadingSkeleton("Cargando productos")
                }
            }
            if (!state.loading && state.catalog?.products.orEmpty().isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(title = "Sin productos disponibles", message = "Cuando haya productos activos los veras en esta seccion.")
                }
            }
            items(state.catalog?.products.orEmpty(), key = { it.publicCode }) { product ->
                StoreGridProductCard(
                    product = product,
                    onOpenProduct = { onOpenProduct(product.publicCode) },
                    onAddToCart = { viewModel.addToCart(product) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreProductScreen(
    viewModel: StoreProductViewModel,
    publicCode: String,
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    onOpenCart: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(publicCode) { viewModel.load(publicCode) }
    LaunchedEffect(state.loggedOut) { if (state.loggedOut) onLoggedOut() }
    Scaffold(
        topBar = {
            CooperativeTopBar(title = stringResource(R.string.store_product_title), onBack = onBack)
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.loading) LoadingSkeleton("Cargando producto", Modifier.align(Alignment.CenterHorizontally))
            Message(state.message)
            state.product?.let { product ->
                AsyncImage(model = product.primaryImageUrl, contentDescription = product.name, modifier = Modifier.fillMaxWidth().height(220.dp), contentScale = ContentScale.Crop)
                Text(product.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                MoneyText(product.currency, product.effectivePrice)
                product.shortDescription?.let { Text(it) }
                Text(product.availabilityStatus, style = MaterialTheme.typography.bodySmall)
                if (product.variants.isNotEmpty()) {
                    Text(stringResource(R.string.store_variant), fontWeight = FontWeight.SemiBold)
                    product.variants.forEach { variant ->
                        FilterChip(
                            selected = state.selectedVariantPublicCode == variant.publicCode,
                            onClick = { viewModel.selectVariant(variant.publicCode) },
                            label = { Text("${variant.name} - ${product.currency} ${variant.effectivePrice}") }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { viewModel.setQuantity(state.quantity - 1) }) { Text("-") }
                    Text(state.quantity.toString(), style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = { viewModel.setQuantity(state.quantity + 1) }, enabled = state.quantity < product.maxQuantityPerOrder) { Text("+") }
                }
                state.disabledReason?.let { reason ->
                    if (!state.canAddToCart || reason == MaxQuantityReached) Text(text = stringResource(reason.messageRes), color = MaterialTheme.colorScheme.error)
                }
                PrimaryButton(text = if (state.adding) stringResource(R.string.store_adding_to_cart) else stringResource(R.string.store_add_to_cart), onClick = viewModel::addToCart, enabled = state.canAddToCart, modifier = Modifier.fillMaxWidth())
                if (state.added) {
                    Text(stringResource(R.string.store_added_to_cart))
                    SecondaryButton(text = stringResource(R.string.store_go_to_cart_with_count, state.cartCount), onClick = onOpenCart, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreCartScreen(viewModel: StoreCartViewModel, onBack: () -> Unit, onLoggedOut: () -> Unit, onCheckout: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var confirmClear by remember { mutableStateOf(false) }
    LaunchedEffect(state.loggedOut) { if (state.loggedOut) onLoggedOut() }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Vaciar carrito") },
            text = { Text("Se eliminarán los productos guardados en este dispositivo.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    viewModel.clear()
                }) { Text(stringResource(R.string.store_clear_cart)) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.register_back)) } }
        )
    }
    Scaffold(containerColor = StoreDarkBackground) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(StoreDarkBackground)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StoreFlowHeader(title = stringResource(R.string.store_cart_title), onBack = onBack)
            Message(state.message)
            if (state.lines.isEmpty()) {
                Text(stringResource(R.string.store_cart_empty), color = StoreMuted)
                return@Column
            }
            state.quote?.let { QuoteSummary(it, lines = state.lines) }
            state.lines.forEach { line ->
                CartLineCard(
                    line = line,
                    quoteItem = state.quote?.itemFor(line),
                    currency = state.quote?.currency.orEmpty(),
                    onQuantity = { viewModel.updateQuantity(line, it) },
                    onRemove = { viewModel.remove(line) }
                )
            }
            StoreDarkPrimaryButton(text = stringResource(R.string.store_checkout), onClick = onCheckout, enabled = !state.loadingQuote && state.quote != null)
            StoreDarkSecondaryButton(text = stringResource(R.string.store_clear_cart), onClick = { confirmClear = true })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreCheckoutScreen(viewModel: StoreCheckoutViewModel, onBack: () -> Unit, onLoggedOut: () -> Unit, onCreated: (String) -> Unit) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.loggedOut) { if (state.loggedOut) onLoggedOut() }
    LaunchedEffect(state.createdOrder?.code) { state.createdOrder?.code?.let(onCreated) }
    BackHandler(enabled = state.submitting) {}
    Scaffold(containerColor = StoreDarkBackground) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(StoreDarkBackground)
                .padding(padding)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StoreFlowHeader(
                    title = stringResource(R.string.store_checkout_title),
                    subtitle = "Flujo seguro de compra",
                    onBack = onBack,
                    action = {
                        StoreHeaderActionButton(
                            text = stringResource(R.string.store_update),
                            enabled = !state.loadingQuote,
                            onClick = viewModel::quote
                        )
                    }
                )
                Message(state.message)
                if (state.lines.isEmpty()) {
                    Text(stringResource(R.string.store_cart_empty), color = StoreMuted)
                    return@Column
                }
                StoreDarkSectionHeader("Paso 1", "Productos")
                state.quote?.let { QuoteSummary(it, compact = true, lines = state.lines) }
                StoreDarkSectionHeader("Paso 2", "Entrega")
                StoreDarkCard {
                    Field(
                        label = stringResource(R.string.store_coupon),
                        value = state.form.couponCode,
                        error = state.fieldErrors["coupon_code"],
                        normalization = TextInputNormalization.Coupon
                    ) { value -> viewModel.updateForm { copy(couponCode = value) } }
                }
                Text(stringResource(R.string.store_delivery), fontWeight = FontWeight.SemiBold, color = Color.White)
                DeliverySegmentedSelector(
                    selected = state.form.deliveryMethod,
                    onSelected = { method -> viewModel.updateForm { copy(deliveryMethod = method) } }
                )
                val instructions = if (state.form.deliveryMethod == "pickup") state.settings?.pickupInstructions else state.settings?.shippingInstructions
                instructions?.let {
                    Text(stringResource(if (state.form.deliveryMethod == "pickup") R.string.store_pickup_instructions else R.string.store_shipping_instructions, it), style = MaterialTheme.typography.bodySmall, color = StoreMuted)
                }
                if (state.form.deliveryMethod == "shipping") {
                    StoreDarkCard {
                        DestinationChoiceField(
                            label = stringResource(R.string.store_department),
                            value = state.form.department,
                            placeholder = stringResource(R.string.store_select_department),
                            options = state.deliveryDestinations.map { it.department },
                            error = state.fieldErrors["department"],
                            onSelected = { value -> viewModel.updateForm { copy(department = value) } }
                        )
                        if (state.form.hasConfiguredCities(state.deliveryDestinations)) {
                            DestinationChoiceField(
                                label = stringResource(R.string.store_city),
                                value = state.form.city,
                                placeholder = stringResource(R.string.store_select_city),
                                options = state.form.selectedDepartment(state.deliveryDestinations)?.cities.orEmpty().map { it.city },
                                error = state.fieldErrors["city"],
                                enabled = state.form.department.isNotBlank(),
                                onSelected = { value -> viewModel.updateForm { copy(city = value) } }
                            )
                        } else {
                            Field(stringResource(R.string.store_city), state.form.city, state.fieldErrors["city"]) { viewModel.updateForm { copy(city = it) } }
                        }
                        if (state.form.hasConfiguredZones(state.deliveryDestinations)) {
                            DestinationChoiceField(
                                label = stringResource(R.string.store_zone),
                                value = state.form.zone,
                                placeholder = stringResource(R.string.store_select_zone),
                                options = state.form.selectedCity(state.deliveryDestinations)?.zones.orEmpty().map { it.zone },
                                error = state.fieldErrors["zone"],
                                enabled = state.form.city.isNotBlank(),
                                onSelected = { value -> viewModel.updateForm { copy(zone = value) } }
                            )
                        } else {
                            Field(stringResource(R.string.store_zone), state.form.zone, state.fieldErrors["zone"]) { viewModel.updateForm { copy(zone = it) } }
                        }
                        Field(stringResource(R.string.store_delivery_address), state.form.deliveryAddress, state.fieldErrors["delivery_address"]) { viewModel.updateForm { copy(deliveryAddress = it) } }
                    }
                }
                StoreDarkSectionHeader("Paso 3", "Cupón y totales")
                if (state.loadingQuote) {
                    StoreDarkCard {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(22.dp), color = FigmaGold, strokeWidth = 2.dp)
                            Text(stringResource(R.string.store_quote_calculating), color = StoreMuted)
                        }
                    }
                } else {
                    state.quote?.let { CheckoutTotals(it) }
                        ?: Text(
                            text = if (state.fieldErrors["shipping"] != null) {
                                stringResource(R.string.store_shipping_rate_not_found)
                            } else {
                                stringResource(R.string.store_quote_required)
                            },
                            color = MaterialTheme.colorScheme.error
                        )
                }
                StoreDarkSectionHeader("Paso 4", "Confirmación")
                StoreDarkPrimaryButton(
                    text = stringResource(if (state.submitting) R.string.store_creating_order else R.string.store_create_order),
                    onClick = viewModel::submit,
                    enabled = state.canCreateOrder
                )
                StoreDarkSecondaryButton(text = stringResource(R.string.store_back_to_cart), onClick = onBack, enabled = !state.submitting)
            }
            if (state.submitting) {
                StoreProcessingOverlay(
                    title = stringResource(R.string.store_processing_title),
                    message = stringResource(R.string.store_processing_order_message)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreOrdersScreen(
    viewModel: StoreOrdersViewModel,
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    onOpenOrder: (String) -> Unit,
    onBottomDestination: (CooperativeDestination) -> Unit
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(state.loggedOut) { if (state.loggedOut) onLoggedOut() }
    Scaffold(
        containerColor = StoreDarkBackground,
        bottomBar = {
            CooperativeBottomBar(
                selected = CooperativeDestination.Orders,
                canOpenStore = true,
                canOpenCredential = true,
                onSelect = onBottomDestination
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(StoreDarkBackground)
                .padding(padding)
                .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StoreFlowHeader(
                title = stringResource(R.string.store_orders_title),
                onBack = onBack,
                action = {
                    StoreHeaderActionButton(
                        text = stringResource(R.string.store_update),
                        enabled = !state.loading,
                        onClick = { viewModel.load(force = true) }
                    )
                }
            )
            Message(state.message)
            if (state.loading && state.orders.isEmpty()) LoadingSkeleton("Cargando pedidos")
            if (!state.loading && state.orders.isEmpty()) EmptyState("Sin pedidos", "Tus pedidos de Mini tienda aparecerán aquí.")
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 108.dp)
            ) {
                items(state.orders, key = { it.code }) { order ->
                    Card(
                        onClick = { onOpenOrder(order.code) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = StoreDarkCardColor),
                        border = BorderStroke(1.dp, StoreBorder)
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.store_order_number, order.code), color = Color.White, fontWeight = FontWeight.SemiBold)
                            order.date?.let { Text(stringResource(R.string.store_order_date, formatStoreDate(it)), color = StoreMuted, style = MaterialTheme.typography.bodySmall) }
                            StoreOrderStatusBadge(order)
                            Text(stringResource(R.string.store_delivery_method, deliveryLabel(order.deliveryMethod)), color = StoreMuted, style = MaterialTheme.typography.bodySmall)
                            Text("${order.currency} ${order.total}", color = FigmaGold, fontWeight = FontWeight.Black)
                            order.itemSummary?.let { Text(it, color = StoreMuted, style = MaterialTheme.typography.bodySmall) }
                            StoreDarkSecondaryButton(text = stringResource(R.string.store_order_view), onClick = { onOpenOrder(order.code) })
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreOrderDetailScreen(viewModel: StoreOrderDetailViewModel, orderCode: String, onBack: () -> Unit, onLoggedOut: () -> Unit, onUploadReceipt: (String) -> Unit) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(orderCode) { viewModel.load(orderCode) }
    LaunchedEffect(state.loggedOut) { if (state.loggedOut) onLoggedOut() }
    LaunchedEffect(state.whatsappUrl) {
        val url = state.whatsappUrl ?: return@LaunchedEffect
        StoreWhatsappPolicy.validate(url)?.let { safeUrl ->
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl))) }
                .onFailure { if (it is ActivityNotFoundException) Unit }
        }
        viewModel.whatsappConsumed()
    }
    Scaffold(containerColor = StoreDarkBackground) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(StoreDarkBackground)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StoreFlowHeader(
                title = stringResource(R.string.store_order_detail_title),
                subtitle = orderCode,
                onBack = onBack,
                action = {
                    StoreHeaderActionButton(
                        text = stringResource(R.string.store_update),
                        enabled = !state.loading,
                        onClick = { viewModel.load(orderCode, force = true) }
                    )
                }
            )
            Message(state.message)
            if (state.loading && state.order == null) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            state.order?.let { order ->
                StoreDarkCard {
                    Text(stringResource(R.string.store_order_number, order.code), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White)
                    StoreOrderStatusBadge(order)
                    order.createdAt?.let { Text(stringResource(R.string.store_order_date, formatStoreDate(it)), style = MaterialTheme.typography.bodySmall, color = StoreMuted) }
                    Text(stringResource(R.string.store_delivery_method, deliveryLabel(order.deliveryMethod)), color = StoreMuted)
                    Text("${order.currency} ${order.total}", color = FigmaGold, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                }
                StoreDarkSectionHeader("Productos", "Detalle del pedido")
                order.items.forEach { item -> OrderItemCard(item, order.currency) }
                OrderTotals(order)
                order.payment?.message?.let {
                    StoreDarkSectionHeader(stringResource(R.string.store_payment_instructions), null)
                    StoreDarkCard { Text(storePaymentInstructionText(it), color = StoreMuted) }
                }
                if (order.isPaymentInReview()) {
                    StoreDarkCard {
                        Text(stringResource(R.string.store_payment_review_status), color = FigmaGold, fontWeight = FontWeight.Black)
                        Text(stringResource(R.string.store_payment_review_message), color = StoreMuted)
                    }
                }
                if (order.capabilities.canUploadReceipt) {
                    StoreDarkPrimaryButton(text = stringResource(R.string.store_upload_receipt), onClick = { onUploadReceipt(order.code) })
                }
                if (order.capabilities.canOpenWhatsapp) {
                    StoreDarkSecondaryButton(text = stringResource(R.string.store_open_whatsapp), onClick = viewModel::requestWhatsapp, enabled = !state.openingWhatsapp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreReceiptScreen(
    viewModel: StoreReceiptViewModel,
    orderCode: String,
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    onGoHome: () -> Unit,
    onViewOrders: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val receiptPreparer = remember(context) { ReceiptPreparer(context) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.setReceipt(receiptPreparer.prepare(uri))
    }
    LaunchedEffect(state.loggedOut) { if (state.loggedOut) onLoggedOut() }
    BackHandler(enabled = state.submitting) {}
    BackHandler(enabled = state.submittedOrder != null) { onViewOrders() }
    Scaffold(containerColor = StoreDarkBackground) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(StoreDarkBackground)
                .padding(padding)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                state.submittedOrder?.let { order ->
                    StoreReceiptSuccess(order = order, onGoHome = onGoHome, onViewOrders = onViewOrders)
                    return@Column
                }
                StoreFlowHeader(title = stringResource(R.string.store_receipt_title), subtitle = stringResource(R.string.store_receipt_order, orderCode), onBack = onBack)
                StoreDarkCard {
                    Text("Si ya realizaste el pago, adjunta tu comprobante para su validación.", color = StoreMuted)
                }
                Message(state.message)
                StoreDarkSecondaryButton(text = stringResource(R.string.payment_pick_receipt), onClick = { launcher.launch(arrayOf("image/jpeg", "image/png", "image/webp", "application/pdf")) })
                state.receipt?.let {
                    StoreDarkCard {
                        Text(it.displayName, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("${it.sizeBytes / 1024} KB", color = StoreMuted)
                    }
                }
                state.fieldError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                StoreDarkPrimaryButton(text = stringResource(R.string.store_upload_receipt), onClick = { viewModel.submit(orderCode) }, enabled = !state.submitting && state.receipt != null)
            }
            if (state.submitting) {
                StoreProcessingOverlay(
                    title = stringResource(R.string.store_processing_title),
                    message = stringResource(R.string.store_processing_receipt_message)
                )
            }
        }
    }
}

@Composable
private fun StoreReceiptSuccess(order: StoreOrder, onGoHome: () -> Unit, onViewOrders: () -> Unit) {
    StoreFlowHeader(title = stringResource(R.string.store_purchase_success_header), subtitle = order.code, onBack = onViewOrders)
    StoreDarkCard {
        Text("✓", color = FigmaGold, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(stringResource(R.string.store_purchase_success_title), color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(stringResource(R.string.store_purchase_receipt_sent), color = FigmaGold, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(stringResource(R.string.store_purchase_receipt_review), color = StoreMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(stringResource(R.string.store_receipt_order, order.code), color = Color.White, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(stringResource(R.string.store_purchase_delivery_window), color = StoreMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(stringResource(R.string.store_purchase_check_orders), color = StoreMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
    StoreDarkPrimaryButton(text = stringResource(R.string.store_go_home), onClick = onGoHome)
    StoreDarkSecondaryButton(text = stringResource(R.string.store_view_my_orders), onClick = onViewOrders)
}

@Composable
private fun StoreCatalogHeader(
    cartCount: Int,
    loggingOut: Boolean,
    onBack: () -> Unit,
    onOpenCart: () -> Unit,
    onLogout: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StoreRoundIconButton(iconRes = R.drawable.ic_arrow_back, contentDescription = "Volver", onClick = onBack)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("SIAFCO", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text("TIENDA VIRTUAL", color = FigmaGold, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        }
        StoreCartButton(cartCount = cartCount, onClick = onOpenCart)
        StoreRoundIconButton(
            iconRes = R.drawable.ic_store_logout,
            contentDescription = stringResource(R.string.home_logout),
            enabled = !loggingOut,
            onClick = onLogout
        )
    }
}

@Composable
private fun StoreRoundIconButton(
    iconRes: Int,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(58.dp)
            .background(StoreDarkChip, CircleShape)
            .border(1.dp, StoreBorder, CircleShape)
    ) {
        androidx.compose.material3.Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = if (enabled) FigmaGold else Color.White.copy(alpha = 0.35f),
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun StoreCartButton(cartCount: Int, onClick: () -> Unit) {
    Box {
        StoreRoundIconButton(iconRes = R.drawable.ic_store_cart, contentDescription = stringResource(R.string.store_cart_title), onClick = onClick)
        if (cartCount > 0) {
            Text(
                text = cartCount.coerceAtMost(99).toString(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(FigmaGold, CircleShape)
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                color = FigmaNavy,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun StoreFlowHeader(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    action: (@Composable () -> Unit)? = null
) {
    val locale = LocalLocale.current.platformLocale
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StoreRoundIconButton(iconRes = R.drawable.ic_arrow_back, contentDescription = "Volver", onClick = onBack)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("SIAFCO", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(title.uppercase(locale), color = FigmaGold, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            subtitle?.let { Text(it, color = StoreMuted, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
        action?.invoke()
    }
}

@Composable
private fun StoreHeaderActionButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (enabled) FigmaGold else StoreBorder),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = FigmaGold,
            disabledContentColor = StoreMuted
        )
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_store_refresh),
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(text, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun DeliverySegmentedSelector(selected: String, onSelected: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DeliverySegment(
            text = stringResource(R.string.store_pickup),
            selected = selected == "pickup",
            onClick = { onSelected("pickup") },
            modifier = Modifier.weight(1f)
        )
        DeliverySegment(
            text = stringResource(R.string.store_shipping),
            selected = selected == "shipping",
            onClick = { onSelected("shipping") },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DeliverySegment(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val locale = LocalLocale.current.platformLocale
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(shape)
            .background(if (selected) FigmaGold else StoreDarkChip, shape)
            .border(1.dp, if (selected) FigmaGold else StoreBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(locale),
            color = if (selected) FigmaNavy else Color.White,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StoreDarkSectionHeader(title: String, subtitle: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        subtitle?.let { Text(it, color = StoreMuted, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun StoreDarkCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = StoreDarkCardColor),
        border = BorderStroke(1.dp, StoreBorder)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun StoreProcessingOverlay(title: String, message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.58f))
            .clickable(enabled = true, onClick = {}),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(color = FigmaGold, strokeWidth = 4.dp)
                Text(
                    text = title,
                    color = FigmaNavyDeep,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = message,
                    color = FigmaNavy,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun StoreDarkPrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = FigmaGold,
            contentColor = FigmaNavy,
            disabledContainerColor = StoreBorder,
            disabledContentColor = StoreMuted
        )
    ) {
        Text(text, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun StoreDarkSecondaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (enabled) FigmaGold else StoreBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (enabled) StoreDarkChip else Color.Transparent,
            contentColor = FigmaGold,
            disabledContentColor = StoreMuted
        )
    ) {
        Text(text, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun StoreOrderStatusBadge(order: StoreOrder) {
    val color = when (order.status) {
        "pendiente", "reservado", "esperando_pago" -> FigmaGold
        "pago_en_revision" -> Color(0xFF7DD3FC)
        "confirmado", "enviado", "entregado" -> Color(0xFF86EFAC)
        "cancelado" -> Color(0xFFFCA5A5)
        else -> StoreMuted
    }
    Text(
        text = order.statusLabel,
        modifier = Modifier
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
            .border(1.dp, color.copy(alpha = 0.46f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = color,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Black
    )
}

@Composable
private fun StoreSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    enabled: Boolean
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.store_search_placeholder), color = StoreMuted) },
        singleLine = true,
        leadingIcon = {
            androidx.compose.material3.Icon(
                painter = painterResource(R.drawable.ic_store_search),
                contentDescription = null,
                tint = StoreMuted
            )
        },
        trailingIcon = {
            TextButton(onClick = onSearch, enabled = enabled) {
                Text(stringResource(R.string.store_update), color = FigmaGold, fontWeight = FontWeight.Black)
            }
        },
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = StoreDarkChip,
            unfocusedContainerColor = StoreDarkChip,
            disabledContainerColor = StoreDarkChip,
            focusedIndicatorColor = StoreBorder,
            unfocusedIndicatorColor = StoreBorder,
            cursorColor = FigmaGold,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        )
    )
}

@Composable
private fun StoreCategoryChips(
    selectedSlug: String?,
    categories: List<StoreCategory>,
    onSelect: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StoreCategoryChip(text = stringResource(R.string.store_all), selected = selectedSlug == null, onClick = { onSelect(null) })
        categories.forEach { category ->
            StoreCategoryChip(text = category.name, selected = selectedSlug == category.slug, onClick = { onSelect(category.slug) })
        }
    }
}

@Composable
private fun StoreCategoryChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        modifier = Modifier
            .background(if (selected) FigmaGold else StoreDarkChip, RoundedCornerShape(24.dp))
            .border(1.dp, if (selected) FigmaGold else StoreBorder, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 13.dp),
        color = if (selected) FigmaNavy else Color.White,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Black,
        maxLines = 1
    )
}

@Composable
private fun StoreGridProductCard(
    product: StoreProduct,
    onOpenProduct: () -> Unit,
    onAddToCart: () -> Unit
) {
    val canAddDirectly = product.canAddToCart(selectedVariantPublicCode = null, quantity = 1)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenProduct),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = StoreDarkCardColor),
        border = BorderStroke(1.dp, StoreBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.16f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                if (product.primaryImageUrl.isNullOrBlank()) {
                    Image(
                        painter = painterResource(R.drawable.brand_logo),
                        contentDescription = stringResource(R.string.brand_logo_content_description),
                        modifier = Modifier.size(72.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    AsyncImage(
                        model = product.primaryImageUrl,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Text(
                text = product.name,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2
            )
            Text(
                text = "${product.currency.ifBlank { "BOB" }} ${product.effectivePrice}",
                color = FigmaGold,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            if (!product.isAvailable) {
                Text(
                    text = product.availabilityStatus,
                    color = StoreMuted,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Button(
                onClick = {
                    if (canAddDirectly) onAddToCart() else onOpenProduct()
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                enabled = product.isAvailable,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FigmaGold, contentColor = FigmaNavy),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                androidx.compose.material3.Icon(
                    painter = painterResource(R.drawable.ic_store_add),
                    contentDescription = null,
                    modifier = Modifier.size(21.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(if (canAddDirectly) R.string.store_add_short else R.string.store_view_product),
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ProductCard(product: StoreProduct, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = product.primaryImageUrl, contentDescription = product.name, modifier = Modifier.size(88.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(product.name, fontWeight = FontWeight.SemiBold)
                product.category?.let { Text(it.name, style = MaterialTheme.typography.bodySmall) }
                Text("${product.currency} ${product.effectivePrice}", color = MaterialTheme.colorScheme.primary)
                Text(product.availabilityStatus, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun CartLineCard(line: StoreCartLine, quoteItem: StoreQuoteItem?, currency: String, onQuantity: (Int) -> Unit, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = StoreDarkCardColor),
        border = BorderStroke(1.dp, StoreBorder)
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            StoreThumbnail(imageUrl = line.imageUrl, contentDescription = quoteItem?.productName ?: line.productPublicCode)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(quoteItem?.productName ?: stringResource(R.string.store_quote_required), color = Color.White, fontWeight = FontWeight.Black)
                quoteItem?.variantName?.let { Text(stringResource(R.string.store_variant_value, it), color = StoreMuted, style = MaterialTheme.typography.bodySmall) }
                quoteItem?.let {
                    Text(stringResource(R.string.store_line_quantity_price, it.quantity, currency.ifBlank { "BOB" }, it.unitPrice), color = StoreMuted)
                    Text(stringResource(R.string.store_line_subtotal, currency.ifBlank { "BOB" }, it.lineTotal), color = FigmaGold, fontWeight = FontWeight.Black)
                } ?: Text(stringResource(R.string.store_line_quantity, line.quantity), color = StoreMuted, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { onQuantity(line.quantity - 1) }, border = BorderStroke(1.dp, FigmaGold)) { Text("-", color = FigmaGold) }
                    Text(line.quantity.toString(), color = Color.White, fontWeight = FontWeight.Bold)
                    OutlinedButton(onClick = { onQuantity(line.quantity + 1) }, border = BorderStroke(1.dp, FigmaGold)) { Text("+", color = FigmaGold) }
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(onClick = onRemove, border = BorderStroke(1.dp, StoreBorder)) { Text(stringResource(R.string.store_remove), color = Color.White) }
                }
            }
        }
    }
}

@Composable
private fun QuoteSummary(quote: StoreQuote, compact: Boolean = false, lines: List<StoreCartLine> = emptyList()) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = StoreDarkCardColor),
        border = BorderStroke(1.dp, StoreBorder)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.store_quote).uppercase(), color = Color.White, fontWeight = FontWeight.Black)
            quote.items.forEach { item ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    StoreThumbnail(
                        imageUrl = item.imageUrlFrom(lines),
                        contentDescription = item.productName
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.productName, color = Color.White, fontWeight = FontWeight.Black)
                        item.variantName?.let { Text(stringResource(R.string.store_variant_value, it), color = StoreMuted, style = MaterialTheme.typography.bodySmall) }
                        Text(stringResource(R.string.store_line_quantity_price, item.quantity, quote.currency, item.unitPrice), color = StoreMuted)
                        Text(stringResource(R.string.store_line_subtotal, quote.currency, item.lineTotal), color = FigmaGold, fontWeight = FontWeight.Black)
                    }
                }
                if (!compact) Spacer(Modifier.height(4.dp))
            }
            Text("${stringResource(R.string.store_subtotal)}: ${quote.currency} ${quote.subtotal}", color = StoreMuted)
            if (quote.discountTotal != "0.00") Text("${stringResource(R.string.store_discount)}: ${quote.currency} ${quote.discountTotal}", color = StoreMuted)
            Text("${stringResource(R.string.store_shipping_cost)}: ${quote.currency} ${quote.shippingTotal}", color = StoreMuted)
            Text("${stringResource(R.string.store_total)}: ${quote.currency} ${quote.total}", color = FigmaGold, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun CheckoutTotals(quote: StoreQuote) {
    val locale = LocalLocale.current.platformLocale
    StoreDarkCard {
        CheckoutTotalRow(label = stringResource(R.string.store_subtotal), value = "${quote.currency} ${quote.subtotal}")
        CheckoutTotalRow(label = stringResource(R.string.store_shipping_cost), value = "${quote.currency} ${quote.shippingTotal}")
        if (quote.discountTotal != "0.00") {
            CheckoutTotalRow(label = stringResource(R.string.store_discount), value = "${quote.currency} ${quote.discountTotal}")
        }
        CheckoutTotalRow(
            label = stringResource(R.string.store_total).uppercase(locale),
            value = "${quote.currency} ${quote.total}",
            highlight = true
        )
    }
}

@Composable
private fun CheckoutTotalRow(label: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = if (highlight) FigmaGold else StoreMuted, fontWeight = if (highlight) FontWeight.Black else FontWeight.Normal)
        Text(value, color = if (highlight) FigmaGold else Color.White, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun StoreThumbnail(imageUrl: String? = null, contentDescription: String? = null) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isNullOrBlank()) {
            androidx.compose.material3.Icon(
                painter = painterResource(R.drawable.ic_nav_store),
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

private fun StoreQuoteItem.imageUrlFrom(lines: List<StoreCartLine>): String? =
    lines.firstOrNull {
        it.productPublicCode == productPublicCode && it.variantPublicCode == variantPublicCode
    }?.imageUrl ?: lines.firstOrNull { it.productPublicCode == productPublicCode }?.imageUrl

@Composable
private fun OrderItemCard(item: StoreOrderItem, currency: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = StoreDarkCardColor),
        border = BorderStroke(1.dp, StoreBorder)
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            StoreThumbnail(imageUrl = item.primaryImageUrl, contentDescription = item.name)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.name, color = Color.White, fontWeight = FontWeight.Black)
                item.variant?.let { Text(stringResource(R.string.store_variant_value, it), color = StoreMuted, style = MaterialTheme.typography.bodySmall) }
                Text(stringResource(R.string.store_line_quantity_price, item.quantity, currency, item.unitPrice), color = StoreMuted)
                Text(stringResource(R.string.store_line_subtotal, currency, item.lineTotal), color = FigmaGold, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun OrderTotals(order: StoreOrder) {
    StoreDarkCard {
        order.subtotal?.let { Text("${stringResource(R.string.store_subtotal)}: ${order.currency} $it", color = StoreMuted) }
        order.discountTotal?.takeIf { it != "0.00" }?.let { Text("${stringResource(R.string.store_discount)}: ${order.currency} $it", color = StoreMuted) }
        order.shippingTotal?.let { Text("${stringResource(R.string.store_shipping_cost)}: ${order.currency} $it", color = StoreMuted) }
        Text("${stringResource(R.string.store_total)}: ${order.currency} ${order.total}", color = FigmaGold, fontWeight = FontWeight.Black)
    }
}

private fun StoreOrder.isPaymentInReview(): Boolean =
    status == "pago_en_revision" || receipts.any { it.status == "pending" }

@Composable
private fun Message(message: UiMessage?) {
    message?.let { Text(stringResource(it.resId), color = MaterialTheme.colorScheme.error) }
}

@Composable
private fun DestinationChoiceField(
    label: String,
    value: String,
    placeholder: String,
    options: List<String>,
    error: String?,
    enabled: Boolean = true,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = StoreFieldLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        OutlinedButton(
            onClick = { if (enabled && options.isNotEmpty()) expanded = true },
            enabled = enabled && options.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (error == null) FigmaGold else StoreFieldError),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = StoreFieldBackground,
                contentColor = FigmaGold,
                disabledContainerColor = StoreFieldBackground,
                disabledContentColor = StoreMuted
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = value.ifBlank { placeholder },
                modifier = Modifier.weight(1f),
                color = if (value.isBlank()) StoreFieldPlaceholder else Color.White,
                textAlign = TextAlign.Start
            )
            Text("▾", color = if (enabled && options.isNotEmpty()) FigmaGold else StoreMuted)
        }
        error?.let { Text(it, color = StoreFieldError, style = MaterialTheme.typography.bodySmall) }
    }

    if (expanded) {
        AlertDialog(
            onDismissRequest = { expanded = false },
            title = { Text(label, fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { option ->
                        TextButton(
                            onClick = {
                                expanded = false
                                onSelected(option)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(option, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { expanded = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    error: String?,
    normalization: TextInputNormalization = TextInputNormalization.Human,
    onChange: (String) -> Unit
) {
    var fieldValue by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    LaunchedEffect(value) {
        if (value != fieldValue.text) {
            fieldValue = fieldValue.copy(text = value, selection = TextRange(value.length), composition = null)
        }
    }

    val keyboardOptions = when (normalization) {
        TextInputNormalization.Human,
        TextInputNormalization.Coupon -> KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Characters)
        TextInputNormalization.None -> KeyboardOptions.Default
    }

    OutlinedTextField(
        value = fieldValue,
        onValueChange = { incoming ->
            val normalized = HumanTextInputNormalizer.visual(incoming, normalization)
            fieldValue = normalized
            if (normalized.text != value) onChange(normalized.text)
        },
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp),
        isError = error != null,
        supportingText = { error?.let { Text(it, color = StoreFieldError) } },
        singleLine = true,
        keyboardOptions = keyboardOptions,
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            disabledTextColor = StoreMuted,
            errorTextColor = Color.White,
            focusedContainerColor = StoreFieldBackground,
            unfocusedContainerColor = StoreFieldBackground,
            disabledContainerColor = StoreFieldBackground,
            errorContainerColor = StoreFieldBackground,
            cursorColor = FigmaGold,
            errorCursorColor = StoreFieldError,
            focusedIndicatorColor = FigmaGold,
            unfocusedIndicatorColor = StoreFieldBorder,
            disabledIndicatorColor = StoreFieldBorder,
            errorIndicatorColor = StoreFieldError,
            focusedLabelColor = FigmaGold,
            unfocusedLabelColor = StoreFieldLabel,
            disabledLabelColor = StoreMuted,
            errorLabelColor = StoreFieldError
        )
    )
}

private val StoreProductDisabledReason.messageRes: Int
    get() = when (this) {
        SoldOut -> R.string.store_unavailable_sold_out
        ComingSoon -> R.string.store_unavailable_coming_soon
        SelectVariant -> R.string.store_unavailable_select_variant
        Unavailable -> R.string.store_unavailable_generic
        MaxQuantityReached -> R.string.store_unavailable_max_quantity
    }

private fun StoreQuote.itemFor(line: StoreCartLine): StoreQuoteItem? =
    items.firstOrNull { it.productPublicCode == line.productPublicCode && it.variantPublicCode == line.variantPublicCode }

private fun deliveryLabel(method: String): String = when (method) {
    "pickup" -> "Recojo"
    "shipping" -> "Envío"
    else -> method
}

private fun formatStoreDate(value: String): String =
    runCatching {
        OffsetDateTime.parse(value).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
    }.getOrElse { value.take(16).replace('T', ' ') }

private fun storePaymentInstructionText(value: String): String =
    if (value.contains("fase posterior", ignoreCase = true)) {
        "Si ya realizaste el pago, adjunta tu comprobante para su validación."
    } else {
        value
    }

private val StoreDarkBackground = Color(0xFF061D3A)
private val StoreDarkSurface = Color(0xFF09284D)
private val StoreDarkCardColor = Color(0xFF0B315F)
private val StoreDarkChip = Color(0xFF102F57)
private val StoreBorder = Color(0xFF244B76)
private val StoreMuted = Color(0xFFAAB7C8)
private val StoreFieldBackground = Color(0xFF103A6D)
private val StoreFieldBorder = Color(0xFF4D79A8)
private val StoreFieldLabel = Color(0xFFD4E1F1)
private val StoreFieldPlaceholder = Color(0xFFAAB7C8)
private val StoreFieldError = Color(0xFFFF9A9A)
