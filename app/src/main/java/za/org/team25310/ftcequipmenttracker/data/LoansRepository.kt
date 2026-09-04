package za.org.team25310.ftcequipmenttracker.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import za.org.team25310.ftcequipmenttracker.data.model.LoanEvent

object LoansRepository {

    private val db = Firebase.firestore
    private val collection = db.collection("loans")

    fun observeLoans(teamId: String): Flow<List<LoanEvent>> = callbackFlow {
        val registration = collection
            .whereArrayContains("teamIds", teamId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val loans = snapshot?.documents.orEmpty()
                    .mapNotNull { doc -> LoanEvent.fromMap(doc.id, doc.data ?: emptyMap()) }
                    .sortedByDescending { it.createdAt }
                trySend(loans)
            }
        awaitClose { registration.remove() }
    }

    suspend fun saveLoan(event: LoanEvent): String {
        val data = event.toMap()
        val doc = if (event.id.isBlank()) {
            collection.document()
        } else {
            collection.document(event.id)
        }
        doc.set(data).await()
        return doc.id
    }

    suspend fun deleteLoan(id: String) {
        collection.document(id).delete().await()
    }

    suspend fun compressPhotoToBase64(file: File, maxDim: Int = 640): String =
        withContext(Dispatchers.IO) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@withContext ""
            val scale = minOf(1f, maxDim.toFloat() / maxOf(bitmap.width, bitmap.height))
            val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
            val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 60, out)
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        }
}
