package bo.org.siafco.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bo.org.siafco.app.R
import bo.org.siafco.app.core.network.UrlResolver
import bo.org.siafco.app.core.ui.CooperativeBottomBar
import bo.org.siafco.app.core.ui.CooperativeDestination
import bo.org.siafco.app.core.ui.CooperativeGoldSoft
import bo.org.siafco.app.core.ui.CooperativeNavy
import bo.org.siafco.app.core.ui.CooperativeSpacing
import bo.org.siafco.app.core.ui.CooperativeSurface
import bo.org.siafco.app.core.ui.CooperativeTextSecondary
import bo.org.siafco.app.core.ui.InstitutionalCard
import bo.org.siafco.app.core.ui.LoadingSkeleton
import bo.org.siafco.app.core.ui.MoneyText
import bo.org.siafco.app.core.ui.SectionHeader
import bo.org.siafco.app.core.ui.SecondaryButton
import bo.org.siafco.app.core.ui.StatusBadge
import bo.org.siafco.app.core.ui.StatusTone
import bo.org.siafco.app.domain.AccessLevel
import bo.org.siafco.app.domain.SessionProfile
import bo.org.siafco.app.domain.StoreOrder
import bo.org.siafco.app.feature.UiMessage
import coil3.compose.AsyncImage

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenProfile: () -> Unit,
    onOpenAffiliationRequest: () -> Unit,
    onOpenCredential: () -> Unit,
    onOpenStore: () -> Unit,
    onOpenStoreOrders: () -> Unit,
    onBottomDestination: (CooperativeDestination) -> Unit,
    onLoggedOut: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(state.loggedOut) {
        if (state.loggedOut) onLoggedOut()
    }

    val profile = state.profile
    val activeAffiliate = profile?.affiliateStatus == "activo" && profile.accessLevel == AccessLevel.Active
    Scaffold(
        bottomBar = {
            CooperativeBottomBar(
                selected = CooperativeDestination.Home,
                canOpenStore = activeAffiliate,
                canOpenCredential = state.capabilities.canViewCredential,
                onSelect = onBottomDestination
            )
        }
    ) { padding ->
        Surface(modifier = Modifier.fillMaxSize().padding(padding), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(CooperativeSpacing.md),
                verticalArrangement = Arrangement.spacedBy(CooperativeSpacing.md)
            ) {
                if (!state.loaded && profile == null) {
                    LoadingSkeleton(message = "Validando tu sesión")
                    return@Column
                }
                profile?.let { AffiliateHeader(it) }
                state.message?.let { Text(text = stringResource(it.resId), color = MaterialTheme.colorScheme.error) }
                PendingOrdersSection(
                    loading = state.ordersLoading,
                    orders = state.attentionOrders,
                    message = state.ordersMessage,
                    onRetry = viewModel::retryAttentionOrders,
                    onOpenOrder = { onOpenStoreOrders() }
                )
                ServicesSection(
                    activeAffiliate = activeAffiliate,
                    canViewProfile = state.capabilities.canViewProfile,
                    canViewRequest = state.capabilities.canViewAffiliationRequest,
                    canViewCredential = state.capabilities.canViewCredential,
                    onOpenProfile = onOpenProfile,
                    onOpenAffiliationRequest = onOpenAffiliationRequest,
                    onOpenCredential = onOpenCredential,
                    onOpenStore = onOpenStore,
                    onOpenStoreOrders = onOpenStoreOrders
                )
            }
        }
    }
}

@Composable
private fun AffiliateHeader(profile: SessionProfile) {
    InstitutionalCard(modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(CooperativeSpacing.md), verticalAlignment = Alignment.CenterVertically) {
            AffiliatePhoto(profile = profile)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(profile.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                StatusBadge(
                    text = profile.affiliateStatusLabel.ifBlank { profile.affiliateStatus },
                    tone = if (profile.affiliateStatus == "activo") StatusTone.Success else StatusTone.Warning
                )
                profile.registrationNumber?.takeIf(String::isNotBlank)?.let {
                    Text("N.º afiliado $it", style = MaterialTheme.typography.bodySmall, color = CooperativeTextSecondary)
                }
            }
        }
    }
}

