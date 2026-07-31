package bo.org.siafco.app.core.data

import kotlinx.coroutines.flow.Flow

interface TokenStore {
    val token: Flow<String?>
    suspend fun getToken(): String?
    suspend fun saveToken(token: String)
    suspend fun clearToken()
}
