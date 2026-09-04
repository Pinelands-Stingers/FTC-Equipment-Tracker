package za.org.team25310.ftcequipmenttracker.data

import za.org.team25310.ftcequipmenttracker.data.model.Team

object AppConfig {
    const val MY_TEAM_NUMBER = "25310"
    const val MY_TEAM_NAME = "Pinelands Stingers"
    const val MY_TEAM_CITY = "Cape Town"

    val myTeam = Team(MY_TEAM_NUMBER, MY_TEAM_NAME, MY_TEAM_CITY)
}
