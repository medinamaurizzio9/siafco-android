package bo.org.siafco.app

import bo.org.siafco.app.data.remote.LoginRequest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class LoginSerializationTest {
    @Test
    fun serializesLoginUsingLaravelFieldNames() {
        val json = Json.encodeToString(
            LoginRequest.serializer(),
            LoginRequest(email = "ana@siafco.test", password = "secret", deviceName = "Pixel 8")
        )

        assertEquals(
            """{"email":"ana@siafco.test","password":"secret","device_name":"Pixel 8"}""",
            json
        )
    }
}
