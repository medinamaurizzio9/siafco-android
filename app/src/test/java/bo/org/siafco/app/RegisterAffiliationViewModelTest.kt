package bo.org.siafco.app

import bo.org.siafco.app.data.repository.AffiliationRegistrationGateway
import bo.org.siafco.app.data.repository.AffiliationRepositoryResult
import bo.org.siafco.app.domain.AccessLevel
import bo.org.siafco.app.domain.AffiliationCatalogs
import bo.org.siafco.app.domain.AffiliationRegistrationForm
import bo.org.siafco.app.domain.AffiliationRegistrationSuccess
import bo.org.siafco.app.domain.PreparedPhoto
import bo.org.siafco.app.domain.SessionProfile
import bo.org.siafco.app.feature.register.RegisterAffiliationViewModel
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
class RegisterAffiliationViewModelTest {
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
    fun submitIsGuardedAgainstDoubleTap() = runTest(dispatcher) {
        val repository = FakeAffiliationGateway(
            registerResult = AffiliationRepositoryResult.Success(success()),
            delayRegistration = true
        )
        val viewModel = RegisterAffiliationViewModel(repository)
        viewModel.updateForm { validForm() }

        viewModel.submit()
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(1, repository.registerCalls)
        assertTrue(viewModel.state.value.completed)
    }

    @Test
    fun successfulSubmitClearsPasswordAndTemporaryPhoto() = runTest(dispatcher) {
        val repository = FakeAffiliationGateway(AffiliationRepositoryResult.Success(success()))
        val photo = tempPhoto()
        val viewModel = RegisterAffiliationViewModel(repository)
        viewModel.updateForm { validForm(photo) }

        viewModel.submit()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.completed)
        assertEquals("", viewModel.state.value.form.password)
        assertEquals("", viewModel.state.value.form.passwordConfirmation)
        assertNull(viewModel.state.value.form.photo)
        assertFalse(photo.file.exists())
    }

    @Test
    fun networkFailurePreservesOnlyNonSensitiveFormData() = runTest(dispatcher) {
        val repository = FakeAffiliationGateway(AffiliationRepositoryResult.NetworkError)
        val photo = tempPhoto()
        val viewModel = RegisterAffiliationViewModel(repository)
        viewModel.updateForm { validForm(photo) }

        viewModel.submit()
        advanceUntilIdle()

        val form = viewModel.state.value.form
        assertEquals("Ana Perez", form.fullName)
        assertEquals("70000001", form.phone)
        assertEquals("", form.password)
        assertEquals("", form.passwordConfirmation)
        assertNull(form.photo)
        assertFalse(photo.file.exists())
    }

    @Test
    fun conflictMarksExistingAccountAndClearsSensitiveData() = runTest(dispatcher) {
        val repository = FakeAffiliationGateway(
            AffiliationRepositoryResult.Conflict("Ya existe una cuenta o solicitud asociada.")
        )
        val viewModel = RegisterAffiliationViewModel(repository)
        viewModel.updateForm { validForm() }

        viewModel.submit()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.accountExists)
        assertEquals("", viewModel.state.value.form.password)
        assertNull(viewModel.state.value.form.photo)
    }

    private class FakeAffiliationGateway(
        private val registerResult: AffiliationRepositoryResult<AffiliationRegistrationSuccess>,
        private val delayRegistration: Boolean = false
    ) : AffiliationRegistrationGateway {
        var registerCalls = 0

        override suspend fun catalogs(): AffiliationRepositoryResult<AffiliationCatalogs> {
            error("No se usa en estas pruebas.")
        }

        override suspend fun register(
            form: AffiliationRegistrationForm
        ): AffiliationRepositoryResult<AffiliationRegistrationSuccess> {
            registerCalls++
            if (delayRegistration) delay(50)
            return registerResult
        }
    }

    private fun validForm(photo: PreparedPhoto = tempPhoto()) = AffiliationRegistrationForm(
        fullName = "Ana Perez",
        ci = "123456",
        issuedIn = "LP",
        birthDate = "1990-01-01",
        maritalStatus = "SOLTERO",
        phone = "70000001",
        email = "ana@siafco.test",
        address = "Calle Ficticia",
        password = "Secret123",
        passwordConfirmation = "Secret123",
        sectorId = 1,
        planId = 1,
        regional = "LA PAZ",
        institution = "Institucion Demo",
        position = "Docente",
        photo = photo,
        termsAccepted = true,
        privacyAccepted = true
    )

    private fun tempPhoto(): PreparedPhoto {
        val file = File.createTempFile("siafco-affiliation-test", ".jpg").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4))
            deleteOnExit()
        }
        return PreparedPhoto(file = file, displayName = "photo.jpg")
    }

    private fun success() = AffiliationRegistrationSuccess(
        profile = SessionProfile(
            name = "Ana Perez",
            email = "ana@siafco.test",
            affiliateStatus = "pendiente_pago",
            affiliateStatusLabel = "Pendiente de pago",
            accessLevel = AccessLevel.Pending
        ),
        requestCode = "SOL-1"
    )
}
