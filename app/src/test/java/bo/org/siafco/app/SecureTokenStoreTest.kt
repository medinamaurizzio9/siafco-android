package bo.org.siafco.app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.Preferences
import bo.org.siafco.app.core.crypto.TokenCipher
import bo.org.siafco.app.core.data.SecureTokenStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

@OptIn(ExperimentalCoroutinesApi::class)
class SecureTokenStoreTest {
    @Test
    fun storesEncryptedTokenAndRecoversIt() = runTest {
        val dataStore = MemoryPreferencesDataStore()
        val store = SecureTokenStore(FakeCipher(), dataStore)

        store.saveToken("plain-token")

        assertEquals("plain-token", store.getToken())
        val raw = dataStore.data.first().asMap().values.joinToString()
        assertFalse(raw.contains("plain-token"))
    }

    @Test
    fun clearsToken() = runTest {
        val store = SecureTokenStore(FakeCipher(), MemoryPreferencesDataStore())

        store.saveToken("token")
        store.clearToken()

        assertNull(store.getToken())
    }

    @Test
    fun corruptedEncryptedDataIsHandledSafely() = runTest {
        val dataStore = MemoryPreferencesDataStore()
        val store = SecureTokenStore(object : TokenCipher {
            override fun encrypt(plainText: String) = "bad"
            override fun decrypt(cipherText: String): String = error("corrupt")
        }, dataStore)

        store.saveToken("token")

        assertNull(store.getToken())
        assertEquals(emptyMap<Preferences.Key<*>, Any>(), dataStore.data.first().asMap())
    }

    private class FakeCipher : TokenCipher {
        override fun encrypt(plainText: String): String {
            return Base64.getEncoder().encodeToString("cipher:$plainText".toByteArray())
        }

        override fun decrypt(cipherText: String): String {
            return String(Base64.getDecoder().decode(cipherText)).removePrefix("cipher:")
        }
    }

    private class MemoryPreferencesDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        override val data = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }
}
