package bo.org.siafco.app.feature.profile
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import bo.org.siafco.app.R
import bo.org.siafco.app.core.network.UrlResolver
import bo.org.siafco.app.core.text.TextInputNormalization
import bo.org.siafco.app.core.ui.CooperativeBottomBar
import bo.org.siafco.app.core.ui.CooperativeDestination
import bo.org.siafco.app.core.ui.CooperativeSpacing
import bo.org.siafco.app.core.ui.CooperativeTextSecondary
import bo.org.siafco.app.core.ui.FigmaGold
import bo.org.siafco.app.core.ui.FigmaDateField
import bo.org.siafco.app.core.ui.FigmaNavyDeep
import bo.org.siafco.app.core.ui.InstitutionalCard
import bo.org.siafco.app.core.ui.NormalizedTextField
import bo.org.siafco.app.core.ui.PrimaryButton
import bo.org.siafco.app.core.ui.SecondaryButton
import bo.org.siafco.app.core.ui.SectionHeader
import bo.org.siafco.app.core.ui.StatusBadge
import bo.org.siafco.app.core.ui.StatusTone
import bo.org.siafco.app.domain.MobileProfile
import bo.org.siafco.app.feature.photo.PhotoInputFlow
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onBottomDestination: (CooperativeDestination) -> Unit,
    onLoggedOut: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var confirmEmail by remember { mutableStateOf(false) }
    var confirmPassword by remember { mutableStateOf(false) }
    var photoFlowVisible by remember { mutableStateOf(false) }
    var openAction by remember { mutableStateOf<ProfileAction?>(null) }

    fun leaveProfile() {
        viewModel.clearSensitiveData()
        onBack()
    }

    BackHandler(enabled = !photoFlowVisible) {
        leaveProfile()
    }

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(state.loggedOut) {
        if (state.loggedOut) onLoggedOut()
    }

    if (confirmEmail) ConfirmDialog(
        text = stringResource(R.string.profile_confirm_email),
        onDismiss = { confirmEmail = false },
        onConfirm = {
            confirmEmail = false
            viewModel.saveProfile()
        }
    )
    if (confirmPassword) ConfirmDialog(
        text = stringResource(R.string.profile_confirm_password),
        onDismiss = { confirmPassword = false },
        onConfirm = {
            confirmPassword = false
            viewModel.changePassword()
        }
    )

    val active = state.profile?.status == "activo"
    Scaffold(
        bottomBar = {
            CooperativeBottomBar(
                selected = CooperativeDestination.Profile,
                canOpenStore = active,
                canOpenCredential = active,
                onSelect = onBottomDestination
            )
        }
    ) { padding ->
        Surface(modifier = Modifier.fillMaxSize().padding(padding), color = Color.White) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (state.loading && state.profile == null) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(32.dp))
                }

                state.profile?.let { profile ->
                    ProfileHeader(
                        profile = profile,
                        state = state,
                        onBack = ::leaveProfile,
                        onChangePhoto = { photoFlowVisible = true }
                    )
                    Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        PersonalDataCard(profile)
                        ProfileActionsCard(
                            onEditProfile = { openAction = ProfileAction.Edit },
                            onChangePassword = { openAction = ProfileAction.Password }
                        )
                        when (openAction) {
                            ProfileAction.Edit -> {
                                EditableProfileCard(state = state, viewModel = viewModel)
                                PrimaryButton(
                                    text = if (state.savingProfile) "Guardando..." else stringResource(R.string.profile_save),
                                    onClick = { if (state.form.email != profile.email) confirmEmail = true else viewModel.saveProfile() },
                                    enabled = !state.savingProfile,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            ProfileAction.Password -> PasswordSection(state = state, viewModel = viewModel, onSubmit = { confirmPassword = true })
                            null -> Unit
                        }
                        if (state.pendingPhoto != null) {
                            PhotoSection(
                                state = state,
                                onUpload = viewModel::savePhoto
                            )
                        }
                    }
                }

                state.message?.let { Text(stringResource(it.resId), color = MaterialTheme.colorScheme.error) }
                state.messageText?.let { Text(it, color = MaterialTheme.colorScheme.secondary) }
            }
        }
    }
    PhotoInputFlow(
        visible = photoFlowVisible,
        onDismiss = { photoFlowVisible = false },
        onPrepared = {
            viewModel.setPhoto(it)
        },
        onError = viewModel::onPhotoError
    )
}

private enum class ProfileAction {
    Edit,
    Password
}

