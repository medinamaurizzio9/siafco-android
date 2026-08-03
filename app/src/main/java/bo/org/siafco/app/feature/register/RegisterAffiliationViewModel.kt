package bo.org.siafco.app.feature.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.org.siafco.app.data.repository.AffiliationRegistrationGateway
import bo.org.siafco.app.data.repository.AffiliationRepositoryResult
import bo.org.siafco.app.domain.AffiliationCatalogs
import bo.org.siafco.app.domain.AffiliationRegistrationForm
import bo.org.siafco.app.domain.AffiliationRegistrationSuccess
import bo.org.siafco.app.domain.AffiliationRegistrationValidator
import bo.org.siafco.app.domain.CatalogPlan
import bo.org.siafco.app.domain.PreparedPhoto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegisterAffiliationViewModel(
    private val repository: AffiliationRegistrationGateway
) : ViewModel() {
    private val _state = MutableStateFlow(RegisterAffiliationUiState())
    val state: StateFlow<RegisterAffiliationUiState> = _state.asStateFlow()

    fun loadCatalogs() {
        if (_state.value.catalogs != null || _state.value.loadingCatalogs) return
        viewModelScope.launch {
            _state.value = _state.value.copy(loadingCatalogs = true, message = null)
            _state.value = when (val result = repository.catalogs()) {
                is AffiliationRepositoryResult.Success -> _state.value.copy(
                    catalogs = result.value,
                    loadingCatalogs = false
                )
                AffiliationRepositoryResult.NetworkError -> _state.value.copy(
                    loadingCatalogs = false,
                    message = "No hay conexión con la cooperativa."
                )
                AffiliationRepositoryResult.RateLimited -> _state.value.copy(
                    loadingCatalogs = false,
                    message = "Demasiadas solicitudes. Intenta nuevamente mas tarde."
                )
                else -> _state.value.copy(
                    loadingCatalogs = false,
                    message = "No pudimos cargar los catalogos."
                )
            }
        }
    }

    fun updateForm(transform: (AffiliationRegistrationForm) -> AffiliationRegistrationForm) {
        val currentPhoto = _state.value.form.photo
        val nextForm = transform(_state.value.form)
        if (currentPhoto != nextForm.photo) {
            PhotoPreparer.clear(currentPhoto)
        }
        _state.value = _state.value.copy(form = nextForm, fieldErrors = emptyMap(), message = null)
    }

    fun onPhotoPrepared(photo: PreparedPhoto) {
        updateForm { it.copy(photo = photo) }
    }

    fun onPhotoError(message: String) {
        _state.value = _state.value.copy(fieldErrors = mapOf("photo" to message))
    }

    fun togglePasswordVisibility() {
        _state.value = _state.value.copy(passwordVisible = !_state.value.passwordVisible)
    }

    fun nextStep() {
        val errors = AffiliationRegistrationValidator.validateStep(_state.value.step, _state.value.form)
        if (errors.isNotEmpty()) {
            _state.value = _state.value.copy(fieldErrors = errors)
            return
        }
        _state.value = _state.value.copy(step = (_state.value.step + 1).coerceAtMost(LAST_STEP), fieldErrors = emptyMap())
    }

    fun previousStep() {
        _state.value = _state.value.copy(step = (_state.value.step - 1).coerceAtLeast(0), fieldErrors = emptyMap())
    }

    fun selectedPlan(): CatalogPlan? {
        val form = _state.value.form
        return _state.value.catalogs?.plans?.firstOrNull { it.id == form.planId }
    }

    fun submit() {
        val current = _state.value
        if (current.submitting) return
        val errors = AffiliationRegistrationValidator.validateAll(current.form)
        if (errors.isNotEmpty()) {
            _state.value = current.copy(fieldErrors = errors, step = firstStepWithError(errors))
            return
        }

        _state.value = current.copy(submitting = true, message = null, fieldErrors = emptyMap())
        viewModelScope.launch {
            when (val result = repository.register(current.form)) {
                is AffiliationRepositoryResult.Success -> {
                    PhotoPreparer.clear(current.form.photo)
                    _state.value = RegisterAffiliationUiState(
                        catalogs = current.catalogs,
                        success = result.value,
                        completed = true
                    )
                }
                is AffiliationRepositoryResult.ValidationError -> _state.value = current.copy(
                    submitting = false,
                    fieldErrors = result.errors.mapValues { it.value.firstOrNull().orEmpty() },
                    step = firstStepWithError(result.errors)
                )
                is AffiliationRepositoryResult.Conflict -> {
                    PhotoPreparer.clear(current.form.photo)
                    _state.value = current.copy(
                        form = current.form.withoutSensitiveData(),
                        submitting = false,
                        accountExists = true,
                        message = result.message
                    )
                }
                AffiliationRepositoryResult.RateLimited -> {
                    PhotoPreparer.clear(current.form.photo)
                    _state.value = current.copy(
                        form = current.form.withoutSensitiveData(),
                        submitting = false,
                        message = "Demasiados intentos. Intenta nuevamente mas tarde."
                    )
                }
                AffiliationRepositoryResult.NetworkError -> {
                    _state.value = current.copy(
                        form = current.form.copy(password = "", passwordConfirmation = ""),
                        submitting = false,
                        message = "No hay conexión con la cooperativa. Conservamos los datos no sensibles."
                    )
                }
                else -> {
                    PhotoPreparer.clear(current.form.photo)
                    _state.value = current.copy(
                        form = current.form.withoutSensitiveData(),
                        submitting = false,
                        message = "No pudimos enviar la solicitud."
                    )
                }
            }
        }
    }

    fun clearSensitiveData() {
        PhotoPreparer.clear(_state.value.form.photo)
        _state.value = _state.value.copy(form = _state.value.form.withoutSensitiveData(), passwordVisible = false)
    }

    override fun onCleared() {
        clearSensitiveData()
        super.onCleared()
    }

    private fun firstStepWithError(errors: Map<String, *>): Int {
        return when {
            errors.keys.any { it in STEP_ONE_FIELDS } -> 0
            errors.keys.any { it in STEP_TWO_FIELDS } -> 1
            errors.keys.any { it in STEP_THREE_FIELDS } -> 2
            errors.containsKey("photo") -> 3
            else -> 4
        }
    }

    private companion object {
        const val LAST_STEP = 4
        val STEP_ONE_FIELDS = setOf("full_name", "ci", "issued_in", "birth_date", "marital_status")
        val STEP_TWO_FIELDS = setOf("phone", "email", "address", "password", "password_confirmation")
        val STEP_THREE_FIELDS = setOf("sector_id", "affiliation_plan_id", "regional", "institution", "position")
    }
}

data class RegisterAffiliationUiState(
    val step: Int = 0,
    val form: AffiliationRegistrationForm = AffiliationRegistrationForm(),
    val catalogs: AffiliationCatalogs? = null,
    val loadingCatalogs: Boolean = false,
    val submitting: Boolean = false,
    val passwordVisible: Boolean = false,
    val fieldErrors: Map<String, String> = emptyMap(),
    val message: String? = null,
    val accountExists: Boolean = false,
    val completed: Boolean = false,
    val success: AffiliationRegistrationSuccess? = null
) {
    val canContinue: Boolean get() = !loadingCatalogs && !submitting
}
