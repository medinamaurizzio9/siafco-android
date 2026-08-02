package bo.org.siafco.app

import bo.org.siafco.app.data.repository.ProfileGateway
import bo.org.siafco.app.data.repository.ProfileResult
import bo.org.siafco.app.domain.ChangePasswordForm
import bo.org.siafco.app.domain.MobileProfile
import bo.org.siafco.app.domain.PreparedPhoto
import bo.org.siafco.app.domain.ProfileUpdateForm
import bo.org.siafco.app.feature.UiMessage
import bo.org.siafco.app.feature.profile.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadGetsProfileAndEditableCatalogs() = runTest(dispatcher) {
        val viewModel = ProfileViewModel(FakeProfileGateway())

        viewModel.load()
        advanceUntilIdle()

        assertEquals("profile@siafco.test", viewModel.state.value.form.email)
        assertEquals(listOf("SOLTERO", "CASADO"), viewModel.state.value.maritalStatuses)
        assertEquals("activo", viewModel.state.value.profile?.status)
    }

    @Test
    fun saveProfileDoesNotSubmitTwiceWhileSaving() = runTest(dispatcher) {
        val gateway = FakeProfileGateway(
            updateResult = ProfileResult.Success(profile(email = "new@siafco.test")),
            loadAfterUpdate = ProfileResult.Success(profile(email = "new@siafco.test")),
            delayUpdates = true
        )
        val viewModel = ProfileViewModel(gateway)

        viewModel.load()
        advanceUntilIdle()
        viewModel.updateForm { copy(email = "new@siafco.test") }
        viewModel.saveProfile()
        viewModel.saveProfile()
        advanceUntilIdle()

        assertEquals(1, gateway.updateProfileCalls)
        assertEquals("new@siafco.test", viewModel.state.value.profile?.email)
    }

    @Test
    fun savePhotoSuccessClearsTemporaryPhoto() = runTest(dispatcher) {
        val photo = photo()
        val viewModel = ProfileViewModel(FakeProfileGateway(
            photoResult = ProfileResult.Success(profile(photoUrl = "https://siafco.test/photo.jpg?v=2")),
            loadAfterPhoto = ProfileResult.Success(profile(photoUrl = "https://siafco.test/photo.jpg?v=2"))
        ))

        viewModel.setPhoto(photo)
        viewModel.savePhoto()
        advanceUntilIdle()

        assertNull(viewModel.state.value.pendingPhoto)
        assertFalse(photo.file.exists())
        assertEquals("https://siafco.test/photo.jpg?v=2", viewModel.state.value.profile?.photoUrl)
    }

    @Test
    fun saveProfileRequiresFreshProfileToReflectChanges() = runTest(dispatcher) {
        val gateway = FakeProfileGateway(
            updateResult = ProfileResult.Success(profile(phone = "76543210")),
            loadAfterUpdate = ProfileResult.Success(profile(phone = "70000000"))
        )
        val viewModel = ProfileViewModel(gateway)

        viewModel.load()
        advanceUntilIdle()
        viewModel.updateForm { copy(phone = "76543210") }
        viewModel.saveProfile()
        advanceUntilIdle()

        assertEquals(UiMessage.Unknown, viewModel.state.value.message)
        assertEquals("70000000", viewModel.state.value.profile?.phone)
        assertEquals("76543210", viewModel.state.value.form.phone)
    }

    @Test
    fun savePhotoKeepsTemporaryPhotoWhenFreshProfileDoesNotChangeUrl() = runTest(dispatcher) {
        val photo = photo()
        val gateway = FakeProfileGateway(
            loadResult = ProfileResult.Success(profile(photoUrl = "https://siafco.test/photo.jpg?v=1")),
            photoResult = ProfileResult.Success(profile(photoUrl = "https://siafco.test/photo.jpg?v=2")),
            loadAfterPhoto = ProfileResult.Success(profile(photoUrl = "https://siafco.test/photo.jpg?v=1"))
        )
        val viewModel = ProfileViewModel(gateway)

        viewModel.load()
        advanceUntilIdle()
        viewModel.setPhoto(photo)
        viewModel.savePhoto()
        advanceUntilIdle()

        assertEquals(UiMessage.Unknown, viewModel.state.value.message)
        assertEquals(photo, viewModel.state.value.pendingPhoto)
        assertTrue(photo.file.exists())
    }

    @Test
    fun selectedPhotoStaysReadableForPreviewBeforeUpload() = runTest(dispatcher) {
        val photo = photo()
        val viewModel = ProfileViewModel(FakeProfileGateway())

        viewModel.setPhoto(photo)

        assertEquals(photo, viewModel.state.value.pendingPhoto)
        assertTrue(photo.file.exists())
        assertTrue(photo.file.canRead())
    }

    @Test
    fun replacingPendingPhotoClearsOnlyPreviousProcessedFile() = runTest(dispatcher) {
        val first = photo()
        val second = photo()
        val viewModel = ProfileViewModel(FakeProfileGateway())

        viewModel.setPhoto(first)
        viewModel.setPhoto(second)

        assertFalse(first.file.exists())
        assertTrue(second.file.exists())
        assertEquals(second, viewModel.state.value.pendingPhoto)
    }

    @Test
    fun savePhotoNetworkErrorKeepsTemporaryPhotoAndSession() = runTest(dispatcher) {
        val photo = photo()
        val gateway = FakeProfileGateway(photoResult = ProfileResult.NetworkError)
        val viewModel = ProfileViewModel(gateway)

        viewModel.setPhoto(photo)
        viewModel.savePhoto()
        advanceUntilIdle()

        assertEquals(UiMessage.Network, viewModel.state.value.message)
        assertFalse(viewModel.state.value.loggedOut)
        assertTrue(photo.file.exists())
        assertEquals(photo, viewModel.state.value.pendingPhoto)
    }

    @Test
    fun changePasswordSuccessClearsPasswordForm() = runTest(dispatcher) {
        val viewModel = ProfileViewModel(FakeProfileGateway(passwordResult = ProfileResult.Success(profile())))

        viewModel.updatePasswordForm {
            copy(currentPassword = "Secret1234", password = "Better1234", passwordConfirmation = "Better1234")
        }
        viewModel.changePassword()
        advanceUntilIdle()

        assertEquals(ChangePasswordForm(), viewModel.state.value.passwordForm)
        assertFalse(viewModel.state.value.passwordVisible)
    }

    @Test
    fun unauthorizedClearsLocalSessionAndLogsOut() = runTest(dispatcher) {
        val gateway = FakeProfileGateway(updateResult = ProfileResult.Unauthorized)
        val viewModel = ProfileViewModel(gateway)

        viewModel.load()
        advanceUntilIdle()
        viewModel.saveProfile()
        advanceUntilIdle()

        assertTrue(gateway.clearLocalSessionCalled)
        assertTrue(viewModel.state.value.loggedOut)
    }

    private class FakeProfileGateway(
        private val loadResult: ProfileResult = ProfileResult.Success(profile()),
        private val loadAfterUpdate: ProfileResult? = null,
        private val loadAfterPhoto: ProfileResult? = null,
        private val updateResult: ProfileResult = ProfileResult.Success(profile()),
        private val photoResult: ProfileResult = ProfileResult.Success(profile()),
        private val passwordResult: ProfileResult = ProfileResult.Success(profile()),
        private val logoutAllResult: ProfileResult = ProfileResult.LoggedOut,
        private val delayUpdates: Boolean = false
    ) : ProfileGateway {
        var updateProfileCalls = 0
        var clearLocalSessionCalled = false
        private var profileLoadCalls = 0
        private var lastOperation: String? = null

        override suspend fun load(): ProfileResult {
            profileLoadCalls++
            return when (lastOperation) {
                "profile" -> loadAfterUpdate ?: loadResult
                "photo" -> loadAfterPhoto ?: loadResult
                else -> loadResult
            }
        }
        override suspend fun loadMaritalStatuses(): List<String> = listOf("SOLTERO", "CASADO")

        override suspend fun updateProfile(changes: ProfileUpdateForm): ProfileResult {
            updateProfileCalls++
            if (delayUpdates) delay(100)
            lastOperation = "profile"
            return updateResult
        }

        override suspend fun updatePhoto(photo: PreparedPhoto): ProfileResult {
            lastOperation = "photo"
            return photoResult
        }
        override suspend fun updatePassword(form: ChangePasswordForm): ProfileResult = passwordResult
        override suspend fun logoutAll(): ProfileResult = logoutAllResult

        override suspend fun clearLocalSession() {
            clearLocalSessionCalled = true
        }
    }

    private companion object {
        fun profile(
            email: String = "profile@siafco.test",
            phone: String = "70000000",
            photoUrl: String? = null
        ) = MobileProfile(
            fullName = "Afiliado Demo",
            ci = "90010001",
            email = email,
            phone = phone,
            address = "Direccion",
            birthDate = "1990-01-01",
            maritalStatus = "SOLTERO",
            photoUrl = photoUrl,
            registrationNumber = "MAG-1",
            sectorName = "Magisterio",
            planName = "Plan base",
            regional = "LA PAZ",
            institution = "Institucion",
            position = null,
            status = "activo",
            statusLabel = "Activo",
            allowedFields = setOf("phone", "email", "address", "birth_date", "marital_status"),
            mustChangePassword = false
        )

        fun photo(): PreparedPhoto {
            val file = File.createTempFile("profile", ".jpg").apply {
                writeBytes(byteArrayOf(1, 2, 3, 4))
                deleteOnExit()
            }
            return PreparedPhoto(file, "profile.jpg")
        }
    }
}
