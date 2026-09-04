package za.org.team25310.ftcequipmenttracker.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import za.org.team25310.ftcequipmenttracker.data.model.Part
import za.org.team25310.ftcequipmenttracker.data.model.Team

object CatalogRepository {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun loadParts(context: Context): List<Part> = withContext(Dispatchers.IO) {
        val text = context.assets.open("parts.json").bufferedReader().use { it.readText() }
        json.decodeFromString<List<Part>>(text)
    }

    suspend fun loadTeams(context: Context): List<Team> = withContext(Dispatchers.IO) {
        context.assets.open("teams.csv").bufferedReader().use { reader ->
            reader.lineSequence()
                .drop(1)
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    val cols = line.split(',')
                    if (cols.size < 3) return@mapNotNull null
                    Team(
                        teamNumber = cols[0].trim(),
                        name = cols[1].trim(),
                        city = cols[2].trim(),
                    )
                }
                .toList()
        }
    }
}
