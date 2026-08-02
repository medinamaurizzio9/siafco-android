package bo.org.siafco.app.data.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import bo.org.siafco.app.domain.StoreCartLine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.storeCartDataStore by preferencesDataStore(name = "siafco_store_cart")

interface StoreCartStore {
    val lines: Flow<List<StoreCartLine>>
    suspend fun add(line: StoreCartLine)
    suspend fun updateQuantity(productPublicCode: String, variantPublicCode: String?, quantity: Int)
    suspend fun remove(productPublicCode: String, variantPublicCode: String?)
    suspend fun clear()
}

class PreferencesStoreCartStore(context: Context) : StoreCartStore {
    private val dataStore = context.storeCartDataStore
    private val json = Json { ignoreUnknownKeys = true }

    override val lines: Flow<List<StoreCartLine>> = dataStore.data.map { preferences ->
        preferences[cartKey]?.let { raw ->
            runCatching { json.decodeFromString<List<CartLineEntity>>(raw).map { it.toDomain() } }
                .getOrDefault(emptyList())
        }.orEmpty()
    }

    override suspend fun add(line: StoreCartLine) {
        dataStore.edit { preferences ->
            val current = preferences.readLines(json)
            preferences[cartKey] = json.encodeToString(StoreCartLogic.add(current, line).map { it.toEntity() })
        }
    }

    override suspend fun updateQuantity(productPublicCode: String, variantPublicCode: String?, quantity: Int) {
        dataStore.edit { preferences ->
            val current = preferences.readLines(json)
            preferences[cartKey] = json.encodeToString(
                StoreCartLogic.updateQuantity(current, productPublicCode, variantPublicCode, quantity).map { it.toEntity() }
            )
        }
    }

    override suspend fun remove(productPublicCode: String, variantPublicCode: String?) {
        dataStore.edit { preferences ->
            val current = preferences.readLines(json)
            preferences[cartKey] = json.encodeToString(
                current.filterNot { it.productPublicCode == productPublicCode && it.variantPublicCode == variantPublicCode }
                    .map { it.toEntity() }
            )
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(cartKey) }
    }

    private fun androidx.datastore.preferences.core.Preferences.readLines(json: Json): List<StoreCartLine> {
        return this[cartKey]?.let { raw ->
            runCatching { json.decodeFromString<List<CartLineEntity>>(raw).map { it.toDomain() } }
                .getOrDefault(emptyList())
        }.orEmpty()
    }

    private companion object {
        val cartKey = stringPreferencesKey("lines")
    }
}

object StoreCartLogic {
    private const val MAX_LINES = 30
    private const val MAX_QUANTITY = 99

    fun add(current: List<StoreCartLine>, line: StoreCartLine): List<StoreCartLine> {
        if (line.productPublicCode.isBlank()) return current
        val normalized = line.copy(quantity = line.quantity.coerceIn(1, MAX_QUANTITY))
        val merged = current.map {
            if (it.productPublicCode == normalized.productPublicCode && it.variantPublicCode == normalized.variantPublicCode) {
                it.copy(quantity = (it.quantity + normalized.quantity).coerceAtMost(MAX_QUANTITY))
            } else {
                it
            }
        }
        return if (merged.any { it.key == normalized.key }) merged else (current + normalized).take(MAX_LINES)
    }

    fun updateQuantity(
        current: List<StoreCartLine>,
        productPublicCode: String,
        variantPublicCode: String?,
        quantity: Int
    ): List<StoreCartLine> {
        if (quantity <= 0) return current.filterNot {
            it.productPublicCode == productPublicCode && it.variantPublicCode == variantPublicCode
        }
        return current.map {
            if (it.productPublicCode == productPublicCode && it.variantPublicCode == variantPublicCode) {
                it.copy(quantity = quantity.coerceAtMost(MAX_QUANTITY))
            } else {
                it
            }
        }
    }
}

@Serializable
private data class CartLineEntity(
    val productPublicCode: String,
    val variantPublicCode: String? = null,
    val quantity: Int
) {
    fun toDomain(): StoreCartLine = StoreCartLine(productPublicCode, variantPublicCode, quantity)
}

private fun StoreCartLine.toEntity(): CartLineEntity = CartLineEntity(productPublicCode, variantPublicCode, quantity)
