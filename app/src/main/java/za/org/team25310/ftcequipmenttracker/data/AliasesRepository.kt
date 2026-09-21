package za.org.team25310.ftcequipmenttracker.data

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

object AliasesRepository {

    private val db = Firebase.firestore
    private val collection = db.collection("partAliases")

    suspend fun loadAliases(teamId: String): Map<String, List<String>> {
        val snapshot = collection.whereEqualTo("teamId", teamId).get().await()
        return snapshot.documents.mapNotNull { doc ->
            val partId = doc.getString("partId")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val aliases = (doc.get("aliases") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            partId to aliases
        }.toMap()
    }

    suspend fun saveAliases(teamId: String, partId: String, aliases: List<String>) {
        val docId = "${teamId}_${partId.replace('/', '_')}"
        val document = collection.document(docId)
        if (aliases.isEmpty()) {
            document.delete().await()
        } else {
            document.set(
                mapOf(
                    "teamId" to teamId,
                    "partId" to partId,
                    "aliases" to aliases,
                    "updatedAt" to System.currentTimeMillis(),
                ),
            ).await()
        }
    }
}
