package bo.org.siafco.app.feature.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bo.org.siafco.app.R
import bo.org.siafco.app.core.network.UrlResolver
import bo.org.siafco.app.core.ui.CooperativeBottomBar
import bo.org.siafco.app.core.ui.CooperativeDestination
import bo.org.siafco.app.core.ui.CooperativeNavy
import bo.org.siafco.app.core.ui.CooperativeSpacing
import bo.org.siafco.app.core.ui.CooperativeSurface
import bo.org.siafco.app.core.ui.CooperativeTextSecondary
import bo.org.siafco.app.core.ui.FigmaGold
import bo.org.siafco.app.core.ui.FigmaNavy
import bo.org.siafco.app.core.ui.FigmaNavyDeep
import bo.org.siafco.app.core.ui.InstitutionalCard
import bo.org.siafco.app.core.ui.LoadingSkeleton
import bo.org.siafco.app.core.ui.SectionHeader
import bo.org.siafco.app.core.ui.SecondaryButton
import bo.org.siafco.app.core.ui.StatusBadge
import bo.org.siafco.app.core.ui.StatusTone
import bo.org.siafco.app.domain.AccessLevel
import bo.org.siafco.app.domain.SessionProfile
import bo.org.siafco.app.domain.StoreOrder
import bo.org.siafco.app.feature.UiMessage
import coil3.compose.AsyncImage
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

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
        Surface(modifier = Modifier.fillMaxSize().padding(padding), color = CooperativeSurface) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                if (!state.loaded && profile == null) {
                    LoadingSkeleton(message = "Validando tu sesión", modifier = Modifier.padding(32.dp))
                    return@Column
                }
                profile?.let { HomeHero(it) }
                Column(
                    modifier = Modifier
                        .padding(horizontal = 22.dp)
                        .padding(top = 24.dp, bottom = CooperativeSpacing.xl),
                    verticalArrangement = Arrangement.spacedBy(26.dp)
                ) {
                    state.message?.let { Text(text = stringResource(it.resId), color = MaterialTheme.colorScheme.error) }
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
                    PendingOrdersSection(
                        loading = state.ordersLoading,
                        orders = state.attentionOrders,
                        message = state.ordersMessage,
                        onRetry = viewModel::retryAttentionOrders,
                        onOpenOrder = { onOpenStoreOrders() }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHero(profile: SessionProfile) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(FigmaNavyDeep, FigmaNavy, Color(0xFF123255))
                ),
                shape = RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp)
            )
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .border(2.dp, FigmaGold, CircleShape)
                            .padding(5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.splash_logo),
                            contentDescription = stringResource(R.string.brand_logo_content_description),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Column {
                        Text("SIAFCO", color = FigmaGold, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        Text("Tierra Bendita", color = CooperativeSurface.copy(alpha = 0.86f), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF162D52).copy(alpha = 0.88f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(2.dp, FigmaGold)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.splash_logo),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                contentScale = ContentScale.Fit
                            )
                            Text("F.I. TIERRA BENDITA", color = FigmaGold, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                        }
                        AffiliateStatusPill(profile)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        AffiliatePhoto(profile = profile, size = 78)
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(profile.name, color = CooperativeSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                            profile.registrationNumber?.takeIf(String::isNotBlank)?.let {
                                Text("Afiliado N.º $it", color = FigmaGold, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.12f)))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        HeroMeta(label = "ESTADO", value = profile.affiliateStatusLabel.ifBlank { profile.affiliateStatus })
                        HeroMeta(label = "ACCESO", value = if (profile.accessLevel == AccessLevel.Active) "Completo" else "Limitado")
                    }
                }
            }
        }
    }
}

@Composable
private fun AffiliateStatusPill(profile: SessionProfile) {
    val active = profile.affiliateStatus == "activo"
    val color = if (active) Color(0xFF12E878) else FigmaGold
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
            .border(1.5.dp, color, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(
            text = profile.affiliateStatusLabel.ifBlank { profile.affiliateStatus }.replaceFirstChar { it.uppercase() },
            color = color,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun HeroMeta(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = Color(0xFF94A1B2), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
        Text(value, color = CooperativeSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun AffiliatePhoto(profile: SessionProfile, size: Int = 68) {
    val resolvedPhotoUrl = UrlResolver.resolve(profile.photoUrl)
    Surface(
        modifier = Modifier.size(size.dp).clip(CircleShape),
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

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
            SectionHeader(title = "Últimas actividades", subtitle = "Pedidos que requieren tu atención")
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CooperativeSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(FigmaGold))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.store_order_number, order.code), fontWeight = FontWeight.Black, color = FigmaNavy)
                Text(order.attentionGroupLabel(), style = MaterialTheme.typography.bodyMedium, color = CooperativeTextSecondary)
                order.date?.let {
                    Text(formatOrderDate(it), style = MaterialTheme.typography.bodySmall, color = CooperativeTextSecondary)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusBadge(text = order.homeStatusLabel(), tone = if (order.status == "pago_en_revision") StatusTone.Warning else StatusTone.Neutral)
                Text(
                    text = "${order.currency.ifBlank { "BOB" }} ${order.total}",
                    color = FigmaNavy,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
            }
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
        if (canViewProfile) add(HomeService(R.drawable.ic_nav_profile, stringResource(R.string.home_profile), onOpenProfile))
        if (canViewCredential) add(HomeService(R.drawable.ic_nav_credential, stringResource(R.string.home_credential), onOpenCredential))
        if (canViewRequest) add(HomeService(R.drawable.ic_service_request, stringResource(R.string.home_request), onOpenAffiliationRequest))
        if (activeAffiliate) {
            add(HomeService(R.drawable.ic_nav_store, stringResource(R.string.home_store), onOpenStore))
            add(HomeService(R.drawable.ic_nav_orders, stringResource(R.string.home_store_orders), onOpenStoreOrders))
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SectionHeader(title = "Servicios rápidos")
        services.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { service ->
                    ServiceTile(service = service, modifier = Modifier.weight(1f))
                }
                repeat(3 - row.size) {
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
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F5F8)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CooperativeSurface)
                    .border(1.5.dp, FigmaGold, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(service.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = FigmaNavy
                )
            }
            Text(service.title, textAlign = TextAlign.Center, fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyMedium, color = FigmaNavy, maxLines = 2)
        }
    }
}

private data class HomeService(
    @param:DrawableRes val iconRes: Int,
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

private fun formatOrderDate(value: String): String {
    val output = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    return runCatching { OffsetDateTime.parse(value).format(output) }
        .recoverCatching { LocalDateTime.parse(value).format(output) }
        .recoverCatching { LocalDate.parse(value).format(output) }
        .getOrElse { value }
}
