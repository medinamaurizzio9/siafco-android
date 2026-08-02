package bo.org.siafco.app.feature.profile

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import bo.org.siafco.app.R
import bo.org.siafco.app.core.network.UrlResolver
import bo.org.siafco.app.domain.MobileProfile
import bo.org.siafco.app.feature.photo.PhotoInputFlow
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import java.time.LocalDate

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var confirmEmail by remember { mutableStateOf(false) }
    var confirmPassword by remember { mutableStateOf(false) }
    var photoFlowVisible by remember { mutableStateOf(false) }

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

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedButton(onClick = {
                leaveProfile()
            }) { Text(stringResource(R.string.register_back)) }

            if (state.loading && state.profile == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            state.profile?.let { profile ->
                ProfileHeader(profile = profile, state = state)
                ProtectedDataCard(profile)
                EditableProfileCard(state = state, onPickDate = { showBirthDatePicker(context, state, viewModel) }, viewModel = viewModel)
                Button(
                    onClick = { if (state.form.email != profile.email) confirmEmail = true else viewModel.saveProfile() },
                    enabled = !state.savingProfile,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.profile_save)) }
                PhotoSection(
                    state = state,
                    onPick = { photoFlowVisible = true },
                    onUpload = viewModel::savePhoto
                )
                PasswordSection(state = state, viewModel = viewModel, onSubmit = { confirmPassword = true })
            }

            state.message?.let { Text(stringResource(it.resId), color = MaterialTheme.colorScheme.error) }
            state.messageText?.let { Text(it, color = MaterialTheme.colorScheme.secondary) }
        }
    }
    PhotoInputFlow(
        visible = photoFlowVisible,
        onDismiss = { photoFlowVisible = false },
        onPrepared = viewModel::setPhoto,
        onError = viewModel::onPhotoError
    )
}

private fun showBirthDatePicker(context: android.content.Context, state: ProfileUiState, viewModel: ProfileViewModel) {
    val initial = runCatching { LocalDate.parse(state.form.birthDate) }.getOrDefault(LocalDate.now().minusYears(25))
    DatePickerDialog(
        context,
        { _, year, month, day -> viewModel.updateForm { copy(birthDate = backendBirthDateFromParts(year, month, day)) } },
        initial.year,
        initial.monthValue - 1,
        initial.dayOfMonth
    ).show()
}

@Composable
private fun ProfileHeader(profile: MobileProfile, state: ProfileUiState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        ProfilePhoto(profile = profile, state = state, modifier = Modifier.size(112.dp))
        Text(profile.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        profile.registrationNumber?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        Text(profile.statusLabel ?: profile.status.orEmpty(), color = MaterialTheme.colorScheme.primary)
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
private fun ProtectedDataCard(profile: MobileProfile) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.profile_readonly), fontWeight = FontWeight.SemiBold)
            InfoLine("Nombre", profile.fullName)
            InfoLine("CI", profile.ci)
            InfoLine("Codigo", profile.registrationNumber)
            InfoLine("Sector", profile.sectorName)
            InfoLine("Plan", profile.planName)
            InfoLine("Regional", profile.regional)
            InfoLine("Institucion", profile.institution)
            InfoLine("Estado", profile.statusLabel ?: profile.status)
        }
    }
}

@Composable
private fun EditableProfileCard(state: ProfileUiState, onPickDate: () -> Unit, viewModel: ProfileViewModel) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.profile_editable), fontWeight = FontWeight.SemiBold)
            Field("phone", stringResource(R.string.register_phone), state.form.phone, state.fieldErrors) { viewModel.updateForm { copy(phone = it) } }
            Field("email", stringResource(R.string.login_email), state.form.email, state.fieldErrors) { viewModel.updateForm { copy(email = it) } }
            Field("address", stringResource(R.string.register_address), state.form.address, state.fieldErrors) { viewModel.updateForm { copy(address = it) } }
            BirthDateField(state = state, onPickDate = onPickDate)
            MaritalStatusField(state = state, viewModel = viewModel)
        }
    }
}

@Composable
private fun BirthDateField(state: ProfileUiState, onPickDate: () -> Unit) {
    OutlinedTextField(
        value = state.form.birthDate.takeIf(String::isNotBlank)?.let(::formatBirthDateForDisplay).orEmpty(),
        onValueChange = {},
        label = { Text(stringResource(R.string.register_birth_date)) },
        modifier = Modifier.fillMaxWidth(),
        readOnly = true,
        trailingIcon = { TextButton(onClick = onPickDate) { Text(stringResource(R.string.payment_pick_date)) } },
        isError = state.fieldErrors.containsKey("birth_date"),
        supportingText = { state.fieldErrors["birth_date"]?.let { Text(it) } }
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
private fun PhotoSection(state: ProfileUiState, onPick: () -> Unit, onUpload: () -> Unit) {
    val hasReadablePendingPhoto = state.pendingPhoto?.file?.let { it.exists() && it.canRead() } == true
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.profile_photo), fontWeight = FontWeight.SemiBold)
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
            state.fieldErrors["photo"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            OutlinedButton(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.profile_change_photo))
            }
            Button(onClick = onUpload, enabled = hasReadablePendingPhoto && !state.savingPhoto, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.profile_upload_photo))
            }
        }
    }
}

private fun formatPhotoSize(sizeBytes: Long): String {
    val kb = sizeBytes / 1024.0
    return if (kb < 1024) "%.0f KB".format(kb) else "%.1f MB".format(kb / 1024.0)
}

@Composable
private fun PasswordSection(state: ProfileUiState, viewModel: ProfileViewModel, onSubmit: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.profile_security), fontWeight = FontWeight.SemiBold)
            PasswordField("current_password", stringResource(R.string.profile_current_password), state.passwordForm.currentPassword, state) {
                viewModel.updatePasswordForm { copy(currentPassword = it) }
            }
            PasswordField("password", stringResource(R.string.profile_new_password), state.passwordForm.password, state) {
                viewModel.updatePasswordForm { copy(password = it) }
            }
            PasswordField("password_confirmation", stringResource(R.string.register_password_confirmation), state.passwordForm.passwordConfirmation, state) {
                viewModel.updatePasswordForm { copy(passwordConfirmation = it) }
            }
            OutlinedButton(onClick = viewModel::togglePasswordVisibility, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (state.passwordVisible) R.string.login_hide_password else R.string.login_show_password))
            }
            Button(onClick = onSubmit, enabled = !state.savingPassword, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.profile_change_password))
            }
        }
    }
}

@Composable
private fun Field(field: String, label: String, value: String, errors: Map<String, String>, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        isError = errors.containsKey(field),
        supportingText = { errors[field]?.let { Text(it) } }
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
        supportingText = { state.passwordErrors[field]?.let { Text(it) } }
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
