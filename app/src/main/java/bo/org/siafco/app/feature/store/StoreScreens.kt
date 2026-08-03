package bo.org.siafco.app.feature.store

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import bo.org.siafco.app.R
import bo.org.siafco.app.core.text.TextInputNormalization
import bo.org.siafco.app.core.ui.NormalizedTextField
import bo.org.siafco.app.domain.StoreCartLine
import bo.org.siafco.app.domain.StoreProduct
import bo.org.siafco.app.domain.StoreQuote
import bo.org.siafco.app.feature.UiMessage
import bo.org.siafco.app.data.receipt.ReceiptPreparer
import coil3.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreCatalogScreen(
    viewModel: StoreCatalogViewModel,
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    onOpenProduct: (String) -> Unit,
    onOpenCart: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(state.loggedOut) { if (state.loggedOut) onLoggedOut() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.store_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Text("<") } },
                actions = { IconButton(onClick = onOpenCart) { Text("Carrito") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.filters.search,
                    onValueChange = viewModel::search,
                    label = { Text(stringResource(R.string.store_search)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(onClick = viewModel::applySearch, enabled = !state.loading) { Text(stringResource(R.string.store_update)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.filters.categorySlug == null,
                    onClick = { viewModel.selectCategory(null) },
                    label = { Text(stringResource(R.string.store_all)) }
                )
                state.catalog?.categories.orEmpty().take(3).forEach { category ->
                    FilterChip(
                        selected = state.filters.categorySlug == category.slug,
                        onClick = { viewModel.selectCategory(category.slug) },
                        label = { Text(category.name) }
                    )
                }
            }
            Message(state.message)
            if (state.loading && state.catalog == null) {
                CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
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
            TopAppBar(
                title = { Text(stringResource(R.string.store_product_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Text("<") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.loading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            Message(state.message)
            state.product?.let { product ->
                AsyncImage(
                    model = product.primaryImageUrl,
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    contentScale = ContentScale.Crop
                )
                Text(product.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("${product.currency} ${product.effectivePrice}", style = MaterialTheme.typography.titleLarge)
                product.shortDescription?.let { Text(it) }
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
                    OutlinedButton(onClick = { viewModel.setQuantity(state.quantity + 1) }) { Text("+") }
                }
                Button(onClick = viewModel::addToCart, enabled = product.isAvailable, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.store_add_to_cart))
                }
                if (state.added) {
                    OutlinedButton(onClick = onOpenCart, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.store_go_to_cart))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreCartScreen(
    viewModel: StoreCartViewModel,
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    onCheckout: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.loggedOut) { if (state.loggedOut) onLoggedOut() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.store_cart_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Text("<") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Message(state.message)
            if (state.lines.isEmpty()) {
                Text(stringResource(R.string.store_cart_empty))
                return@Column
            }
            state.quote?.let { QuoteSummary(it) }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f, fill = false)) {
                items(state.lines, key = { it.key }) { line ->
                    CartLineCard(line, onQuantity = { viewModel.updateQuantity(line, it) }, onRemove = { viewModel.remove(line) })
                }
            }
            Button(onClick = onCheckout, modifier = Modifier.fillMaxWidth(), enabled = !state.loadingQuote) {
                Text(stringResource(R.string.store_checkout))
            }
            OutlinedButton(onClick = viewModel::clear, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.store_clear_cart)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreCheckoutScreen(
    viewModel: StoreCheckoutViewModel,
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    onCreated: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.loggedOut) { if (state.loggedOut) onLoggedOut() }
    LaunchedEffect(state.createdOrder?.code) { state.createdOrder?.code?.let(onCreated) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.store_checkout_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Text("<") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Message(state.message)
            NormalizedTextField(
                value = state.form.couponCode,
                onValueChange = { value -> viewModel.updateForm { copy(couponCode = value) } },
                label = { Text(stringResource(R.string.store_coupon)) },
                modifier = Modifier.fillMaxWidth(),
                normalization = TextInputNormalization.Coupon
            )
            Text(stringResource(R.string.store_delivery), fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.form.deliveryMethod == "pickup",
                    onClick = { viewModel.updateForm { copy(deliveryMethod = "pickup") } },
                    label = { Text(stringResource(R.string.store_pickup)) }
                )
                FilterChip(
                    selected = state.form.deliveryMethod == "shipping",
                    onClick = { viewModel.updateForm { copy(deliveryMethod = "shipping") } },
                    label = { Text(stringResource(R.string.store_shipping)) }
                )
            }
            if (state.form.deliveryMethod == "shipping") {
                Field("department", state.form.department, state.fieldErrors) { viewModel.updateForm { copy(department = it) } }
                Field("city", state.form.city, state.fieldErrors) { viewModel.updateForm { copy(city = it) } }
                Field("zone", state.form.zone, state.fieldErrors) { viewModel.updateForm { copy(zone = it) } }
                Field("delivery_address", state.form.deliveryAddress, state.fieldErrors) { viewModel.updateForm { copy(deliveryAddress = it) } }
            }
            Button(onClick = viewModel::submit, enabled = !state.submitting && state.lines.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.store_create_order))
            }
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
    onOpenOrder: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(state.loggedOut) { if (state.loggedOut) onLoggedOut() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.store_orders_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Text("<") } },
                actions = { IconButton(onClick = { viewModel.load(force = true) }) { Text(stringResource(R.string.store_update)) } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Message(state.message)
            if (state.loading && state.orders.isEmpty()) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.orders, key = { it.code }) { order ->
                    Card(onClick = { onOpenOrder(order.code) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(order.code, fontWeight = FontWeight.SemiBold)
                            Text(order.statusLabel)
                            Text("${order.currency} ${order.total}")
                            order.itemSummary?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreOrderDetailScreen(
    viewModel: StoreOrderDetailViewModel,
    orderCode: String,
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    onUploadReceipt: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(orderCode) { viewModel.load(orderCode) }
    LaunchedEffect(state.loggedOut) { if (state.loggedOut) onLoggedOut() }
    LaunchedEffect(state.whatsappUrl) {
        val url = state.whatsappUrl ?: return@LaunchedEffect
        val safeUrl = StoreWhatsappPolicy.validate(url)
        if (safeUrl != null) {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)))
            }.onFailure {
                if (it is ActivityNotFoundException) Unit
            }
        }
        viewModel.whatsappConsumed()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.store_order_detail_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Text("<") } },
                actions = { IconButton(onClick = { viewModel.load(orderCode, force = true) }) { Text(stringResource(R.string.store_update)) } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Message(state.message)
            if (state.loading && state.order == null) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            state.order?.let { order ->
                Text(order.code, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(order.statusLabel)
                Text("${order.currency} ${order.total}", style = MaterialTheme.typography.titleMedium)
                order.items.forEach { item -> Text("${item.quantity} x ${item.name}: ${order.currency} ${item.lineTotal}") }
                order.payment?.message?.let { Text(it) }
                if (order.capabilities.canUploadReceipt) {
                    Button(onClick = { onUploadReceipt(order.code) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.store_upload_receipt))
                    }
                }
                if (order.capabilities.canOpenWhatsapp) {
                    OutlinedButton(onClick = viewModel::requestWhatsapp, enabled = !state.openingWhatsapp, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.store_open_whatsapp))
                    }
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
    onSubmitted: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val receiptPreparer = remember(context) { ReceiptPreparer(context) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.setReceipt(receiptPreparer.prepare(uri))
    }
    LaunchedEffect(state.loggedOut) { if (state.loggedOut) onLoggedOut() }
    LaunchedEffect(state.submittedOrder) { if (state.submittedOrder != null) onSubmitted() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.store_receipt_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Text("<") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.store_receipt_order, orderCode), fontWeight = FontWeight.SemiBold)
            Message(state.message)
            OutlinedButton(onClick = { launcher.launch(arrayOf("image/jpeg", "image/png", "image/webp", "application/pdf")) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.payment_pick_receipt))
            }
            state.receipt?.let { Text("${it.displayName} - ${it.sizeBytes / 1024} KB") }
            state.fieldError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = { viewModel.submit(orderCode) }, enabled = !state.submitting && state.receipt != null, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.store_upload_receipt))
            }
            if (state.submitting) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun ProductCard(product: StoreProduct, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = product.primaryImageUrl,
                contentDescription = product.name,
                modifier = Modifier.weight(0.35f).height(96.dp),
                contentScale = ContentScale.Crop
            )
            Column(Modifier.weight(0.65f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(product.name, fontWeight = FontWeight.SemiBold)
                product.category?.let { Text(it.name, style = MaterialTheme.typography.bodySmall) }
                Text("${product.currency} ${product.effectivePrice}", color = MaterialTheme.colorScheme.primary)
                Text(product.availabilityStatus, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun CartLineCard(line: StoreCartLine, onQuantity: (Int) -> Unit, onRemove: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(line.productPublicCode, fontWeight = FontWeight.SemiBold)
            line.variantPublicCode?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
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

@Composable
private fun QuoteSummary(quote: StoreQuote) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.store_quote), fontWeight = FontWeight.SemiBold)
            quote.items.forEach { item ->
                Text("${item.quantity} x ${item.productName}: ${quote.currency} ${item.lineTotal}")
            }
            Text("${stringResource(R.string.store_total)}: ${quote.currency} ${quote.total}", fontWeight = FontWeight.Bold)
        }
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
