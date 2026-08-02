package bo.org.siafco.app

import bo.org.siafco.app.feature.store.StoreWhatsappPolicy
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class StoreWhatsappPolicyTest {
    @Test
    fun acceptsOnlyExpectedWhatsappHttpsUrls() {
        assertNotNull(StoreWhatsappPolicy.validate("https://wa.me/59170000000?text=Pedido"))
        assertNotNull(StoreWhatsappPolicy.validate("https://api.whatsapp.com/send?phone=59170000000&text=Pedido"))
        assertNull(StoreWhatsappPolicy.validate("http://wa.me/59170000000"))
        assertNull(StoreWhatsappPolicy.validate("https://fake-wa.me/59170000000"))
        assertNull(StoreWhatsappPolicy.validate("https://user:pass@wa.me/59170000000"))
        assertNull(StoreWhatsappPolicy.validate("javascript:alert(1)"))
        assertNull(StoreWhatsappPolicy.validate("file:///tmp/a"))
        assertNull(StoreWhatsappPolicy.validate("content://provider/a"))
        assertNull(StoreWhatsappPolicy.validate("data:text/plain,a"))
        assertNull(StoreWhatsappPolicy.validate("intent://send#Intent;scheme=https;end"))
        assertNull(StoreWhatsappPolicy.validate("https://wa.me/${"1".repeat(2_100)}"))
    }
}
