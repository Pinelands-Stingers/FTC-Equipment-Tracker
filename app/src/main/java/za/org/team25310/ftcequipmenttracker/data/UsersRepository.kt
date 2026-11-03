package za.org.team25310.ftcequipmenttracker.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import za.org.team25310.ftcequipmenttracker.data.model.AppUser
import za.org.team25310.ftcequipmenttracker.data.model.Role
import za.org.team25310.ftcequipmenttracker.data.model.TeamInvite

object UsersRepository {

    private val db = Firebase.firestore
    private val collection = db.collection("users")

    suspend fun getUser(uid: String): AppUser? {
        val snapshot = collection.document(uid).get().await()
        return snapshot.data?.let { AppUser.fromMap(uid, it) }
    }

    suspend fun createUserForNewTeam(
        teamId: String,
        role: Role,
        displayName: String,
    ): TeamInvite {
        val currentUser = FirebaseAuth.getInstance().currentUser
            ?: error("Not signed in")
        currentUser.getIdToken(true).await()

        val ensured = TeamInvitesRepository.ensureInvite(
            teamId = teamId,
            createdBy = currentUser.uid,
            createdByEmail = currentUser.email.orEmpty(),
        )
        if (!ensured.created && ensured.invite.createdBy != currentUser.uid) {
            error("This team already has an account. Ask a coach for the invite code.")
        }

        collection.document(currentUser.uid)
            .set(userData(teamId, role, displayName, ensured.invite.codeFor(role)))
            .await()
        return ensured.invite
    }

    suspend fun joinTeam(
        teamId: String,
        role: Role,
        inviteCode: String,
        displayName: String,
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
            ?: error("Not signed in")
        currentUser.getIdToken(true).await()

        runCatching {
            collection.document(currentUser.uid)
                .set(userData(teamId, role, displayName, TeamInvite.normalizeCode(inviteCode)))
                .await()
        }.onFailure { failure ->
            if (failure is FirebaseFirestoreException &&
                failure.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
            ) {
                error("That invite code is not valid for this team and role.")
            }
        }.getOrThrow()
    }

    private fun userData(
        teamId: String,
        role: Role,
        displayName: String,
        inviteCode: String,
    ): Map<String, Any?> = mapOf(
        "teamId" to teamId,
        "role" to role.name,
        "displayName" to displayName,
        "createdAt" to System.currentTimeMillis(),
        "inviteCode" to inviteCode,
    )
}
