package za.org.team25310.ftcequipmenttracker.data

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import za.org.team25310.ftcequipmenttracker.data.model.TeamInvite

data class EnsureInviteResult(
    val invite: TeamInvite,
    val created: Boolean,
)

data class TeamClaim(
    val claimedByEmail: String,
    val claimedAt: Long,
)

object TeamInvitesRepository {

    private val db = Firebase.firestore
    private val invites = db.collection("teamInvites")
    private val directory = db.collection("teamDirectory")

    fun document(teamId: String) = invites.document(teamId)

    suspend fun hasTeam(teamId: String): Boolean =
        directory.document(teamId).get().await().exists()

    suspend fun getTeamClaim(teamId: String): TeamClaim? {
        val snapshot = directory.document(teamId).get().await()
        if (!snapshot.exists()) return null
        return TeamClaim(
            claimedByEmail = snapshot.getString("createdByEmail").orEmpty(),
            claimedAt = snapshot.getLong("createdAt") ?: 0L,
        )
    }

    suspend fun getInvite(teamId: String): TeamInvite? {
        val snapshot = invites.document(teamId).get().await()
        return if (snapshot.exists()) TeamInvite.fromMap(snapshot.data.orEmpty()) else null
    }

    suspend fun ensureInvite(
        teamId: String,
        createdBy: String,
        createdByEmail: String,
    ): EnsureInviteResult {
        val inviteRef = invites.document(teamId)
        val directoryRef = directory.document(teamId)
        return db.runTransaction { transaction ->
            val directorySnapshot = transaction.get(directoryRef)
            when {
                !directorySnapshot.exists() -> {
                    val invite = TeamInvite.newInvite(createdBy)
                    transaction.set(inviteRef, invite.toMap())
                    transaction.set(directoryRef, directoryData(createdBy, createdByEmail))
                    EnsureInviteResult(invite, created = true)
                }

                (directorySnapshot.getString("createdBy") ?: "") != createdBy -> {
                    EnsureInviteResult(
                        TeamInvite(createdBy = directorySnapshot.getString("createdBy") ?: ""),
                        created = false,
                    )
                }

                else -> {
                    val snapshot = transaction.get(inviteRef)
                    EnsureInviteResult(
                        TeamInvite.fromMap(snapshot.data.orEmpty()),
                        created = false,
                    )
                }
            }
        }.await()
    }

    suspend fun createInvite(
        teamId: String,
        createdBy: String,
        createdByEmail: String,
    ): TeamInvite {
        val inviteRef = invites.document(teamId)
        val directoryRef = directory.document(teamId)
        return db.runTransaction { transaction ->
            val inviteSnapshot = transaction.get(inviteRef)
            val directorySnapshot = transaction.get(directoryRef)
            val invite = if (inviteSnapshot.exists()) {
                TeamInvite.fromMap(inviteSnapshot.data.orEmpty())
            } else {
                TeamInvite.newInvite(createdBy).also { transaction.set(inviteRef, it.toMap()) }
            }
            if (!directorySnapshot.exists()) {
                transaction.set(directoryRef, directoryData(createdBy, createdByEmail))
            }
            invite
        }.await()
    }

    suspend fun rotateInvite(teamId: String, updatedBy: String): TeamInvite {
        val ref = invites.document(teamId)
        return db.runTransaction { transaction ->
            val snapshot = transaction.get(ref)
            val existing = if (snapshot.exists()) TeamInvite.fromMap(snapshot.data.orEmpty()) else null
            val fresh = TeamInvite.newInvite(createdBy = existing?.createdBy ?: updatedBy)
            val rotated = fresh.copy(
                createdAt = existing?.createdAt ?: fresh.createdAt,
            )
            transaction.set(ref, rotated.toMap())
            rotated
        }.await()
    }

    private fun directoryData(createdBy: String, createdByEmail: String): Map<String, Any?> =
        mapOf(
            "createdBy" to createdBy,
            "createdByEmail" to createdByEmail,
            "createdAt" to System.currentTimeMillis(),
        )
}
