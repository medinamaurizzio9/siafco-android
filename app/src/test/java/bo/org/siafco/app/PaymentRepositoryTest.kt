package bo.org.siafco.app

import bo.org.siafco.app.data.remote.SiafcoApi
import bo.org.siafco.app.data.repository.PaymentRepository
import bo.org.siafco.app.data.repository.PaymentRepositoryResult
import bo.org.siafco.app.domain.NormalizedPaymentPayload
import bo.org.siafco.app.domain.PreparedReceipt
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File

class PaymentRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: PaymentRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        repository = PaymentRepository(api(server))
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun successSendsIdempotencyKeyAndMultipartFields() = runTest {
        server.enqueue(jsonResponse(SUCCESS_JSON, 201))

        val result = repository.submit("550e8400-e29b-41d4-a716-446655440000", payload(), receipt())

        assertTrue(result is PaymentRepositoryResult.Success)
        assertEquals("payment_submitted", (result as PaymentRepositoryResult.Success).request.status)
        assertEquals("pending", result.request.paymentStatus)
        assertEquals("http://10.0.2.2:8000/storage/institutional/payment/payment-qr.png?v=123", result.request.paymentQrUrl)
        assertEquals("70000000", result.request.supportPhone)
        val request = server.takeRequest()
        assertEquals("/api/mobile/v1/me/affiliation-request/payment", request.url.encodedPath)
        assertEquals("550e8400-e29b-41d4-a716-446655440000", request.headers["Idempotency-Key"])
        assertTrue(request.headers["Content-Type"].orEmpty().startsWith("multipart/form-data"))
        val body = request.body!!.utf8()
        assertTrue(body.contains("transaction_number"))
        assertTrue(body.contains("TRX-123"))
        assertTrue(body.contains("paid_amount"))
        assertTrue(body.contains("250.00"))
        assertTrue(body.contains("receipt-abc123.jpg"))
    }

    @Test
    fun validationErrorsAreMapped() = runTest {
        server.enqueue(jsonResponse("""{"success":false,"message":"Error","errors":{"paid_amount":["Monto invalido."]}}""", 422))

        val result = repository.submit("key", payload(), receipt())

        assertTrue(result is PaymentRepositoryResult.ValidationError)
        assertEquals("Monto invalido.", (result as PaymentRepositoryResult.ValidationError).errors["paid_amount"]?.first())
    }

    @Test
    fun conflictUnauthorizedRateLimitAndNetworkAreMapped() = runTest {
        server.enqueue(jsonResponse("""{"success":false,"message":"Clave repetida.","errors":[]}""", 409))
        server.enqueue(jsonResponse("""{"success":false,"message":"No autorizado.","errors":[]}""", 401))
        server.enqueue(jsonResponse("""{"success":false,"message":"Espera.","errors":[]}""", 429))

        assertTrue(repository.submit("key-1", payload(), receipt()) is PaymentRepositoryResult.Conflict)
        assertTrue(repository.submit("key-2", payload(), receipt()) is PaymentRepositoryResult.Unauthorized)
        assertTrue(repository.submit("key-3", payload(), receipt()) is PaymentRepositoryResult.RateLimited)

        server.close()
        assertTrue(repository.submit("key-4", payload(), receipt()) is PaymentRepositoryResult.NetworkError)
    }

    private fun api(server: MockWebServer): SiafcoApi {
        val json = Json { ignoreUnknownKeys = true }
        return Retrofit.Builder()
            .baseUrl(server.url("/api/mobile/v1/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SiafcoApi::class.java)
    }

    private fun jsonResponse(body: String, code: Int = 200): MockResponse {
        return MockResponse.Builder()
            .code(code)
            .addHeader("Content-Type", "application/json")
            .body(body)
            .build()
    }

    private fun payload() = NormalizedPaymentPayload(
        transactionNumber = "TRX-123",
        paymentDate = "2026-08-01",
        paidAmount = "250.00",
        payerName = "Ana Demo",
        bankName = "Banco",
        receiptSha256 = "abc123"
    )

    private fun receipt(): PreparedReceipt {
        val file = File.createTempFile("receipt", ".jpg").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4))
            deleteOnExit()
        }
        return PreparedReceipt(file, "receipt.jpg", "image/jpeg", file.length(), "abc123", true)
    }

    private companion object {
        private const val SUCCESS_JSON = """
            {
              "success": true,
              "message": "Pago enviado para revision.",
              "data": {
                "idempotent": false,
                "affiliation_request": {
                  "request_code": "SOL-1",
                  "status": "payment_submitted",
                  "status_label": "Pago en revision",
                  "status_description": "Pago registrado.",
                  "observations": null,
                  "amount_due": 250,
                  "currency": "BOB",
                  "plan": {"name":"AFILIACION INICIAL","type":"independiente","affiliation_fee":250,"credential_fee":0,"total_amount":250,"payment_instructions":null},
                  "payment": {
                    "status": "pending",
                    "status_label": "Pendiente",
                    "transaction_number": "TRX-123",
                    "payment_date": "2026-08-01",
                    "paid_amount": 250,
                    "submitted_at": "2026-08-01T12:00:00Z",
                    "rejection_reason": null,
                    "has_receipt": true
                  },
                  "payment_instructions": {
                    "bank": null,
                    "holder": null,
                    "account": null,
                    "instructions": null,
                    "qr_url": "http://127.0.0.1:8000/storage/institutional/payment/payment-qr.png?v=123",
                    "support_phone": "70000000"
                  },
                  "capabilities": {"can_submit_payment":true,"can_login":true,"can_view_credential":false}
                }
              }
            }
        """
    }
}
