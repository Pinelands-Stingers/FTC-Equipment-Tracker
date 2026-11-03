package za.org.team25310.ftcequipmenttracker.data

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import za.org.team25310.ftcequipmenttracker.data.model.TeamInfo

object TeamAdminRepository {

    private val db = Firebase.firestore
    private val config = db.collection("appConfig").document("security")
    private val verifiedTeams = db.collection("verifiedTeams")
    private val reports = db.collection("teamReports")

    suspend fun isVerificationRequired(): Boolean =
        config.get().await().getBoolean("requireVerifiedTeams") ?: false

    suspend fun isTeamVerified(teamId: String): Boolean =
        verifiedTeams.document(teamId).get().await().exists()

    suspend fun getTeamInfo(teamId: String): TeamInfo {
        val claim = TeamInvitesRepository.getTeamClaim(teamId)
        return TeamInfo(
            teamId = teamId,
            claimed = claim != null,
            claimedByEmail = claim?.claimedByEmail.orEmpty(),
            claimedAt = claim?.claimedAt ?: 0L,
            verified = isTeamVerified(teamId),
        )
    }

    suspend fun reportTeam(
        teamId: String,
        reportedByUid: String,
        reportedByEmail: String,
        reason: String,
    ) {
        reports.add(
            mapOf(
                "teamId" to teamId,
                "reportedByUid" to reportedByUid,
                "reportedByEmail" to reportedByEmail,
                "reason" to reason.trim(),
                "status" to "open",
                "createdAt" to System.currentTimeMillis(),
            ),
        ).await()
    }
}