@Composable
private fun ProfileHeader(
    profile: MobileProfile,
    state: ProfileUiState,
    onBack: () -> Unit,
    onChangePhoto: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(FigmaNavyDeep, RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp))
            .padding(horizontal = 24.dp, vertical = 30.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Card(
                    onClick = onBack,
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Box(modifier = Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.register_back),
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.profile_title),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Box(modifier = Modifier.size(46.dp))
            }
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(118.dp)
                        .clip(CircleShape)
                        .border(4.dp, FigmaGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    ProfilePhoto(profile = profile, state = state, modifier = Modifier.fillMaxSize())
                }
                Card(
                    onClick = onChangePhoto,
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = FigmaGold),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_profile_camera),
                            contentDescription = stringResource(R.string.profile_change_photo),
                            tint = FigmaNavyDeep,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Text(profile.fullName, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            StatusBadge(profile.statusLabel ?: profile.status.orEmpty(), tone = if (profile.status == "activo") StatusTone.Success else StatusTone.Warning)
            profile.registrationNumber?.let {
                Text("Afiliado N° $it", color = FigmaGold, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProfilePhoto(profile: MobileProfile, state: ProfileUiState, modifier: Modifier = Modifier) {
    val pending = state.pendingPhoto
    val resolvedUrl = UrlResolver.resolve(profile.photoUrl)
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            pending != null -> Image(
                painter = rememberAsyncImagePainter(pending.file),
                contentDescription = stringResource(R.string.register_photo_preview),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            resolvedUrl != null -> AsyncImage(
                model = resolvedUrl,
                contentDescription = stringResource(R.string.profile_photo),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            else -> Text(profile.fullName.take(1).uppercase(), style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun PersonalDataCard(profile: MobileProfile) {
    InstitutionalCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader("Datos personales", "Información real registrada en SIAFCO")
        ProfileInfoRow(R.drawable.ic_profile_shield, "Cédula de identidad", profile.ci)
        ProfileInfoRow(R.drawable.ic_mail, "Correo electrónico", profile.email)
        ProfileInfoRow(R.drawable.ic_profile_phone, "Teléfono móvil", profile.phone)
        ProfileInfoRow(R.drawable.ic_profile_location, "Dirección de domicilio", profile.address)
        ProfileInfoRow(R.drawable.ic_profile_calendar, "Fecha de nacimiento", formatProfileDate(profile.birthDate))
        ProfileInfoRow(R.drawable.ic_service_request, "Número de afiliado", profile.registrationNumber)
        ProfileInfoRow(R.drawable.ic_nav_store, "Sector", profile.sectorName)
        ProfileInfoRow(R.drawable.ic_profile_shield, "Plan", profile.planName)
        ProfileInfoRow(R.drawable.ic_profile_location, "Regional", profile.regional)
        ProfileInfoRow(R.drawable.ic_profile_shield, "Institución", profile.institution)
        ProfileInfoRow(R.drawable.ic_profile_edit, "Cargo", profile.position)
        ProfileInfoRow(R.drawable.ic_profile_shield, "Estado", profile.statusLabel ?: profile.status)
    }
}

@Composable
private fun ProfileActionsCard(
    onEditProfile: () -> Unit,
    onChangePassword: () -> Unit
) {
    InstitutionalCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader("Acciones", "Administra tu información de afiliado")
        PrimaryButton(
            text = "Editar Perfil",
            onClick = onEditProfile,
            modifier = Modifier.fillMaxWidth()
        )
        SecondaryButton(
            text = "Cambiar Contraseña",
            onClick = onChangePassword,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ProfileInfoRow(iconRes: Int, label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(FigmaGold.copy(alpha = 0.16f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = FigmaNavyDeep,
                modifier = Modifier.size(21.dp)
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = CooperativeTextSecondary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = FigmaNavyDeep,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatProfileDate(value: String?): String? {
    if (value.isNullOrBlank()) return null
    return runCatching {
        LocalDate.parse(value).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }.getOrDefault(value)
}

@Composable
private fun ProtectedDataCard(profile: MobileProfile) {
    InstitutionalCard(modifier = Modifier.fillMaxWidth()) {
            SectionHeader(stringResource(R.string.profile_readonly), "Información administrada por la cooperativa")
            InfoLine("Nombre", profile.fullName)
            InfoLine("CI", profile.ci)
            InfoLine("Código", profile.registrationNumber)
            InfoLine("Sector", profile.sectorName)
            InfoLine("Plan", profile.planName)
            InfoLine("Regional", profile.regional)
            InfoLine("Institución", profile.institution)
            InfoLine("Estado", profile.statusLabel ?: profile.status)
    }
}

@Composable
private fun EditableProfileCard(state: ProfileUiState, viewModel: ProfileViewModel) {
    InstitutionalCard(modifier = Modifier.fillMaxWidth()) {
            SectionHeader(stringResource(R.string.profile_editable), "Actualiza solo los campos permitidos")
            Field("phone", stringResource(R.string.register_phone), state.form.phone, state.fieldErrors, KeyboardType.Phone) { viewModel.updateForm { copy(phone = it) } }
            Field("email", stringResource(R.string.login_email), state.form.email, state.fieldErrors, KeyboardType.Email) { viewModel.updateForm { copy(email = it) } }
            Field("address", stringResource(R.string.register_address), state.form.address, state.fieldErrors, humanText = true) { viewModel.updateForm { copy(address = it) } }
            BirthDateField(state = state, viewModel = viewModel)
            MaritalStatusField(state = state, viewModel = viewModel)
    }
}

@Composable
private fun BirthDateField(state: ProfileUiState, viewModel: ProfileViewModel) {
    FigmaDateField(
        value = state.form.birthDate,
        onValueChange = { value -> viewModel.updateForm { copy(birthDate = value) } },
        label = stringResource(R.string.register_birth_date),
        modifier = Modifier.fillMaxWidth(),
        error = state.fieldErrors["birth_date"],
        maxSelectableDate = LocalDate.now()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaritalStatusField(state: ProfileUiState, viewModel: ProfileViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("SOLTERO", "CASADO", "DIVORCIADO", "VIUDO")
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = state.form.maritalStatus,
            onValueChange = {},
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth(),
            readOnly = true,
            label = { Text(stringResource(R.string.register_marital_status)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            isError = state.fieldErrors.containsKey("marital_status"),
            supportingText = { state.fieldErrors["marital_status"]?.let { Text(it) } }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        viewModel.updateForm { copy(maritalStatus = option) }
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PhotoSection(state: ProfileUiState, onUpload: () -> Unit) {
    val hasReadablePendingPhoto = state.pendingPhoto?.file?.let { it.exists() && it.canRead() } == true
    InstitutionalCard(modifier = Modifier.fillMaxWidth()) {
            SectionHeader(stringResource(R.string.profile_photo), "La foto se guarda aparte del perfil")
            state.pendingPhoto?.let {
                Image(
                    painter = rememberAsyncImagePainter(it.file),
                    contentDescription = stringResource(R.string.register_photo_preview),
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Text("Resolución: ${it.width} × ${it.height} px", style = MaterialTheme.typography.bodySmall)
                Text("Tamaño optimizado: ${formatPhotoSize(it.sizeBytes)}", style = MaterialTheme.typography.bodySmall)
                Text("Esta fotografía se utilizará en tu perfil y credencial.", style = MaterialTheme.typography.bodySmall)
            }
            if (state.pendingPhoto != null) {
                Text("Fotografia seleccionada, todavia no guardada.", style = MaterialTheme.typography.bodySmall)
            }
            state.fieldErrors["photo"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            PrimaryButton(text = if (state.savingPhoto) "Subiendo..." else stringResource(R.string.profile_upload_photo), onClick = onUpload, enabled = hasReadablePendingPhoto && !state.savingPhoto, modifier = Modifier.fillMaxWidth())
    }
}

private fun formatPhotoSize(sizeBytes: Long): String {
    val kb = sizeBytes / 1024.0
    return if (kb < 1024) "%.0f KB".format(kb) else "%.1f MB".format(kb / 1024.0)
}

@Composable
private fun PasswordSection(state: ProfileUiState, viewModel: ProfileViewModel, onSubmit: () -> Unit) {
    InstitutionalCard(modifier = Modifier.fillMaxWidth()) {
            SectionHeader(stringResource(R.string.profile_security), stringResource(R.string.profile_password_help))
            PasswordField("current_password", stringResource(R.string.profile_current_password), state.passwordForm.currentPassword, state) {
                viewModel.updatePasswordForm { copy(currentPassword = it) }
            }
            PasswordField("password", stringResource(R.string.profile_new_password), state.passwordForm.password, state) {
                viewModel.updatePasswordForm { copy(password = it) }
            }
            PasswordField("password_confirmation", stringResource(R.string.register_password_confirmation), state.passwordForm.passwordConfirmation, state) {
                viewModel.updatePasswordForm { copy(passwordConfirmation = it) }
            }
            SecondaryButton(text = stringResource(if (state.passwordVisible) R.string.login_hide_password else R.string.login_show_password), onClick = viewModel::togglePasswordVisibility, modifier = Modifier.fillMaxWidth())
            PrimaryButton(text = stringResource(R.string.profile_change_password), onClick = onSubmit, enabled = !state.savingPassword, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Field(
    field: String,
    label: String,
    value: String,
    errors: Map<String, String>,
    keyboardType: KeyboardType = KeyboardType.Text,
    humanText: Boolean = false,
    onChange: (String) -> Unit
) {
    NormalizedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        isError = errors.containsKey(field),
        supportingText = { errors[field]?.let { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        normalization = if (humanText) TextInputNormalization.Human else TextInputNormalization.None
    )
}

@Composable
private fun PasswordField(field: String, label: String, value: String, state: ProfileUiState, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        isError = state.passwordErrors.containsKey(field),
        supportingText = { state.passwordErrors[field]?.let { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
    )
}

@Composable
private fun InfoLine(label: String, value: String?) {
    if (!value.isNullOrBlank()) Text("$label: $value", style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun ConfirmDialog(text: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.profile_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.register_back)) } }
    )
}
