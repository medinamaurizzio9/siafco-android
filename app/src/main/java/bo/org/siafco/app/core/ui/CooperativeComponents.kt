package bo.org.siafco.app.core.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import bo.org.siafco.app.R

enum class CooperativeDestination(val label: String, @param:DrawableRes val iconRes: Int) {
    Home("Inicio", R.drawable.ic_nav_home),
    Store("Tienda", R.drawable.ic_nav_store),
    Orders("Pedidos", R.drawable.ic_nav_orders),
    Credential("Credencial", R.drawable.ic_nav_credential),
    Profile("Perfil", R.drawable.ic_nav_profile)
}

object CooperativeSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}

val SiafcoTextFieldShape = RoundedCornerShape(16.dp)

@Composable
fun siafcoOutlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = CooperativeTextPrimary,
    unfocusedTextColor = CooperativeTextPrimary,
    disabledTextColor = CooperativeTextSecondary,
    errorTextColor = CooperativeTextPrimary,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color(0xFFFAFBFD),
    disabledContainerColor = Color(0xFFE8EDF3),
    errorContainerColor = Color.White,
    focusedBorderColor = FigmaGold,
    unfocusedBorderColor = Color(0xFF5B708A),
    disabledBorderColor = Color(0xFFB8C2D0),
    errorBorderColor = CooperativeError,
    focusedLabelColor = FigmaNavy,
    unfocusedLabelColor = Color(0xFF4C5F75),
    disabledLabelColor = CooperativeTextSecondary,
    errorLabelColor = CooperativeError,
    cursorColor = FigmaNavy,
    errorCursorColor = CooperativeError,
    focusedPlaceholderColor = Color(0xFF667085),
    unfocusedPlaceholderColor = Color(0xFF667085),
    disabledPlaceholderColor = Color(0xFF8A94A3),
    focusedSupportingTextColor = CooperativeTextSecondary,
    unfocusedSupportingTextColor = CooperativeTextSecondary,
    errorSupportingTextColor = CooperativeError
)

@Composable
fun FigmaBrandRow(
    modifier: Modifier = Modifier,
    logoSize: androidx.compose.ui.unit.Dp = 74.dp,
    title: String = "SIAFCO",
    subtitle: String = "Fondo de Inversiones Tierra Bendita",
    titleColor: Color = FigmaGold,
    subtitleColor: Color = FigmaGold
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.splash_logo),
            contentDescription = stringResource(R.string.brand_logo_content_description),
            modifier = Modifier.size(logoSize),
            contentScale = ContentScale.Fit
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = titleColor)
            Text(subtitle, style = MaterialTheme.typography.titleSmall, color = subtitleColor)
        }
    }
}

@Composable
fun FigmaHeaderPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(238.dp)
            .background(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(FigmaNavyDeep, FigmaNavy, Color(0xFF203B56))
                ),
                shape = RoundedCornerShape(bottomStart = 42.dp, bottomEnd = 42.dp)
            )
            .padding(horizontal = 28.dp, vertical = 38.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        content()
    }
}

@Composable
fun FigmaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = FigmaGold, contentColor = FigmaNavy),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
    }
}

@Composable
fun FigmaSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = FigmaGold),
        border = androidx.compose.foundation.BorderStroke(2.dp, FigmaGold),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.invoke()
            if (leadingIcon != null) {
                Spacer(Modifier.width(10.dp))
            }
            Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun FigmaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label, fontWeight = FontWeight.SemiBold) },
        singleLine = true,
        isError = isError,
        supportingText = supportingText,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = SiafcoTextFieldShape,
        colors = siafcoOutlinedTextFieldColors()
    )
}

@Composable
fun FigmaPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    FigmaTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_lock),
                contentDescription = null,
                tint = FigmaNavy
            )
        },
        isError = isError,
        supportingText = supportingText,
        keyboardOptions = keyboardOptions,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleVisible) {
                Icon(
                    painter = painterResource(if (visible) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
                    contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
                    tint = FigmaNavy
                )
            }
        }
    )
}

@Composable
fun FigmaDisabledFeatureRow(
    text: String,
    modifier: Modifier = Modifier,
    trailingText: String? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = Color(0xFF20242B), style = MaterialTheme.typography.titleMedium)
        trailingText?.let {
            Text(it, color = FigmaGold, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun FigmaDisabledRememberRow(
    text: String,
    modifier: Modifier = Modifier,
    trailingText: String? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(30.dp)
                    .background(FigmaGold.copy(alpha = 0.76f), RoundedCornerShape(999.dp))
                    .padding(4.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(FigmaNavy, CircleShape)
                )
            }
            Text(text, color = Color(0xFF20242B), style = MaterialTheme.typography.titleMedium)
        }
        trailingText?.let {
            Text(it, color = FigmaGold, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun FigmaDividerOr(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFFE5E9F0)))
        Spacer(Modifier.width(18.dp))
        Text("o", color = FigmaMuted, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(18.dp))
        Box(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFFE5E9F0)))
    }
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
    onSelect: (CooperativeDestination) -> Unit,
    dark: Boolean = false
) {
    NavigationBar(
        containerColor = if (dark) FigmaNavyDeep else CooperativeSurface,
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
                        Icon(
                            painter = painterResource(destination.iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                label = { Text(destination.label, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = if (dark) FigmaGold else CooperativeNavy,
                    selectedTextColor = if (dark) FigmaGold else CooperativeNavy,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = if (dark) Color(0xFFAAB7C8) else CooperativeTextSecondary,
                    unselectedTextColor = if (dark) Color(0xFFAAB7C8) else CooperativeTextSecondary,
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
        trailingIcon = trailingIcon,
        shape = SiafcoTextFieldShape,
        colors = siafcoOutlinedTextFieldColors()
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
