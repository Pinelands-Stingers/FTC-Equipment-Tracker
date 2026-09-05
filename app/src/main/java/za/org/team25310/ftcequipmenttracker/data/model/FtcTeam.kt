package za.org.team25310.ftcequipmenttracker.data.model

import kotlinx.serialization.Serializable

@Serializable
data class FtcTeamsResponse(
    val teamCountTotal: Int = 0,
    val teamCountPage: Int = 0,
    val pageCurrent: Int = 0,
    val pageTotal: Int = 0,
    val teams: List<FtcTeam> = emptyList(),
)

@Serializable
data class FtcTeam(
    val teamNumber: Int = 0,
    val nameFull: String? = null,
    val nameShort: String? = null,
    val city: String? = null,
    val country: String? = null,
) {
    fun toTeam(): Team = Team(
        teamNumber = teamNumber.toString(),
        name = nameFull ?: nameShort ?: "",
        city = city.orEmpty(),
    )
}
