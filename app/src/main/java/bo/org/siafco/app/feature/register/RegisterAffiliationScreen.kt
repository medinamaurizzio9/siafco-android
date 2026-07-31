package bo.org.siafco.app.feature.register

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import bo.org.siafco.app.R
import bo.org.siafco.app.domain.CatalogOption
import bo.org.siafco.app.domain.CatalogPlan
import bo.org.siafco.app.domain.CatalogSector
import coil3.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch

@Composable
fun RegisterAffiliationScreen(
    viewModel: RegisterAffiliationViewModel,
    onCompleted: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val invalidPhotoMessage = stringResource(R.string.register_photo_invalid)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                PhotoPreparer.prepare(context, uri)
                    .onSuccess(viewModel::onPhotoPrepared)
                    .onFailure { viewModel.onPhotoError(it.message ?: invalidPhotoMessage) }
            }
        }
    }

    LaunchedEffect(Unit) { viewModel.loadCatalogs() }
    LaunchedEffect(state.completed) {
        if (state.completed) onCompleted()
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.clearSensitiveData() }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.register_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            LinearProgressIndicator(
                progress = { (state.step + 1) / 5f },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.register_step_indicator, state.step + 1, 5, stepTitle(state.step)),
                style = MaterialTheme.typography.titleMedium
            )
            state.message?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
            if (state.loadingCatalogs) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        when (state.step) {
                            0 -> IdentityStep(state, viewModel)
                            1 -> ContactStep(state, viewModel)
                            2 -> InstitutionalStep(state, viewModel)
                            3 -> PhotoStep(state, onPickPhoto = {
                                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            })
                            4 -> ConfirmationStep(state, viewModel)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        if (state.step == 0) {
                            viewModel.clearSensitiveData()
                            onBackToLogin()
                        } else {
                            viewModel.previousStep()
                        }
                    },
                    enabled = !state.submitting,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(if (state.step == 0) R.string.register_back_login else R.string.register_back))
                }
                Button(
                    onClick = { if (state.step == 4) viewModel.submit() else viewModel.nextStep() },
                    enabled = state.canContinue,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(if (state.step == 4) R.string.register_submit else R.string.register_next))
                }
            }
            if (state.accountExists) {
                TextButton(onClick = onBackToLogin, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.register_go_login))
                }
            }
        }
    }
}

@Composable
private fun IdentityStep(state: RegisterAffiliationUiState, viewModel: RegisterAffiliationViewModel) {
    val form = state.form
    Field(R.string.register_full_name, form.fullName, state.fieldErrors["full_name"]) {
        viewModel.updateForm { current -> current.copy(fullName = it) }
    }
    Field(R.string.register_ci, form.ci, state.fieldErrors["ci"]) {
        viewModel.updateForm { current -> current.copy(ci = it) }
    }
    Field(R.string.register_ci_complement, form.ciComplement, null) {
        viewModel.updateForm { current -> current.copy(ciComplement = it) }
    }
    MenuField(
        label = stringResource(R.string.register_issued_in),
        value = state.catalogs?.issuedIn?.firstOrNull { it.value == form.issuedIn }?.label.orEmpty(),
        options = state.catalogs?.issuedIn.orEmpty(),
        error = state.fieldErrors["issued_in"],
        optionLabel = { it.label },
        onSelected = { viewModel.updateForm { current -> current.copy(issuedIn = it.value) } }
    )
    Field(R.string.register_birth_date, form.birthDate, state.fieldErrors["birth_date"], KeyboardType.Number) {
        viewModel.updateForm { current -> current.copy(birthDate = it) }
    }
    MenuField(
        label = stringResource(R.string.register_marital_status),
        value = form.maritalStatus,
        options = state.catalogs?.maritalStatuses.orEmpty(),
        error = state.fieldErrors["marital_status"],
        optionLabel = { it },
        onSelected = { viewModel.updateForm { current -> current.copy(maritalStatus = it) } }
    )
}

@Composable
private fun ContactStep(state: RegisterAffiliationUiState, viewModel: RegisterAffiliationViewModel) {
    val form = state.form
    Field(R.string.register_phone, form.phone, state.fieldErrors["phone"], KeyboardType.Phone) {
        viewModel.updateForm { current -> current.copy(phone = it.filter(Char::isDigit).take(8)) }
    }
    Field(R.string.login_email, form.email, state.fieldErrors["email"], KeyboardType.Email) {
        viewModel.updateForm { current -> current.copy(email = it) }
    }
    Field(R.string.register_address, form.address, state.fieldErrors["address"]) {
        viewModel.updateForm { current -> current.copy(address = it) }
    }
    PasswordField(
        label = stringResource(R.string.login_password),
        value = form.password,
        error = state.fieldErrors["password"],
        visible = state.passwordVisible,
        onToggle = viewModel::togglePasswordVisibility,
        onChange = { viewModel.updateForm { current -> current.copy(password = it) } }
    )
    PasswordField(
        label = stringResource(R.string.register_password_confirmation),
        value = form.passwordConfirmation,
        error = state.fieldErrors["password_confirmation"],
        visible = state.passwordVisible,
        onToggle = viewModel::togglePasswordVisibility,
        onChange = { viewModel.updateForm { current -> current.copy(passwordConfirmation = it) } }
    )
}

