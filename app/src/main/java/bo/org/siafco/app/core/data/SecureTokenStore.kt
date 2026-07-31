package bo.org.siafco.app.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import bo.org.siafco.app.core.crypto.TokenCipher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.siafcoDataStore by preferencesDataStore(name = "siafco_secure")

class SecureTokenStore(
    private val cipher: TokenCipher,
    private val dataStore: DataStore<Preferences>
) : TokenStore {
    constructor(context: Context, cipher: TokenCipher) : this(cipher, context.siafcoDataStore)

    override val token: Flow<String?> = dataStore.data.map { preferences ->
        preferences[ENCRYPTED_TOKEN]?.let { decryptOrNull(it) }
    }

    override suspend fun getToken(): String? {
        val encrypted = dataStore.data.first()[ENCRYPTED_TOKEN] ?: return null
        return decryptOrNull(encrypted).also { token ->
            if (token == null) clearToken()
        }
    }

    override suspend fun saveToken(token: String) {
        dataStore.edit { preferences ->
            preferences[ENCRYPTED_TOKEN] = cipher.encrypt(token)
        }
    }

    override suspend fun clearToken() {
        dataStore.edit { preferences ->
            preferences.remove(ENCRYPTED_TOKEN)
        }
    }

    private fun decryptOrNull(value: String): String? = runCatching {
        cipher.decrypt(value)
    }.getOrNull()

    private companion object {
        val ENCRYPTED_TOKEN = stringPreferencesKey("encrypted_access_token")
    }
}
