package bo.org.siafco.app.data.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import bo.org.siafco.app.domain.sha256
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID

private val Context.storePendingOrderDataStore by preferencesDataStore(name = "siafco_store_pending_orders")

interface StorePendingOrderStore {
    suspend fun keyFor(payloadSignature: String): String
    suspend fun clear()
}

class PreferencesStorePendingOrderStore(context: Context) : StorePendingOrderStore {
    private val dataStore = context.storePendingOrderDataStore
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun keyFor(payloadSignature: String): String {
        val current = read()
        if (current?.payloadSignature == payloadSignature) return current.idempotencyKey
        val draft = PendingOrderEntity(
            payloadSignature = payloadSignature,
            idempotencyKey = UUID.randomUUID().toString(),
            createdAt = Instant.now().toString()
        )
        dataStore.edit { it[pendingOrderKey] = json.encodeToString(draft) }
        return draft.idempotencyKey
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(pendingOrderKey) }
    }

    private suspend fun read(): PendingOrderEntity? {
        return dataStore.data.first()[pendingOrderKey]?.let { raw ->
            runCatching { json.decodeFromString<PendingOrderEntity>(raw) }.getOrNull()
        }
    }

    private companion object {
        val pendingOrderKey = stringPreferencesKey("pending_order")
    }
}

fun storePayloadSignature(value: String): String = sha256(value)

@Serializable
private data class PendingOrderEntity(
    val payloadSignature: String,
    val idempotencyKey: String,
    val createdAt: String
)
