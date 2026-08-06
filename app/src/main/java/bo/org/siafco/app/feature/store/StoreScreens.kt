package bo.org.siafco.app.feature.store

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import bo.org.siafco.app.R
import bo.org.siafco.app.core.text.TextInputNormalization
import bo.org.siafco.app.core.ui.CooperativeBottomBar
import bo.org.siafco.app.core.ui.CooperativeDestination
import bo.org.siafco.app.core.ui.CooperativeSpacing
import bo.org.siafco.app.core.ui.CooperativeTextSecondary
import bo.org.siafco.app.core.ui.CooperativeTopBar
import bo.org.siafco.app.core.ui.EmptyState
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
    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(state.loggedOut) { if (state.loggedOut) onLoggedOut() }
    Scaffold(
        topBar = {
            CooperativeTopBar(
                title = stringResource(R.string.store_title),
                onBack = onBack,
                actions = { SecondaryButton(text = "Carrito", onClick = onOpenCart) }
            )
        },
        bottomBar = {
            CooperativeBottomBar(
                selected = CooperativeDestination.Store,
                canOpenStore = true,
                canOpenCredential = true,
                onSelect = onBottomDestination
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(title = stringResource(R.string.store_title), subtitle = "Beneficios y productos para afiliados")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.filters.search,
                    onValueChange = viewModel::search,
                    label = { Text(stringResource(R.string.store_search)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                PrimaryButton(text = stringResource(R.string.store_update), onClick = viewModel::applySearch, enabled = !state.loading)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.filters.categorySlug == null, onClick = { viewModel.selectCategory(null) }, label = { Text(stringResource(R.string.store_all)) })
                state.catalog?.categories.orEmpty().take(3).forEach { category ->
                    FilterChip(selected = state.filters.categorySlug == category.slug, onClick = { viewModel.selectCategory(category.slug) }, label = { Text(category.name) })
                }
            }
            Message(state.message)
            if (state.loading && state.catalog == null) LoadingSkeleton("Cargando productos")
            if (!state.loading && state.catalog?.products.orEmpty().isEmpty()) {
                EmptyState(title = "Sin productos disponibles", message = "Cuando haya productos activos los verás en esta sección.")
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.catalog?.products.orEmpty(), key = { it.publicCode }) { product ->
                    ProductCard(product = product, onClick = { onOpenProduct(product.publicCode) })
                }
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
    Scaffold(topBar = { CooperativeTopBar(title = stringResource(R.string.store_cart_title), onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Message(state.message)
            if (state.lines.isEmpty()) {
                Text(stringResource(R.string.store_cart_empty))
                return@Column
            }
            state.quote?.let { QuoteSummary(it) }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f, fill = false)) {
                items(state.lines, key = { it.key }) { line ->
                    CartLineCard(
                        line = line,
                        quoteItem = state.quote?.itemFor(line),
                        currency = state.quote?.currency.orEmpty(),
                        onQuantity = { viewModel.updateQuantity(line, it) },
                        onRemove = { viewModel.remove(line) }
                    )
                }
            }
            PrimaryButton(text = stringResource(R.string.store_checkout), onClick = onCheckout, modifier = Modifier.fillMaxWidth(), enabled = !state.loadingQuote && state.quote != null)
            SecondaryButton(text = stringResource(R.string.store_clear_cart), onClick = { confirmClear = true }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreCheckoutScreen(viewModel: StoreCheckoutViewModel, onBack: () -> Unit, onLoggedOut: () -> Unit, onCreated: (String) -> Unit) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.loggedOut) { if (state.loggedOut) onLoggedOut() }
    LaunchedEffect(state.createdOrder?.code) { state.createdOrder?.code?.let(onCreated) }
    Scaffold(topBar = { CooperativeTopBar(title = stringResource(R.string.store_checkout_title), onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(stringResource(R.string.store_checkout_title), "Flujo seguro de compra")
            Message(state.message)
            if (state.lines.isEmpty()) {
                Text(stringResource(R.string.store_cart_empty))
                return@Column
            }
            if (state.loadingQuote) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            SectionHeader("Paso 1", "Productos")
            state.quote?.let { QuoteSummary(it, compact = true) }
            SectionHeader("Paso 2", "Entrega")
            NormalizedTextField(
                value = state.form.couponCode,
                onValueChange = { value -> viewModel.updateForm { copy(couponCode = value) } },
                label = { Text(stringResource(R.string.store_coupon)) },
                modifier = Modifier.fillMaxWidth(),
                normalization = TextInputNormalization.Coupon
            )
            Text(stringResource(R.string.store_delivery), fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.form.deliveryMethod == "pickup", onClick = { viewModel.updateForm { copy(deliveryMethod = "pickup") } }, label = { Text(stringResource(R.string.store_pickup)) })
                FilterChip(selected = state.form.deliveryMethod == "shipping", onClick = { viewModel.updateForm { copy(deliveryMethod = "shipping") } }, label = { Text(stringResource(R.string.store_shipping)) })
            }
            val instructions = if (state.form.deliveryMethod == "pickup") state.settings?.pickupInstructions else state.settings?.shippingInstructions
            instructions?.let {
                Text(stringResource(if (state.form.deliveryMethod == "pickup") R.string.store_pickup_instructions else R.string.store_shipping_instructions, it), style = MaterialTheme.typography.bodySmall)
            }
            if (state.form.deliveryMethod == "shipping") {
                Field("department", state.form.department, state.fieldErrors) { viewModel.updateForm { copy(department = it) } }
                Field("city", state.form.city, state.fieldErrors) { viewModel.updateForm { copy(city = it) } }
                Field("zone", state.form.zone, state.fieldErrors) { viewModel.updateForm { copy(zone = it) } }
                Field("delivery_address", state.form.deliveryAddress, state.fieldErrors) { viewModel.updateForm { copy(deliveryAddress = it) } }
            }
            SectionHeader("Paso 3", "Cupón y totales")
            if (state.quote == null && !state.loadingQuote) Text(stringResource(R.string.store_quote_required), color = MaterialTheme.colorScheme.error)
            SectionHeader("Paso 4", "Confirmación")
            PrimaryButton(text = stringResource(R.string.store_create_order), onClick = viewModel::submit, enabled = state.canCreateOrder, modifier = Modifier.fillMaxWidth())
            if (state.submitting) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
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
        topBar = { CooperativeTopBar(title = stringResource(R.string.store_orders_title), onBack = onBack, actions = { SecondaryButton(text = stringResource(R.string.store_update), onClick = { viewModel.load(force = true) }) }) },
        bottomBar = {
            CooperativeBottomBar(
                selected = CooperativeDestination.Orders,
                canOpenStore = true,
                canOpenCredential = true,
                onSelect = onBottomDestination
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Message(state.message)
            if (state.loading && state.orders.isEmpty()) LoadingSkeleton("Cargando pedidos")
            if (!state.loading && state.orders.isEmpty()) EmptyState("Sin pedidos", "Tus pedidos de Mini tienda aparecerán aquí.")
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.orders, key = { it.code }) { order ->
                    Card(onClick = { onOpenOrder(order.code) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.store_order_number, order.code), fontWeight = FontWeight.SemiBold)
                            order.date?.let { Text(stringResource(R.string.store_order_date, it), style = MaterialTheme.typography.bodySmall) }
                            StatusBadge(order.statusLabel)
                            Text(stringResource(R.string.store_delivery_method, deliveryLabel(order.deliveryMethod)), style = MaterialTheme.typography.bodySmall)
                            MoneyText(order.currency, order.total)
                            order.itemSummary?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            SecondaryButton(text = stringResource(R.string.store_order_view), onClick = { onOpenOrder(order.code) }, modifier = Modifier.fillMaxWidth())
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
    Scaffold(topBar = { CooperativeTopBar(title = stringResource(R.string.store_order_detail_title), onBack = onBack, actions = { SecondaryButton(text = stringResource(R.string.store_update), onClick = { viewModel.load(orderCode, force = true) }) }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Message(state.message)
            if (state.loading && state.order == null) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            state.order?.let { order ->
                Text(stringResource(R.string.store_order_number, order.code), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(order.statusLabel)
                order.createdAt?.let { Text(stringResource(R.string.store_order_date, it), style = MaterialTheme.typography.bodySmall) }
                Text(stringResource(R.string.store_delivery_method, deliveryLabel(order.deliveryMethod)))
                MoneyText(order.currency, order.total)
                order.items.forEach { item -> OrderItemCard(item, order.currency) }
                OrderTotals(order)
                order.payment?.message?.let {
                    Text(stringResource(R.string.store_payment_instructions), fontWeight = FontWeight.SemiBold)
                    Text(it)
                }
                if (order.capabilities.canUploadReceipt) {
                    PrimaryButton(text = stringResource(R.string.store_upload_receipt), onClick = { onUploadReceipt(order.code) }, modifier = Modifier.fillMaxWidth())
                }
                if (order.capabilities.canOpenWhatsapp) {
                    SecondaryButton(text = stringResource(R.string.store_open_whatsapp), onClick = viewModel::requestWhatsapp, enabled = !state.openingWhatsapp, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreReceiptScreen(viewModel: StoreReceiptViewModel, orderCode: String, onBack: () -> Unit, onLoggedOut: () -> Unit, onSubmitted: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val receiptPreparer = remember(context) { ReceiptPreparer(context) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.setReceipt(receiptPreparer.prepare(uri))
    }
    LaunchedEffect(state.loggedOut) { if (state.loggedOut) onLoggedOut() }
    LaunchedEffect(state.submittedOrder) { if (state.submittedOrder != null) onSubmitted() }
    Scaffold(topBar = { CooperativeTopBar(title = stringResource(R.string.store_receipt_title), onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.store_receipt_order, orderCode), fontWeight = FontWeight.SemiBold)
            Message(state.message)
            SecondaryButton(text = stringResource(R.string.payment_pick_receipt), onClick = { launcher.launch(arrayOf("image/jpeg", "image/png", "image/webp", "application/pdf")) }, modifier = Modifier.fillMaxWidth())
            state.receipt?.let { Text("${it.displayName} - ${it.sizeBytes / 1024} KB") }
            state.fieldError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            PrimaryButton(text = stringResource(R.string.store_upload_receipt), onClick = { viewModel.submit(orderCode) }, enabled = !state.submitting && state.receipt != null, modifier = Modifier.fillMaxWidth())
            if (state.submitting) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
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
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            StoreThumbnail()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(quoteItem?.productName ?: stringResource(R.string.store_quote_required), fontWeight = FontWeight.SemiBold)
                quoteItem?.variantName?.let { Text(stringResource(R.string.store_variant_value, it), style = MaterialTheme.typography.bodySmall) }
                quoteItem?.let {
                    Text(stringResource(R.string.store_line_quantity_price, it.quantity, currency.ifBlank { "BOB" }, it.unitPrice))
                    Text(stringResource(R.string.store_line_subtotal, currency.ifBlank { "BOB" }, it.lineTotal), fontWeight = FontWeight.SemiBold)
                } ?: Text(stringResource(R.string.store_line_quantity, line.quantity), style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { onQuantity(line.quantity - 1) }) { Text("-") }
                    Text(line.quantity.toString())
                    OutlinedButton(onClick = { onQuantity(line.quantity + 1) }) { Text("+") }
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(onClick = onRemove) { Text(stringResource(R.string.store_remove)) }
                }
            }
        }
    }
}

@Composable
private fun QuoteSummary(quote: StoreQuote, compact: Boolean = false) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.store_quote).uppercase(), fontWeight = FontWeight.SemiBold)
            quote.items.forEach { item ->
                Text(item.productName, fontWeight = FontWeight.SemiBold)
                item.variantName?.let { Text(stringResource(R.string.store_variant_value, it), style = MaterialTheme.typography.bodySmall) }
                Text(stringResource(R.string.store_line_quantity, item.quantity))
                Text(stringResource(R.string.store_line_price, quote.currency, item.unitPrice))
                Text(stringResource(R.string.store_line_subtotal, quote.currency, item.lineTotal))
                if (!compact) Spacer(Modifier.height(4.dp))
            }
            Text("${stringResource(R.string.store_subtotal)}: ${quote.currency} ${quote.subtotal}")
            if (quote.discountTotal != "0.00") Text("${stringResource(R.string.store_discount)}: ${quote.currency} ${quote.discountTotal}")
            Text("${stringResource(R.string.store_shipping_cost)}: ${quote.currency} ${quote.shippingTotal}")
            Text("${stringResource(R.string.store_total)}: ${quote.currency} ${quote.total}", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StoreThumbnail() {
    Image(
        painter = painterResource(R.drawable.brand_logo),
        contentDescription = stringResource(R.string.brand_logo_content_description),
        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)),
        contentScale = ContentScale.Fit
    )
}

@Composable
private fun OrderItemCard(item: StoreOrderItem, currency: String) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            StoreThumbnail()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.name, fontWeight = FontWeight.SemiBold)
                item.variant?.let { Text(stringResource(R.string.store_variant_value, it), style = MaterialTheme.typography.bodySmall) }
                Text(stringResource(R.string.store_line_quantity_price, item.quantity, currency, item.unitPrice))
                Text(stringResource(R.string.store_line_subtotal, currency, item.lineTotal), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun OrderTotals(order: StoreOrder) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        order.subtotal?.let { Text("${stringResource(R.string.store_subtotal)}: ${order.currency} $it") }
        order.discountTotal?.takeIf { it != "0.00" }?.let { Text("${stringResource(R.string.store_discount)}: ${order.currency} $it") }
        order.shippingTotal?.let { Text("${stringResource(R.string.store_shipping_cost)}: ${order.currency} $it") }
        Text("${stringResource(R.string.store_total)}: ${order.currency} ${order.total}", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Message(message: UiMessage?) {
    message?.let { Text(stringResource(it.resId), color = MaterialTheme.colorScheme.error) }
}

@Composable
private fun Field(field: String, value: String, errors: Map<String, String>, onChange: (String) -> Unit) {
    NormalizedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(field.replace('_', ' ')) },
        modifier = Modifier.fillMaxWidth(),
        isError = errors.containsKey(field),
        supportingText = { errors[field]?.let { Text(it) } },
        normalization = TextInputNormalization.Human
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
