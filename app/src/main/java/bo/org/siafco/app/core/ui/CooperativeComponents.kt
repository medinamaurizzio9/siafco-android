package bo.org.siafco.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

enum class CooperativeDestination(val label: String, val mark: String) {
    Home("Inicio", "IN"),
    Store("Tienda", "TI"),
    Orders("Pedidos", "PE"),
    Credential("Credencial", "CR"),
    Profile("Perfil", "PF")
}

object CooperativeSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CooperativeTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
        },
        navigationIcon = {
            if (onBack != null) {
                OutlinedButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                    Text("Atrás")
                }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = CooperativeNavy,
            navigationIconContentColor = CooperativeNavy,
            actionIconContentColor = CooperativeNavy
        )
    )
}

@Composable
fun CooperativeBottomBar(
    selected: CooperativeDestination,
    canOpenStore: Boolean,
    canOpenCredential: Boolean,
    onSelect: (CooperativeDestination) -> Unit
) {
    NavigationBar(
        containerColor = CooperativeSurface,
        tonalElevation = 8.dp
    ) {
        CooperativeDestination.entries.forEach { destination ->
            val enabled = when (destination) {
                CooperativeDestination.Store,
                CooperativeDestination.Orders -> canOpenStore
                CooperativeDestination.Credential -> canOpenCredential
                else -> true
            }
            NavigationBarItem(
                selected = selected == destination,
                enabled = enabled,
                onClick = { onSelect(destination) },
                icon = {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (selected == destination) CooperativeGoldSoft else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(destination.mark, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                    }
                },
                label = { Text(destination.label, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CooperativeNavy,
                    selectedTextColor = CooperativeNavy,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = CooperativeTextSecondary,
                    unselectedTextColor = CooperativeTextSecondary,
                    disabledIconColor = CooperativeTextSecondary.copy(alpha = 0.36f),
                    disabledTextColor = CooperativeTextSecondary.copy(alpha = 0.36f)
                )
            )
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CooperativeGold, contentColor = CooperativeNavy),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = CooperativeNavy)
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun InstitutionalCard(
    modifier: Modifier = Modifier,
    tonal: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (tonal) Color(0xFFEAF0F7) else CooperativeSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(CooperativeSpacing.md), verticalArrangement = Arrangement.spacedBy(CooperativeSpacing.sm), content = content)
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CooperativeTextPrimary)
        subtitle?.takeIf(String::isNotBlank)?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = CooperativeTextSecondary)
        }
    }
}

@Composable
fun StatusBadge(text: String, tone: StatusTone = StatusTone.Neutral, modifier: Modifier = Modifier) {
    val color = when (tone) {
        StatusTone.Success -> CooperativeSuccess
        StatusTone.Warning -> CooperativeWarning
        StatusTone.Error -> CooperativeError
        StatusTone.Neutral -> CooperativeNavySecondary
    }
    Text(
        text = text,
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
            .border(1.dp, color.copy(alpha = 0.24f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        color = color,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold
    )
}

enum class StatusTone { Success, Warning, Error, Neutral }

@Composable
fun LoadingSkeleton(message: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator()
        Text(message, color = CooperativeTextSecondary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    InstitutionalCard(modifier = modifier.fillMaxWidth(), tonal = true) {
        Text(title, fontWeight = FontWeight.Bold, color = CooperativeNavy)
        Text(message, color = CooperativeTextSecondary)
    }
}

@Composable
fun ErrorState(message: String, modifier: Modifier = Modifier) {
    InstitutionalCard(modifier = modifier.fillMaxWidth()) {
        Text("No pudimos cargar esta información", fontWeight = FontWeight.Bold, color = CooperativeError)
        Text(message, color = CooperativeTextSecondary)
    }
}

@Composable
fun MoneyText(currency: String, amount: String, modifier: Modifier = Modifier) {
    Text(
        text = "${currency.ifBlank { "BOB" }} $amount",
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        color = CooperativeNavy,
        fontWeight = FontWeight.Black
    )
}

@Composable
fun SecureTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    supportingText: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        isError = isError,
        keyboardOptions = keyboardOptions,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        supportingText = supportingText,
        trailingIcon = trailingIcon
    )
}

@Composable
fun OrderSummaryCard(
    title: String,
    total: String,
    currency: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    InstitutionalCard(modifier = modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(title, fontWeight = FontWeight.Bold, color = CooperativeNavy)
            MoneyText(currency = currency, amount = total)
        }
        content()
    }
}

@Composable
fun ProductCard(
    title: String,
    subtitle: String?,
    price: String,
    currency: String,
    modifier: Modifier = Modifier,
    status: String? = null,
    onClick: (() -> Unit)? = null
) {
    val cardContent: @Composable ColumnScope.() -> Unit = {
        Text(title, fontWeight = FontWeight.Bold, color = CooperativeTextPrimary)
        subtitle?.takeIf(String::isNotBlank)?.let { Text(it, color = CooperativeTextSecondary, style = MaterialTheme.typography.bodySmall) }
        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            MoneyText(currency = currency, amount = price)
            status?.takeIf(String::isNotBlank)?.let { StatusBadge(it) }
        }
    }
    if (onClick == null) {
        InstitutionalCard(modifier = modifier, content = cardContent)
    } else {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CooperativeSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            content = { Column(modifier = Modifier.padding(CooperativeSpacing.md), verticalArrangement = Arrangement.spacedBy(CooperativeSpacing.sm), content = cardContent) }
        )
    }
}

@Composable
fun InfoRow(label: String, value: String?, modifier: Modifier = Modifier) {
    if (value.isNullOrBlank()) return
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(label, color = CooperativeTextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.45f))
        Text(value, color = CooperativeTextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.55f))
    }
}
