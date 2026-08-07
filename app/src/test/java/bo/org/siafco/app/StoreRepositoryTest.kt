package bo.org.siafco.app

import bo.org.siafco.app.data.remote.SiafcoApi
import bo.org.siafco.app.data.repository.StoreCatalogFilters
import bo.org.siafco.app.data.repository.StoreOrderFilters
import bo.org.siafco.app.data.repository.StoreRepository
import bo.org.siafco.app.data.repository.StoreResult
import bo.org.siafco.app.domain.StoreCartLine
import bo.org.siafco.app.domain.StoreQuoteRequestData
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class StoreRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: StoreRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        repository = StoreRepository(api(server))
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun catalogDeserializesLaravelContractAndKeepsMoneyAsString() = runTest {
        server.enqueue(jsonResponse(CATALOG_JSON))

        val result = repository.catalog(StoreCatalogFilters(search = "polera", perPage = 20))

        assertTrue(result is StoreResult.Success)
        val catalog = (result as StoreResult.Success).value
        assertEquals("BOB", catalog.settings.currency)
        assertEquals("indumentaria", catalog.categories.first().slug)
        assertEquals("PROD-1", catalog.products.first().publicCode)
        assertEquals("120.00", catalog.products.first().regularPrice)
        assertEquals("99.90", catalog.products.first().effectivePrice)
        val product = catalog.products.first()
        assertEquals("http://10.0.2.2:8000/storage/store/products/polera.jpg?v=1", product.primaryImageUrl)
        assertEquals("disponible", product.availabilityStatus)
        assertTrue(product.canOrder)
        assertTrue(product.isAvailable)

        val request = server.takeRequest()
        assertEquals("/api/mobile/v1/store", request.url.encodedPath)
        assertEquals("polera", request.url.queryParameter("search"))
        assertEquals("20", request.url.queryParameter("per_page"))
    }

    @Test
    fun quoteSendsOnlyIdentifiersQuantitiesAndDeliveryData() = runTest {
        server.enqueue(jsonResponse(QUOTE_JSON))

        val result = repository.quote(
            StoreQuoteRequestData(
                lines = listOf(StoreCartLine("PROD-1", "VAR-1", 2)),
                deliveryMethod = "shipping",
                department = "La Paz",
                city = "La Paz",
                zone = "Centro",
                deliveryAddress = "Direccion ficticia",
                couponCode = "CUPON"
            )
        )

        assertTrue(result is StoreResult.Success)
        val quote = (result as StoreResult.Success).value
        assertEquals("209.80", quote.total)
        assertEquals("Polera SIAFCO", quote.items.single().productName)
        assertEquals("M", quote.items.single().variantName)
        assertEquals(2, quote.items.single().quantity)
        assertEquals("99.90", quote.items.single().unitPrice)
        assertEquals("199.80", quote.items.single().lineTotal)
        val body = server.takeRequest().body!!.utf8()
        assertTrue(body.contains("product_public_code"))
        assertTrue(body.contains("variant_public_code"))
        assertTrue(!body.contains("subtotal"))
        assertTrue(!body.contains("total"))
    }

    @Test
    fun createOrderUsesIdempotencyHeaderAndMapsOrder() = runTest {
        server.enqueue(jsonResponse(ORDER_JSON))

        val result = repository.createOrder(
            idempotencyKey = "idem-123",
            request = StoreQuoteRequestData(lines = listOf(StoreCartLine("PROD-1", null, 1)), deliveryMethod = "pickup")
        )

        assertTrue(result is StoreResult.Success)
        assertEquals("PED-1", (result as StoreResult.Success).value.code)
        assertEquals("idem-123", server.takeRequest().headers["Idempotency-Key"])
    }

    @Test
    fun ordersReceiptAndWhatsappContractsAreMapped() = runTest {
        server.enqueue(jsonResponse(ORDERS_JSON))
        server.enqueue(jsonResponse(ORDER_JSON))
        server.enqueue(jsonResponse(WHATSAPP_JSON))

        assertTrue(repository.orders() is StoreResult.Success)
        val ordersRequest = server.takeRequest()
        assertEquals("/api/mobile/v1/store/orders", ordersRequest.url.encodedPath)
        assertNull(ordersRequest.url.queryParameter("attention_only"))

        val detail = repository.order("PED-1")
        assertTrue(detail is StoreResult.Success)
        val item = (detail as StoreResult.Success).value.items.single()
        assertEquals("http://10.0.2.2:8000/storage/store/products/polera.jpg?v=1", item.primaryImageUrl)
        val whatsapp = repository.whatsapp("PED-1")

        assertTrue(whatsapp is StoreResult.Success)
        assertEquals("https://wa.me/59170000000?text=Pedido", (whatsapp as StoreResult.Success).value.url)
    }

    @Test
    fun attentionOnlyOrdersSendDedicatedQueryWithoutStatusFilter() = runTest {
        server.enqueue(jsonResponse(ORDERS_JSON))

        val result = repository.orders(StoreOrderFilters(attentionOnly = true, perPage = 3))

        assertTrue(result is StoreResult.Success)
        val request = server.takeRequest()
        assertEquals("/api/mobile/v1/store/orders", request.url.encodedPath)
        assertEquals("true", request.url.queryParameter("attention_only"))
        assertEquals("3", request.url.queryParameter("per_page"))
        assertNull(request.url.queryParameter("status"))
    }

    @Test
    fun deliveryDestinationsDeserializeLaravelContractWithoutPrices() = runTest {
        server.enqueue(jsonResponse(DELIVERY_DESTINATIONS_JSON))

        val result = repository.deliveryDestinations()

        assertTrue(result is StoreResult.Success)
        val destinations = (result as StoreResult.Success).value
        assertEquals(1, destinations.size)
        assertEquals("LA PAZ", destinations.single().department)
        assertEquals(listOf("EL ALTO", "LA PAZ"), destinations.single().cities.map { it.city })
        assertEquals("SOPOCACHI", destinations.single().cities[1].zones.single().zone)
        val request = server.takeRequest()
        assertEquals("/api/mobile/v1/store/delivery-destinations", request.url.encodedPath)
    }

    @Test
    fun httpErrorsAreControlledAndNetworkKeepsSessionOutsideRepository() = runTest {
        server.enqueue(jsonResponse("""{"success":false,"message":"No autorizado","errors":{}}""", 401))
        server.enqueue(jsonResponse("""{"success":false,"message":"No activo","errors":{}}""", 403))
        server.enqueue(jsonResponse("""{"success":false,"message":"No existe","errors":{}}""", 404))
        server.enqueue(jsonResponse("""{"success":false,"message":"Conflicto","errors":{}}""", 409))
        server.enqueue(jsonResponse("""{"success":false,"message":"Error","errors":{"items":["No valido"]}}""", 422))
        server.enqueue(jsonResponse("""{"success":false,"message":"Limite","errors":{}}""", 429))

        assertTrue(repository.catalog() is StoreResult.Unauthorized)
        assertTrue(repository.catalog() is StoreResult.Forbidden)
        assertTrue(repository.catalog() is StoreResult.NotFound)
        assertTrue(repository.catalog() is StoreResult.Conflict)
        assertTrue(repository.catalog() is StoreResult.ValidationError)
        assertTrue(repository.catalog() is StoreResult.RateLimited)

        server.close()
        assertTrue(repository.catalog() is StoreResult.NetworkError)
    }

    @Test
    fun unauthorizedStoreResponseRunsSessionCleanup() = runTest {
        var cleaned = false
        repository = StoreRepository(api(server), onUnauthorized = { cleaned = true })
        server.enqueue(jsonResponse("""{"success":false,"message":"No autenticado","errors":{}}""", 401))

        val result = repository.catalog()

        assertTrue(result is StoreResult.Unauthorized)
        assertTrue(cleaned)
    }

    private fun api(server: MockWebServer): SiafcoApi {
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
        return Retrofit.Builder()
            .baseUrl(server.url("/api/mobile/v1/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SiafcoApi::class.java)
    }

    private fun jsonResponse(body: String, code: Int = 200): MockResponse =
        MockResponse.Builder()
            .code(code)
            .addHeader("Content-Type", "application/json")
            .body(body)
            .build()

    private companion object {
        private const val PRODUCT_JSON = """
            {
              "public_code":"PROD-1",
              "slug":"polera",
              "sku":"SKU-1",
              "name":"Polera SIAFCO",
              "short_description":"Producto ficticio",
              "description":"Detalle ficticio",
              "regular_price":"120.00",
              "affiliate_price":"100.00",
              "effective_price":"99.90",
              "promo_price":"99.90",
              "currency":"BOB",
              "availability_status":"disponible",
              "delivery_modes":["pickup","shipping"],
              "featured":true,
              "max_quantity_per_order":3,
              "primary_image_url":"http://127.0.0.1:8000/storage/store/products/polera.jpg?v=1",
              "category":{"slug":"indumentaria","name":"Indumentaria"},
              "capabilities":{"can_order":true},
              "images":[{"url":"http://127.0.0.1:8000/storage/store/products/polera.jpg?v=1","alt":"Polera","is_primary":true}],
              "variants":[{"public_code":"VAR-1","name":"M","type":"talla","price_delta":"0.00","effective_price":"99.90"}]
            }
        """

        private const val CATALOG_JSON = """
            {"success":true,"message":"OK","data":{
              "settings":{"currency":"BOB","pickup_enabled":true,"shipping_enabled":true,"pickup_instructions":"Recojo","shipping_instructions":"Envio","payment":{"qr_url":null,"bank":"Banco","holder":"SIAFCO","account":"000","instructions":"Pagar"},"whatsapp_enabled":true},
              "featured":[$PRODUCT_JSON],
              "categories":[{"slug":"indumentaria","name":"Indumentaria"}],
              "products":[$PRODUCT_JSON],
              "pagination":{"current_page":1,"per_page":20,"last_page":1,"total":1}
            }}
        """

        private const val QUOTE_JSON = """
            {"success":true,"message":"OK","data":{"quote":{
              "items":[{"product":{"public_code":"PROD-1","name":"Polera SIAFCO"},"variant":{"public_code":"VAR-1","name":"M","type":"talla"},"quantity":2,"unit_price":"99.90","line_total":"199.80","price_reason":"affiliate"}],
              "subtotal":"199.80","discount_total":"0.00","shipping_total":"10.00","total":"209.80","currency":"BOB",
              "coupon":{"applied":false,"hint":"Cupón no aplicado"},
              "shipping":{"method":"shipping","amount":"10.00","currency":"BOB","scope":"city","department":"La Paz","city":"La Paz","zone":"Centro"},
              "expires_at":"2026-08-02T12:00:00Z"
            }}}
        """

        private const val ORDER_JSON = """
            {"success":true,"message":"OK","data":{"order":{
              "code":"PED-1","date":"02/08/2026","status":"pending_payment","status_label":"Pendiente de pago","created_at":"2026-08-02T12:00:00Z","updated_at":"2026-08-02T12:00:00Z",
              "total":"209.80","currency":"BOB","delivery_method":"pickup","item_summary":"1 producto",
              "capabilities":{"can_upload_receipt":true,"can_open_whatsapp":true,"can_cancel":false,"can_view_receipt":false},
              "delivery":{"method":"pickup","department":null,"city":null,"zone":null,"address":null},
              "items":[{"sku":"SKU-1","name":"Polera SIAFCO","variant":"M","unit_price":"99.90","quantity":1,"discount_total":"0.00","line_total":"99.90","primary_image_url":"http://127.0.0.1:8000/storage/store/products/polera.jpg?v=1"}],
              "subtotal":"99.90","discount_total":"0.00","shipping_total":"0.00","payment":{"status":"pending","message":"Pendiente"},
              "receipts":[],"status_history":[{"from_status":null,"to_status":"pending_payment","changed_at":"2026-08-02T12:00:00Z"}]
            }}}
        """

        private const val ORDERS_JSON = """
            {"success":true,"message":"OK","data":{"orders":[{
              "code":"PED-1","date":"02/08/2026","status":"pending_payment","status_label":"Pendiente de pago","created_at":"2026-08-02T12:00:00Z","updated_at":"2026-08-02T12:00:00Z",
              "total":"209.80","currency":"BOB","delivery_method":"pickup","item_summary":"1 producto",
              "capabilities":{"can_upload_receipt":true,"can_open_whatsapp":true,"can_cancel":false,"can_view_receipt":false}
            }],"pagination":{"current_page":1,"per_page":15,"last_page":1,"total":1}}}
        """

        private const val WHATSAPP_JSON = """
            {"success":true,"message":"OK","data":{"whatsapp":{"url":"https://wa.me/59170000000?text=Pedido","opened_at":"2026-08-02T12:00:00Z","message_preview":"Pedido"}}}
        """

        private const val DELIVERY_DESTINATIONS_JSON = """
            {"success":true,"message":"OK","data":[
              {"department":"LA PAZ","cities":[
                {"city":"EL ALTO","zones":[]},
                {"city":"LA PAZ","zones":[{"zone":"SOPOCACHI"}]}
              ]}
            ]}
        """
    }
}
