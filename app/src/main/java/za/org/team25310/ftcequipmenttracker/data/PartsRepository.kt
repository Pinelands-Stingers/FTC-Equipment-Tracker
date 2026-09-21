package za.org.team25310.ftcequipmenttracker.data

import android.content.Context
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import za.org.team25310.ftcequipmenttracker.data.model.Part
import za.org.team25310.ftcequipmenttracker.data.model.Vendor

object PartsRepository {

    private const val PARTS_COLLECTION = "parts"
    private const val META_COLLECTION = "catalogMeta"
    private const val META_DOCUMENT = "current"
    private const val CACHE_FILE = "parts_cache.json"
    private const val FALLBACK_ASSET = "parts.json"
    private const val PAGE_SIZE = 500L

    private val db = Firebase.firestore
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class PartsCache(
        val version: Int = 0,
        val parts: List<Part> = emptyList(),
    )

    suspend fun loadParts(context: Context): List<Part> = withContext(Dispatchers.IO) {
        val cache = readCache(context)
        val remoteVersion = runCatching { fetchVersion() }.getOrNull()

        if (remoteVersion != null && remoteVersion != cache?.version) {
            val fresh = runCatching { fetchAllParts() }.getOrNull()
            if (!fresh.isNullOrEmpty()) {
                writeCache(context, PartsCache(remoteVersion, fresh))
                return@withContext fresh
            }
        }

        cache?.parts ?: loadFallback(context)
    }

    private suspend fun fetchVersion(): Int? =
        db.collection(META_COLLECTION).document(META_DOCUMENT).get().await()
            .getLong("version")
            ?.toInt()

    private suspend fun fetchAllParts(): List<Part> {
        val all = mutableListOf<Part>()
        var last: DocumentSnapshot? = null
        while (true) {
            var query = db.collection(PARTS_COLLECTION).limit(PAGE_SIZE)
            if (last != null) query = query.startAfter(last)
            val snapshot = query.get().await()
            val documents = snapshot.documents
            documents.mapNotNullTo(all) { mapPart(it.id, it.data ?: emptyMap()) }
            if (documents.size < PAGE_SIZE) break
            last = documents.last()
        }
        return all
    }

    private fun mapPart(id: String, data: Map<String, Any?>): Part? {
        val name = data["name"] as? String ?: return null
        val tags = (data["tags"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        return Part(
            id = id,
            sku = data["sku"] as? String ?: "",
            name = name,
            vendor = Vendor.from(data["vendor"] as? String),
            category = data["category"] as? String ?: tags.firstOrNull().orEmpty(),
            ftcLegal = data["ftcLegal"] as? Boolean ?: true,
            tags = tags,
            url = data["url"] as? String ?: "",
        )
    }

    private fun readCache(context: Context): PartsCache? = runCatching {
        val file = File(context.filesDir, CACHE_FILE)
        if (!file.exists()) return@runCatching null
        json.decodeFromString<PartsCache>(file.readText())
    }.getOrNull()

    private fun writeCache(context: Context, cache: PartsCache) {
        runCatching {
            File(context.filesDir, CACHE_FILE).writeText(json.encodeToString(cache))
        }
    }

    private fun loadFallback(context: Context): List<Part> = runCatching {
        context.assets.open(FALLBACK_ASSET).bufferedReader().use { it.readText() }
            .let { json.decodeFromString<List<Part>>(it) }
    }.getOrDefault(emptyList())
}
