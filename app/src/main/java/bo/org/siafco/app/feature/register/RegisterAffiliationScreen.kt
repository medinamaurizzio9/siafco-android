package bo.org.siafco.app.feature.register

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bo.org.siafco.app.R
import bo.org.siafco.app.core.text.HumanTextInputNormalizer
import bo.org.siafco.app.core.text.TextInputNormalization
import bo.org.siafco.app.core.ui.FigmaGold
import bo.org.siafco.app.core.ui.FigmaMuted
import bo.org.siafco.app.core.ui.FigmaNavy
import bo.org.siafco.app.core.ui.FigmaNavyDeep
import bo.org.siafco.app.core.ui.FigmaPrimaryButton
import bo.org.siafco.app.core.ui.FigmaSecondaryButton
import bo.org.siafco.app.domain.CatalogOption
import bo.org.siafco.app.domain.CatalogPlan
import bo.org.siafco.app.domain.CatalogSector
import bo.org.siafco.app.feature.photo.PhotoInputFlow
import coil3.compose.rememberAsyncImagePainter

@Composable
fun RegisterAffiliationScreen(
    viewModel: RegisterAffiliationViewModel,
    onCompleted: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val invalidPhotoMessage = stringResource(R.string.register_photo_invalid)
    var photoFlowVisible by remember { mutableStateOf(false) }
    var confirmExit by remember { mutableStateOf(false) }

    fun requestBack() {
        when (RegisterBackPolicy.decide(state.step)) {
            RegisterBackDecision.ConfirmExit -> confirmExit = true
            RegisterBackDecision.PreviousStep -> viewModel.previousStep()
        }
    }

    BackHandler(enabled = !state.submitting && !photoFlowVisible) {
        requestBack()
    }

    LaunchedEffect(Unit) { viewModel.loadCatalogs() }
    LaunchedEffect(state.completed) {
        if (state.completed) onCompleted()
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.clearSensitiveData() }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            RegisterHeader(onBack = { requestBack() })
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 34.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                RegisterStepIndicator(currentStep = state.step)
                Text(
                    text = "Paso ${state.step + 1}: ${stepTitle(state.step)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = FigmaNavyDeep
                )
                state.message?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
                if (state.loadingCatalogs) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    Card(
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            when (state.step) {
                                0 -> IdentityStep(state, viewModel)
                                1 -> ContactStep(state, viewModel)
                                2 -> InstitutionalStep(state, viewModel)
                                3 -> PhotoStepFigma(state, onPickPhoto = { photoFlowVisible = true })
                                4 -> ConfirmationStepFigma(state, viewModel)
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    if (state.step > 0) {
                        FigmaSecondaryButton(
                            text = stringResource(R.string.register_back),
                            onClick = { requestBack() },
                            enabled = !state.submitting,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    FigmaPrimaryButton(
                        text = stringResource(if (state.step == 4) R.string.register_submit else R.string.register_next),
                        onClick = { if (state.step == 4) viewModel.submit() else viewModel.nextStep() },
                        enabled = state.canContinue,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (state.accountExists) {
                    TextButton(onClick = onBackToLogin, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.register_go_login))
                    }
                }
            }
        }
    }
    PhotoInputFlow(
        visible = photoFlowVisible,
        onDismiss = { photoFlowVisible = false },
        onPrepared = viewModel::onPhotoPrepared,
        onError = { viewModel.onPhotoError(it.ifBlank { invalidPhotoMessage }) }
    )
    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Abandonar afiliación") },
            text = { Text("Se borrará la contraseña y la fotografía temporal antes de volver al inicio de sesión.") },
            confirmButton = {
                Button(onClick = {
                    confirmExit = false
                    viewModel.clearSensitiveData()
                    onBackToLogin()
                }) {
                    Text("Salir")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmExit = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun RegisterHeader(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(FigmaNavyDeep, RoundedCornerShape(bottomStart = 38.dp, bottomEnd = 38.dp))
            .padding(horizontal = 24.dp, vertical = 38.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                onClick = onBack,
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.register_back),
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Text(
                text = "Nueva Afiliación",
                modifier = Modifier.weight(1f),
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Box(modifier = Modifier.size(56.dp))
        }
    }
}

@Composable
private fun RegisterStepIndicator(currentStep: Int) {
    val labels = listOf("Personal", "Contacto", "Laboral", "Foto", "Confirmar")
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        labels.forEachIndexed { index, label ->
            val completed = index < currentStep
            val selected = index == currentStep
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .background(if (completed || selected) FigmaGold else FigmaMuted.copy(alpha = 0.55f))
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(if (selected || completed) FigmaGold else Color.White, CircleShape)
                            .border(2.dp, if (selected || completed) FigmaGold else FigmaMuted, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (index + 1).toString(),
                            color = if (selected || completed) FigmaNavyDeep else FigmaMuted,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                    if (index < labels.lastIndex) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .background(if (completed) FigmaGold else FigmaMuted.copy(alpha = 0.55f))
                        )
                    }
                }
                Text(
                    text = label,
                    color = if (selected) FigmaGold else FigmaMuted,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun IdentityStep(state: RegisterAffiliationUiState, viewModel: RegisterAffiliationViewModel) {
    val form = state.form
    Field(R.string.register_full_name, form.fullName, state.fieldErrors["full_name"], humanText = true) {
        viewModel.updateForm { current -> current.copy(fullName = it) }
    }
    Field(R.string.register_ci, form.ci, state.fieldErrors["ci"], KeyboardType.Number) {
        viewModel.updateForm { current -> current.copy(ci = it) }
    }
    Field(R.string.register_ci_complement, form.ciComplement, null, humanText = true) {
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
    Field(R.string.register_address, form.address, state.fieldErrors["address"], humanText = true) {
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
    Field(R.string.register_institution, form.institution, state.fieldErrors["institution"], humanText = true) {
        viewModel.updateForm { current -> current.copy(institution = it) }
    }
    Field(R.string.register_position, form.position, state.fieldErrors["position"], humanText = true) {
        viewModel.updateForm { current -> current.copy(position = it) }
    }
}

@Composable
private fun PhotoStepFigma(state: RegisterAffiliationUiState, onPickPhoto: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F9FC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val photo = state.form.photo
            if (photo == null) {
                Box(
                    modifier = Modifier
                        .size(132.dp)
                        .background(FigmaGold.copy(alpha = 0.16f), CircleShape)
                        .border(2.dp, FigmaGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_profile_camera),
                        contentDescription = null,
                        tint = FigmaNavy,
                        modifier = Modifier.size(44.dp)
                    )
                }
                Text(
                    text = "Fotografía del afiliado",
                    color = FigmaNavyDeep,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
            } else {
                Image(
                    painter = rememberAsyncImagePainter(photo.file),
                    contentDescription = stringResource(R.string.register_photo_preview),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(RoundedCornerShape(22.dp)),
                    contentScale = ContentScale.Crop
                )
                Text(text = photo.displayName, fontWeight = FontWeight.Bold, color = FigmaNavyDeep)
                Text(text = "Resolución: ${photo.width} × ${photo.height} px", style = MaterialTheme.typography.bodySmall)
                Text(text = "Tamaño optimizado: ${formatPhotoSize(photo.sizeBytes)}", style = MaterialTheme.typography.bodySmall)
                Text(text = "Esta fotografía se utilizará en tu perfil y credencial.", style = MaterialTheme.typography.bodySmall)
            }
            state.fieldErrors["photo"]?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
            FigmaSecondaryButton(
                text = stringResource(R.string.register_pick_photo),
                onClick = onPickPhoto,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.register_photo_help),
                style = MaterialTheme.typography.bodySmall,
                color = FigmaMuted,
                textAlign = TextAlign.Center
            )
        }
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
        Text(text = "Resolución: ${it.width} × ${it.height} px", style = MaterialTheme.typography.bodySmall)
        Text(text = "Tamaño optimizado: ${formatPhotoSize(it.sizeBytes)}", style = MaterialTheme.typography.bodySmall)
        Text(text = "Esta fotografía se utilizará en tu perfil y credencial.", style = MaterialTheme.typography.bodySmall)
    }
    state.fieldErrors["photo"]?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
    OutlinedButton(onClick = onPickPhoto, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.register_pick_photo))
    }
    Text(text = stringResource(R.string.register_photo_help), style = MaterialTheme.typography.bodySmall)
}

