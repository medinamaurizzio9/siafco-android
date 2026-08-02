package bo.org.siafco.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.org.siafco.app.data.repository.ProfileGateway
import bo.org.siafco.app.data.repository.ProfileResult
import bo.org.siafco.app.domain.ChangePasswordForm
import bo.org.siafco.app.domain.MobileProfile
import bo.org.siafco.app.domain.PreparedPhoto
import bo.org.siafco.app.domain.ProfileUpdateForm
import bo.org.siafco.app.feature.UiMessage
import bo.org.siafco.app.feature.register.PhotoPreparer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class ProfileViewModel(private val repository: ProfileGateway) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    fun load() {
        if (_state.value.loaded || _state.value.loading) return
        _state.value = _state.value.copy(loading = true, message = null)
        viewModelScope.launch {
            val catalogs = repository.loadMaritalStatuses()
            when (val result = repository.load()) {
                is ProfileResult.Success -> _state.value = _state.value.copy(
                    loaded = true,
                    loading = false,
                    profile = result.profile,
                    form = result.profile.toForm(),
                    maritalStatuses = catalogs
                )
                else -> applyResult(result, loading = false)
            }
        }
    }

    fun updateForm(block: ProfileUpdateForm.() -> ProfileUpdateForm) {
        _state.value = _state.value.copy(form = _state.value.form.block(), fieldErrors = emptyMap(), message = null)
    }

    fun setPhoto(photo: PreparedPhoto?) {
        PhotoPreparer.clear(_state.value.pendingPhoto)
        _state.value = _state.value.copy(pendingPhoto = photo, fieldErrors = emptyMap(), message = null)
    }

    fun onPhotoError(message: String) {
        _state.value = _state.value.copy(
            savingPhoto = false,
            fieldErrors = mapOf("photo" to message),
            messageText = message
        )
    }

    fun saveProfile() {
        val current = _state.value
        if (current.savingProfile) return
        val errors = validateProfile(current.form)
        if (errors.isNotEmpty()) {
            _state.value = current.copy(fieldErrors = errors, message = UiMessage.Validation)
            return
        }
        _state.value = current.copy(savingProfile = true, fieldErrors = emptyMap(), message = null, messageText = null)
        viewModelScope.launch {
            when (val result = repository.updateProfile(current.form)) {
                is ProfileResult.Success -> confirmProfileUpdate(current.form)
                else -> applyResult(result, savingProfile = false)
            }
        }
    }

    fun savePhoto() {
        val current = _state.value
        val photo = current.pendingPhoto ?: return
        if (current.savingPhoto) return
        _state.value = current.copy(savingPhoto = true, fieldErrors = emptyMap(), message = null, messageText = null)
        viewModelScope.launch {
            when (val result = repository.updatePhoto(photo)) {
                is ProfileResult.Success -> confirmPhotoUpdate(photo, current.profile?.photoUrl)
                is ProfileResult.ValidationError, is ProfileResult.Forbidden, is ProfileResult.RateLimited, is ProfileResult.HttpError, ProfileResult.Unauthorized -> {
                    PhotoPreparer.clear(photo)
                    _state.value = _state.value.copy(pendingPhoto = null)
                    applyResult(result, savingPhoto = false)
                }
                ProfileResult.NetworkError -> applyResult(result, savingPhoto = false)
                ProfileResult.UnknownError -> {
                    PhotoPreparer.clear(photo)
                    _state.value = _state.value.copy(pendingPhoto = null)
                    applyResult(result, savingPhoto = false)
                }
                ProfileResult.LoggedOut -> applyResult(result, savingPhoto = false)
            }
        }
    }

    fun updatePasswordForm(block: ChangePasswordForm.() -> ChangePasswordForm) {
        _state.value = _state.value.copy(passwordForm = _state.value.passwordForm.block(), passwordErrors = emptyMap(), message = null)
    }

    fun togglePasswordVisibility() {
        _state.value = _state.value.copy(passwordVisible = !_state.value.passwordVisible)
    }

    fun changePassword() {
        val current = _state.value
        if (current.savingPassword) return
        val errors = validatePassword(current.passwordForm)
        if (errors.isNotEmpty()) {
            _state.value = current.copy(passwordErrors = errors, message = UiMessage.Validation)
            return
        }
        _state.value = current.copy(savingPassword = true, passwordErrors = emptyMap(), message = null)
        viewModelScope.launch {
            when (val result = repository.updatePassword(current.passwordForm)) {
                is ProfileResult.Success -> _state.value = _state.value.copy(
                    savingPassword = false,
                    profile = result.profile,
                    passwordForm = ChangePasswordForm(),
                    passwordVisible = false,
                    messageText = "Contrasena actualizada. La sesion actual continua activa."
                )
                else -> {
                    if (result is ProfileResult.Unauthorized) {
                        _state.value = _state.value.copy(passwordForm = ChangePasswordForm(), passwordVisible = false)
                    }
                    applyResult(result, savingPassword = false)
                }
            }
        }
    }

    fun logoutAll() {
        if (_state.value.loggingOutAll) return
        _state.value = _state.value.copy(loggingOutAll = true, message = null)
        viewModelScope.launch {
            when (val result = repository.logoutAll()) {
                ProfileResult.LoggedOut -> _state.value = ProfileUiState(loaded = true, loggedOut = true)
                ProfileResult.NetworkError -> _state.value = _state.value.copy(
                    loggingOutAll = false,
                    message = UiMessage.Network,
                    messageText = "No se pudo cerrar sesiones remotas. Podrian seguir activas."
                )
                else -> applyResult(result, loggingOutAll = false)
            }
        }
    }

    fun clearSensitiveData() {
        PhotoPreparer.clear(_state.value.pendingPhoto)
        _state.value = _state.value.copy(pendingPhoto = null, passwordForm = ChangePasswordForm(), passwordVisible = false)
    }

    private suspend fun confirmProfileUpdate(expected: ProfileUpdateForm) {
        when (val refreshed = repository.load()) {
            is ProfileResult.Success -> {
                if (refreshed.profile.matches(expected)) {
                    _state.value = _state.value.copy(
                        savingProfile = false,
                        profile = refreshed.profile,
                        form = refreshed.profile.toForm(),
                        messageText = "Perfil actualizado."
                    )
                } else {
                    _state.value = _state.value.copy(
                        savingProfile = false,
                        message = UiMessage.Unknown,
                        messageText = "El servidor respondio, pero el perfil no refleja los cambios. Intenta nuevamente."
                    )
                }
            }
            else -> applyResult(refreshed, savingProfile = false)
        }
    }

    private suspend fun confirmPhotoUpdate(photo: PreparedPhoto, previousPhotoUrl: String?) {
        when (val refreshed = repository.load()) {
            is ProfileResult.Success -> {
                val confirmedPhotoUrl = refreshed.profile.photoUrl
                if (!confirmedPhotoUrl.isNullOrBlank() && confirmedPhotoUrl != previousPhotoUrl) {
                    PhotoPreparer.clear(photo)
                    _state.value = _state.value.copy(
                        savingPhoto = false,
                        pendingPhoto = null,
                        profile = refreshed.profile,
                        form = refreshed.profile.toForm(),
                        messageText = "Fotografia actualizada."
                    )
                } else {
                    _state.value = _state.value.copy(
                        savingPhoto = false,
                        fieldErrors = mapOf("photo" to "El servidor respondio, pero la fotografia oficial no cambio. Intenta subirla nuevamente."),
                        message = UiMessage.Unknown,
                        messageText = "La fotografia todavia no fue confirmada por el servidor."
                    )
                }
            }
            else -> applyResult(refreshed, savingPhoto = false)
        }
    }

    private suspend fun applyResult(
        result: ProfileResult,
        loading: Boolean? = null,
        savingProfile: Boolean? = null,
        savingPhoto: Boolean? = null,
        savingPassword: Boolean? = null,
        loggingOutAll: Boolean? = null
    ) {
        when (result) {
            is ProfileResult.ValidationError -> _state.value = _state.value.copy(
                loading = loading ?: _state.value.loading,
                savingProfile = savingProfile ?: _state.value.savingProfile,
                savingPhoto = savingPhoto ?: _state.value.savingPhoto,
                savingPassword = savingPassword ?: _state.value.savingPassword,
                loggingOutAll = loggingOutAll ?: _state.value.loggingOutAll,
                fieldErrors = result.errors.mapValues { it.value.firstOrNull().orEmpty() },
                passwordErrors = result.errors.mapValues { it.value.firstOrNull().orEmpty() },
                message = UiMessage.Validation
            )
            ProfileResult.Unauthorized -> {
                repository.clearLocalSession()
                _state.value = ProfileUiState(loaded = true, loggedOut = true)
            }
            is ProfileResult.Forbidden -> _state.value = _state.value.withFlags(loading, savingProfile, savingPhoto, savingPassword, loggingOutAll).copy(
                message = UiMessage.Forbidden,
                messageText = result.message
            )
            is ProfileResult.RateLimited -> _state.value = _state.value.withFlags(loading, savingProfile, savingPhoto, savingPassword, loggingOutAll).copy(
                message = UiMessage.RateLimited,
                messageText = result.message
            )
            ProfileResult.NetworkError -> _state.value = _state.value.withFlags(loading, savingProfile, savingPhoto, savingPassword, loggingOutAll).copy(message = UiMessage.Network)
            ProfileResult.UnknownError, is ProfileResult.HttpError -> _state.value = _state.value.withFlags(loading, savingProfile, savingPhoto, savingPassword, loggingOutAll).copy(message = UiMessage.Unknown)
            is ProfileResult.Success, ProfileResult.LoggedOut -> Unit
        }
    }

    private fun ProfileUiState.withFlags(
        loading: Boolean?,
        savingProfile: Boolean?,
        savingPhoto: Boolean?,
        savingPassword: Boolean?,
        loggingOutAll: Boolean?
    ) = copy(
        loading = loading ?: this.loading,
        savingProfile = savingProfile ?: this.savingProfile,
        savingPhoto = savingPhoto ?: this.savingPhoto,
        savingPassword = savingPassword ?: this.savingPassword,
        loggingOutAll = loggingOutAll ?: this.loggingOutAll
    )

    private fun validateProfile(form: ProfileUpdateForm): Map<String, String> = buildMap {
        if (form.email.isBlank() || !form.email.contains("@")) put("email", "Ingresa un correo valido.")
        if (form.phone.any { !(it.isDigit() || it in "+(). -") }) put("phone", "Ingresa un telefono valido.")
        val birthDate = form.birthDate.takeIf(String::isNotBlank)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (form.birthDate.isNotBlank() && birthDate == null) put("birth_date", "Ingresa una fecha valida.")
        if (birthDate != null && !birthDate.isBefore(LocalDate.now())) put("birth_date", "La fecha debe ser anterior a hoy.")
    }

    private fun validatePassword(form: ChangePasswordForm): Map<String, String> = buildMap {
        if (form.currentPassword.isBlank()) put("current_password", "Ingresa tu contrasena actual.")
        if (form.password.length < 8 || !form.password.any(Char::isLetter) || !form.password.any(Char::isDigit)) {
            put("password", "La contrasena debe tener al menos 8 caracteres, letras y numeros.")
        }
        if (form.passwordConfirmation.isBlank()) put("password_confirmation", "Confirma la nueva contrasena.")
        if (form.password != form.passwordConfirmation) put("password_confirmation", "Las contrasenas no coinciden.")
    }

    private fun MobileProfile.toForm() = ProfileUpdateForm(
        phone = phone.orEmpty(),
        email = email,
        address = address.orEmpty(),
        birthDate = birthDate.orEmpty(),
        maritalStatus = maritalStatus.orEmpty()
    )

    private fun MobileProfile.matches(expected: ProfileUpdateForm): Boolean =
        phone.sameProfileValue(expected.phone) &&
            email == expected.email &&
            address.sameProfileValue(expected.address) &&
            birthDate.sameProfileValue(expected.birthDate) &&
            maritalStatus.sameProfileValue(expected.maritalStatus)

    private fun String?.sameProfileValue(expected: String): Boolean =
        orEmpty().trim() == expected.trim()
}

data class ProfileUiState(
    val loaded: Boolean = false,
    val loading: Boolean = false,
    val profile: MobileProfile? = null,
    val form: ProfileUpdateForm = ProfileUpdateForm(),
    val maritalStatuses: List<String> = emptyList(),
    val pendingPhoto: PreparedPhoto? = null,
    val passwordForm: ChangePasswordForm = ChangePasswordForm(),
    val passwordVisible: Boolean = false,
    val savingProfile: Boolean = false,
    val savingPhoto: Boolean = false,
    val savingPassword: Boolean = false,
    val loggingOutAll: Boolean = false,
    val loggedOut: Boolean = false,
    val message: UiMessage? = null,
    val messageText: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    val passwordErrors: Map<String, String> = emptyMap()
)
