package bo.org.siafco.app.feature.profile

import android.app.DatePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import bo.org.siafco.app.R
import bo.org.siafco.app.domain.MobileProfile
import bo.org.siafco.app.feature.register.PhotoPreparer
import coil3.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmEmail by remember { mutableStateOf(false) }
    var confirmPassword by remember { mutableStateOf(false) }
    var confirmLogoutAll by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                PhotoPreparer.prepare(context, uri)
                    .onSuccess(viewModel::setPhoto)
            }
        }
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
    if (confirmLogoutAll) ConfirmDialog(
        text = stringResource(R.string.profile_confirm_logout_all),
        onDismiss = { confirmLogoutAll = false },
        onConfirm = {
            confirmLogoutAll = false
            viewModel.logoutAll()
        }
    )

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (state.loading && state.profile == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            state.profile?.let { profile ->
                ReadOnlyProfile(profile)
                EditableProfile(
                    state = state,
                    onPickDate = {
                        val today = LocalDate.now()
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                viewModel.updateForm {
                                    copy(birthDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day))
                                }
                            },
                            today.year,
                            today.monthValue - 1,
                            today.dayOfMonth
                        ).show()
                    },
                    viewModel = viewModel
                )
                Button(
                    onClick = {
                        if (state.form.email != profile.email) confirmEmail = true else viewModel.saveProfile()
                    },
                    enabled = !state.savingProfile,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.profile_save)) }

                PhotoSection(
                    state = state,
                    onPick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onUpload = viewModel::savePhoto
                )

                PasswordSection(
                    state = state,
                    viewModel = viewModel,
                    onSubmit = { confirmPassword = true }
                )

                OutlinedButton(
                    onClick = { confirmLogoutAll = true },
                    enabled = !state.loggingOutAll,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.profile_logout_all)) }
            }

            state.message?.let { Text(stringResource(it.resId), color = MaterialTheme.colorScheme.error) }
            state.messageText?.let { Text(it, color = MaterialTheme.colorScheme.secondary) }
            OutlinedButton(
                onClick = {
                    viewModel.clearSensitiveData()
                    onBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.register_back)) }
        }
    }
}

@Composable
private fun ReadOnlyProfile(profile: MobileProfile) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.profile_readonly), fontWeight = FontWeight.SemiBold)
        InfoLine("Nombre", profile.fullName)
        InfoLine("CI", profile.ci)
        InfoLine("Registro", profile.registrationNumber)
        InfoLine("Sector", profile.sectorName)
        InfoLine("Plan", profile.planName)
        InfoLine("Regional", profile.regional)
        InfoLine("Institución", profile.institution)
        InfoLine("Cargo", profile.position)
        InfoLine("Estado", profile.statusLabel ?: profile.status)
    }
}

@Composable
private fun EditableProfile(state: ProfileUiState, onPickDate: () -> Unit, viewModel: ProfileViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val form = state.form
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.profile_editable), fontWeight = FontWeight.SemiBold)
        Field("phone", stringResource(R.string.register_phone), form.phone, state.fieldErrors, { viewModel.updateForm { copy(phone = it) } })
        Field("email", stringResource(R.string.login_email), form.email, state.fieldErrors, { viewModel.updateForm { copy(email = it) } })
        Field("address", stringResource(R.string.register_address), form.address, state.fieldErrors, { viewModel.updateForm { copy(address = it) } })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = form.birthDate,
                onValueChange = { viewModel.updateForm { copy(birthDate = it) } },
                label = { Text(stringResource(R.string.register_birth_date)) },
                modifier = Modifier.weight(1f),
                isError = state.fieldErrors.containsKey("birth_date"),
                supportingText = { state.fieldErrors["birth_date"]?.let { Text(it) } }
            )
            OutlinedButton(onClick = onPickDate, modifier = Modifier.align(Alignment.CenterVertically)) {
                Text(stringResource(R.string.payment_pick_date))
            }
        }
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(form.maritalStatus.ifBlank { stringResource(R.string.register_marital_status) })
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            state.maritalStatuses.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        viewModel.updateForm { copy(maritalStatus = option) }
                    }
                )
            }
        }
        state.fieldErrors["marital_status"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun PhotoSection(state: ProfileUiState, onPick: () -> Unit, onUpload: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.profile_photo), fontWeight = FontWeight.SemiBold)
        val photo = state.pendingPhoto
        if (photo != null) {
            Image(
                painter = rememberAsyncImagePainter(photo.file),
                contentDescription = stringResource(R.string.register_photo_preview),
                modifier = Modifier.size(120.dp)
            )
        }
        OutlinedButton(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.register_pick_photo))
        }
        Button(onClick = onUpload, enabled = photo != null && !state.savingPhoto, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.profile_upload_photo))
        }
    }
}

@Composable
private fun PasswordSection(state: ProfileUiState, viewModel: ProfileViewModel, onSubmit: () -> Unit) {
    val form = state.passwordForm
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.profile_security), fontWeight = FontWeight.SemiBold)
        Text(stringResource(R.string.profile_password_help), style = MaterialTheme.typography.bodySmall)
        PasswordField("current_password", stringResource(R.string.profile_current_password), form.currentPassword, state, viewModel) {
            viewModel.updatePasswordForm { copy(currentPassword = it) }
        }
        PasswordField("password", stringResource(R.string.profile_new_password), form.password, state, viewModel) {
            viewModel.updatePasswordForm { copy(password = it) }
        }
        PasswordField("password_confirmation", stringResource(R.string.register_password_confirmation), form.passwordConfirmation, state, viewModel) {
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
private fun PasswordField(field: String, label: String, value: String, state: ProfileUiState, viewModel: ProfileViewModel, onChange: (String) -> Unit) {
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
    if (!value.isNullOrBlank()) Text("$label: $value")
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