@Composable
private fun InstitutionalStep(state: RegisterAffiliationUiState, viewModel: RegisterAffiliationViewModel) {
    val form = state.form
    MenuField(
        label = stringResource(R.string.register_sector),
        value = state.catalogs?.sectors?.firstOrNull { it.id == form.sectorId }?.name.orEmpty(),
        options = state.catalogs?.sectors.orEmpty(),
        error = state.fieldErrors["sector_id"],
        optionLabel = { it.name },
        onSelected = { viewModel.updateForm { current -> current.copy(sectorId = it.id) } }
    )
    MenuField(
        label = stringResource(R.string.register_plan),
        value = state.catalogs?.plans?.firstOrNull { it.id == form.planId }?.name.orEmpty(),
        options = state.catalogs?.plans.orEmpty(),
        error = state.fieldErrors["affiliation_plan_id"],
        optionLabel = { it.name },
        onSelected = { viewModel.updateForm { current -> current.copy(planId = it.id) } }
    )
    PlanSummary(viewModel.selectedPlan())
    MenuField(
        label = stringResource(R.string.register_regional),
        value = form.regional,
        options = state.catalogs?.regionals.orEmpty(),
        error = state.fieldErrors["regional"],
        optionLabel = { it },
        onSelected = { viewModel.updateForm { current -> current.copy(regional = it) } }
    )
    Field(R.string.register_institution, form.institution, state.fieldErrors["institution"]) {
        viewModel.updateForm { current -> current.copy(institution = it) }
    }
    Field(R.string.register_position, form.position, state.fieldErrors["position"]) {
        viewModel.updateForm { current -> current.copy(position = it) }
    }
}

@Composable
private fun PhotoStep(state: RegisterAffiliationUiState, onPickPhoto: () -> Unit) {
    state.form.photo?.let {
        Image(
            painter = rememberAsyncImagePainter(it.file),
            contentDescription = stringResource(R.string.register_photo_preview),
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )
        Text(text = it.displayName)
    }
    state.fieldErrors["photo"]?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
    OutlinedButton(onClick = onPickPhoto, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.register_pick_photo))
    }
    Text(text = stringResource(R.string.register_photo_help), style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun ConfirmationStep(state: RegisterAffiliationUiState, viewModel: RegisterAffiliationViewModel) {
    val form = state.form
    val plan = viewModel.selectedPlan()
    Text(text = form.fullName, fontWeight = FontWeight.Bold)
    Text(text = form.email)
    PlanSummary(plan)
    Text(text = stringResource(R.string.register_terms_version, state.catalogs?.institution?.termsVersion.orEmpty()))
    Text(text = stringResource(R.string.register_privacy_version, state.catalogs?.institution?.privacyVersion.orEmpty()))
    Text(
        text = stringResource(R.string.register_legal_text_pending),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.secondary
    )
    CheckRow(
        checked = form.termsAccepted,
        label = stringResource(R.string.register_accept_terms),
        error = state.fieldErrors["terms_accepted"],
        onChecked = { viewModel.updateForm { current -> current.copy(termsAccepted = it) } }
    )
    CheckRow(
        checked = form.privacyAccepted,
        label = stringResource(R.string.register_accept_privacy),
        error = state.fieldErrors["privacy_accepted"],
        onChecked = { viewModel.updateForm { current -> current.copy(privacyAccepted = it) } }
    )
    if (state.submitting) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun PlanSummary(plan: CatalogPlan?) {
    plan ?: return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = plan.name, fontWeight = FontWeight.SemiBold)
        plan.description?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
        Text(text = "${plan.currency} ${"%.2f".format(plan.totalAmount)}")
    }
}

@Composable
private fun Field(
    labelRes: Int,
    value: String,
    error: String?,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(labelRes)) },
        isError = error != null,
        supportingText = { error?.let { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = labelRes != R.string.register_address
    )
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    error: String?,
    visible: Boolean,
    onToggle: () -> Unit,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        isError = error != null,
        supportingText = { error?.let { Text(it) } },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            TextButton(onClick = onToggle) {
                Text(stringResource(if (visible) R.string.login_hide_password else R.string.login_show_password))
            }
        },
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> MenuField(
    label: String,
    value: String,
    options: List<T>,
    error: String?,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth(),
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            isError = error != null,
            supportingText = { error?.let { Text(it) } }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CheckRow(checked: Boolean, label: String, error: String?, onChecked: (Boolean) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = onChecked)
            Text(text = label)
        }
        error?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun stepTitle(step: Int): String = when (step) {
    0 -> stringResource(R.string.register_step_identity)
    1 -> stringResource(R.string.register_step_contact)
    2 -> stringResource(R.string.register_step_institution)
    3 -> stringResource(R.string.register_step_photo)
    else -> stringResource(R.string.register_step_confirm)
}
