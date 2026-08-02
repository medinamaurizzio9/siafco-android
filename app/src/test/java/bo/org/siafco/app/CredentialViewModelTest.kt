package bo.org.siafco.app

import bo.org.siafco.app.data.repository.CredentialGateway
import bo.org.siafco.app.data.repository.CredentialResult
import bo.org.siafco.app.data.repository.MobileCredential
import bo.org.siafco.app.feature.UiMessage
import bo.org.siafco.app.feature.credential.CredentialViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CredentialViewModelTest {
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
    fun successKeepsNativeCredentialAndReloadCallsRepositoryAgain() = runTest(dispatcher) {
        val gateway = FakeCredentialGateway(
            mutableListOf(
                CredentialResult.Success(credential("REG-1")),
                CredentialResult.Success(credential("REG-2"))
            )
        )
        val viewModel = CredentialViewModel(gateway)

        viewModel.load()
        advanceUntilIdle()
        viewModel.load(force = true)
        advanceUntilIdle()

        assertEquals(2, gateway.calls)
        assertEquals("REG-2", viewModel.state.value.credential?.registrationNumber)
    }

    @Test
    fun authorizationErrorsBecomeControlledMessages() = runTest(dispatcher) {
        val viewModel = CredentialViewModel(FakeCredentialGateway(mutableListOf(CredentialResult.Forbidden)))

        viewModel.load()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.messageText.orEmpty().contains("estado"))
        assertFalse(viewModel.state.value.loggedOut)
    }

    @Test
    fun unauthorizedMarksSessionAsLoggedOut() = runTest(dispatcher) {
        val viewModel = CredentialViewModel(FakeCredentialGateway(mutableListOf(CredentialResult.Unauthorized)))

        viewModel.load()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.loggedOut)
        assertEquals(UiMessage.Unauthorized, viewModel.state.value.message)
    }

    @Test
    fun notFoundShowsCredentialUnavailableText() = runTest(dispatcher) {
        val viewModel = CredentialViewModel(FakeCredentialGateway(mutableListOf(CredentialResult.NotFound)))

        viewModel.load()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.messageText.orEmpty().contains("credencial"))
    }

    @Test
    fun networkErrorDoesNotClearExistingCredentialOrSession() = runTest(dispatcher) {
        val gateway = FakeCredentialGateway(
            mutableListOf(
                CredentialResult.Success(credential("REG-1")),
                CredentialResult.NetworkError
            )
        )
        val viewModel = CredentialViewModel(gateway)

        viewModel.load()
        advanceUntilIdle()
        viewModel.load(force = true)
        advanceUntilIdle()

        assertEquals("REG-1", viewModel.state.value.credential?.registrationNumber)
        assertEquals(UiMessage.Network, viewModel.state.value.message)
        assertFalse(viewModel.state.value.loggedOut)
    }

    @Test
    fun invalidPayloadIsControlled() = runTest(dispatcher) {
        val viewModel = CredentialViewModel(FakeCredentialGateway(mutableListOf(CredentialResult.InvalidPayload)))

        viewModel.load()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.messageText.orEmpty().contains("valida"))
    }

    private class FakeCredentialGateway(
        private val results: MutableList<CredentialResult>
    ) : CredentialGateway {
        var calls = 0
        override suspend fun loadCredential(): CredentialResult {
            calls++
            return results.removeFirst()
        }
    }

    private fun credential(registration: String) = MobileCredential(
        institutionName = "SIAFCO",
        affiliateName = "AFILIADO MOVIL",
        registrationNumber = registration,
        sector = "MAGISTERIO",
        regional = "LA PAZ",
        status = "activo",
        statusLabel = "ACTIVO",
        issuedAt = "02/08/2026",
        photoUrl = "https://siafco.test/storage/affiliates/photos/a.jpg?v=123",
        verificationUrl = "https://siafco.test/verificar/a",
        qrPngBytes = byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)
    )
}
