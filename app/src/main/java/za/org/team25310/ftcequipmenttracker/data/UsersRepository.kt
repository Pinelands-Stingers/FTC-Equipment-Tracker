package za.org.team25310.ftcequipmenttracker.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import za.org.team25310.ftcequipmenttracker.data.model.AppUser
import za.org.team25310.ftcequipmenttracker.data.model.Role

object UsersRepository {

    private val db = Firebase.firestore
    private val collection = db.collection("users")

    suspend fun getUser(uid: String): AppUser? {
        val snapshot = collection.document(uid).get().await()
        return snapshot.data?.let { AppUser.fromMap(uid, it) }
    }

    suspend fun createUser(
        teamId: String,
        role: Role,
        inviteCode: String,
        displayName: String,
    ): AppUser {
        val currentUser = FirebaseAuth.getInstance().currentUser
            ?: error("Not signed in")
        currentUser.getIdToken(true).await()
        val createdAt = System.currentTimeMillis()
        val data = mapOf(
            "teamId" to teamId,
            "role" to role.name,
            "displayName" to displayName,
            "createdAt" to createdAt,
            "inviteCode" to inviteCode,
        )
        collection.document(currentUser.uid).set(data).await()
        return AppUser(currentUser.uid, teamId, role, displayName, createdAt)
    }
}