private fun formatPhotoSize(sizeBytes: Long): String {
    val kb = sizeBytes / 1024.0
    return if (kb < 1024) "%.0f KB".format(kb) else "%.1f MB".format(kb / 1024.0)
}

@Composable
private fun ConfirmationStepFigma(state: RegisterAffiliationUiState, viewModel: RegisterAffiliationViewModel) {
    val form = state.form
    val plan = viewModel.selectedPlan()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F9FC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Revisa tu solicitud", color = FigmaNavyDeep, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            SummaryLine("Nombre completo", form.fullName)
            SummaryLine("Correo electrónico", form.email)
            SummaryLine("Teléfono", form.phone)
            SummaryLine("Dirección", form.address)
            SummaryLine("Regional", form.regional)
            SummaryLine("Institución", form.institution)
            SummaryLine("Cargo", form.position)
            PlanSummary(plan)
        }
    }
    Text(text = stringResource(R.string.register_terms_version, state.catalogs?.institution?.termsVersion.orEmpty()))
    Text(text = stringResource(R.string.register_privacy_version, state.catalogs?.institution?.privacyVersion.orEmpty()))
    Text(
        text = stringResource(R.string.register_legal_text_pending),
        style = MaterialTheme.typography.bodySmall,
        color = FigmaMuted
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
private fun SummaryLine(label: String, value: String) {
    if (value.isBlank()) return
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(label, color = FigmaMuted, modifier = Modifier.weight(0.42f))
        Text(value, color = FigmaNavyDeep, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.58f))
    }
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
    humanText: Boolean = false,
    onChange: (String) -> Unit
) {
    val normalization = if (humanText) TextInputNormalization.Human else TextInputNormalization.None
    var fieldValue by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    LaunchedEffect(value) {
        if (value != fieldValue.text) {
            fieldValue = fieldValue.copy(text = value, selection = TextRange(value.length), composition = null)
        }
    }
    OutlinedTextField(
        value = fieldValue,
        onValueChange = { incoming ->
            val normalized = HumanTextInputNormalizer.visual(incoming, normalization)
            fieldValue = normalized
            if (normalized.text != value) onChange(normalized.text)
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(labelRes)) },
        isError = error != null,
        supportingText = { error?.let { Text(it) } },
        keyboardOptions = when (normalization) {
            TextInputNormalization.Human,
            TextInputNormalization.Coupon -> KeyboardOptions(keyboardType = keyboardType, capitalization = KeyboardCapitalization.Characters)
            TextInputNormalization.None -> KeyboardOptions(keyboardType = keyboardType)
        },
        singleLine = labelRes != R.string.register_address,
        leadingIcon = { FieldIcon(labelRes) },
        shape = RoundedCornerShape(22.dp),
        colors = registerTextFieldColors()
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
        leadingIcon = {
            Icon(painter = painterResource(R.drawable.ic_lock), contentDescription = null, tint = FigmaNavy)
        },
        trailingIcon = {
            IconButton(onClick = onToggle) {
                Icon(
                    painter = painterResource(if (visible) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
                    contentDescription = stringResource(if (visible) R.string.login_hide_password else R.string.login_show_password),
                    tint = FigmaNavy
                )
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(22.dp),
        colors = registerTextFieldColors()
    )
}

@Composable
private fun FieldIcon(labelRes: Int) {
    val icon = when (labelRes) {
        R.string.register_full_name -> R.drawable.ic_nav_profile
        R.string.register_ci,
        R.string.register_ci_complement -> R.drawable.ic_profile_shield
        R.string.register_birth_date -> R.drawable.ic_profile_calendar
        R.string.register_phone -> R.drawable.ic_profile_phone
        R.string.login_email -> R.drawable.ic_mail
        R.string.register_address -> R.drawable.ic_profile_location
        R.string.register_institution -> R.drawable.ic_profile_shield
        R.string.register_position -> R.drawable.ic_profile_edit
        else -> R.drawable.ic_service_request
    }
    Icon(painter = painterResource(icon), contentDescription = null, tint = FigmaNavy)
}

@Composable
private fun registerTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    disabledContainerColor = Color(0xFFE9EDF3),
    errorContainerColor = Color.White,
    focusedIndicatorColor = FigmaGold,
    unfocusedIndicatorColor = FigmaGold,
    errorIndicatorColor = MaterialTheme.colorScheme.error,
    focusedLabelColor = FigmaMuted,
    unfocusedLabelColor = FigmaMuted,
    cursorColor = FigmaNavy
)

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
            leadingIcon = {
                Icon(painter = painterResource(menuIcon(label)), contentDescription = null, tint = FigmaNavy)
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            isError = error != null,
            supportingText = { error?.let { Text(it) } },
            shape = RoundedCornerShape(22.dp),
            colors = registerTextFieldColors()
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

private fun menuIcon(label: String): Int = when {
    label.contains("expedido", ignoreCase = true) -> R.drawable.ic_profile_shield
    label.contains("estado", ignoreCase = true) -> R.drawable.ic_profile_shield
    label.contains("sector", ignoreCase = true) -> R.drawable.ic_nav_store
    label.contains("plan", ignoreCase = true) -> R.drawable.ic_service_request
    label.contains("regional", ignoreCase = true) -> R.drawable.ic_profile_location
    else -> R.drawable.ic_service_request
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