@Composable
private fun AffiliatePhoto(profile: SessionProfile) {
    val resolvedPhotoUrl = UrlResolver.resolve(profile.photoUrl)
    Surface(
        modifier = Modifier.size(68.dp).clip(CircleShape),
        color = CooperativeNavy,
        contentColor = CooperativeSurface
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (resolvedPhotoUrl != null) {
                AsyncImage(
                    model = resolvedPhotoUrl,
                    contentDescription = stringResource(R.string.profile_photo),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(initials(profile.name), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun PendingOrdersSection(
    loading: Boolean,
    orders: List<StoreOrder>,
    message: UiMessage?,
    onRetry: () -> Unit,
    onOpenOrder: () -> Unit
) {
    if (!loading && orders.isEmpty() && message == null) return

    Column(verticalArrangement = Arrangement.spacedBy(CooperativeSpacing.sm)) {
        if (loading) {
            InstitutionalCard(modifier = Modifier.fillMaxWidth(), tonal = true) {
                LoadingSkeleton(message = "Consultando pedidos")
            }
        }
        message?.let {
            InstitutionalCard(modifier = Modifier.fillMaxWidth(), tonal = true) {
                Text("No pudimos actualizar tus pedidos", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                SecondaryButton(text = "Reintentar", onClick = onRetry, modifier = Modifier.fillMaxWidth())
            }
        }
        if (orders.isNotEmpty()) {
            SectionHeader(title = "Pendientes")
            orders.forEach { order ->
                PendingOrderCard(order = order, onOpen = onOpenOrder)
            }
            SecondaryButton(text = "Ver todos mis pedidos", onClick = onOpenOrder, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PendingOrderCard(order: StoreOrder, onOpen: () -> Unit) {
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CooperativeSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(CooperativeSpacing.md), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.store_order_number, order.code), fontWeight = FontWeight.Bold)
                StatusBadge(text = order.homeStatusLabel(), tone = if (order.status == "pago_en_revision") StatusTone.Warning else StatusTone.Neutral)
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column {
                    order.date?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = CooperativeTextSecondary) }
                    Text(order.attentionGroupLabel(), style = MaterialTheme.typography.bodySmall, color = CooperativeTextSecondary)
                }
                MoneyText(currency = order.currency, amount = order.total)
            }
            SecondaryButton(text = "Ver pedido", onClick = onOpen, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ServicesSection(
    activeAffiliate: Boolean,
    canViewProfile: Boolean,
    canViewRequest: Boolean,
    canViewCredential: Boolean,
    onOpenProfile: () -> Unit,
    onOpenAffiliationRequest: () -> Unit,
    onOpenCredential: () -> Unit,
    onOpenStore: () -> Unit,
    onOpenStoreOrders: () -> Unit
) {
    val services = buildList {
        if (canViewProfile) add(HomeService("PF", stringResource(R.string.home_profile), onOpenProfile))
        if (canViewRequest) add(HomeService("MS", stringResource(R.string.home_request), onOpenAffiliationRequest))
        if (canViewCredential) add(HomeService("CR", stringResource(R.string.home_credential), onOpenCredential))
        if (activeAffiliate) {
            add(HomeService("TI", stringResource(R.string.home_store), onOpenStore))
            add(HomeService("PE", stringResource(R.string.home_store_orders), onOpenStoreOrders))
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(CooperativeSpacing.sm)) {
        SectionHeader(title = "Servicios")
        services.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(CooperativeSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                row.forEach { service ->
                    ServiceTile(service = service, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ServiceTile(service: HomeService, modifier: Modifier = Modifier) {
    Card(
        onClick = service.onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CooperativeSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CooperativeGoldSoft),
                contentAlignment = Alignment.Center
            ) {
                Text(service.mark, color = CooperativeNavy, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelMedium)
            }
            Text(service.title, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private data class HomeService(
    val mark: String,
    val title: String,
    val onClick: () -> Unit
)

private fun initials(name: String): String {
    val parts = name.split(' ').filter(String::isNotBlank)
    return parts.take(2).joinToString("") { it.take(1) }.ifBlank { "A" }.uppercase()
}

private fun StoreOrder.attentionGroupLabel(): String = when (status) {
    "pago_en_revision" -> "Pagos en revisión"
    else -> "Pedidos sin pagar"
}

private fun StoreOrder.homeStatusLabel(): String = when (status) {
    "pendiente" -> "Pendiente"
    "reservado" -> "Reservado"
    "esperando_pago" -> "Esperando pago"
    "pago_en_revision" -> "Pago en revisión"
    else -> statusLabel
}
