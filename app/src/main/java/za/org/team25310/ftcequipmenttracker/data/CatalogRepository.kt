package za.org.team25310.ftcequipmenttracker.data

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import za.org.team25310.ftcequipmenttracker.data.model.FtcTeam
import za.org.team25310.ftcequipmenttracker.data.model.Team

object CatalogRepository {

    private const val CACHE_FILE = "ftc_teams_cache.json"
    private const val CACHE_MAX_AGE_MS = 24L * 60L * 60L * 1000L

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun loadTeams(context: Context): List<Team> = withContext(Dispatchers.IO) {
        val cached = loadCachedTeams(context)
        if (cached != null && isCacheFresh(context)) {
            return@withContext cached
        }

        val configured = FtcApiConfig.USERNAME.isNotBlank() && FtcApiConfig.TOKEN.isNotBlank()
        val fresh = if (configured) runCatching { FtcEventsApi.fetchTeams() }.getOrNull() else null
        if (!fresh.isNullOrEmpty()) {
            saveCachedTeams(context, fresh)
            return@withContext fresh.map { it.toTeam() }
        }

        cached ?: loadTeamsFromAssets(context)
    }

    suspend fun refreshTeams(context: Context): List<Team> = withContext(Dispatchers.IO) {
        if (FtcApiConfig.USERNAME.isBlank() || FtcApiConfig.TOKEN.isBlank()) {
            error("FTC API credentials are not configured")
        }
        val fresh = FtcEventsApi.fetchTeams()
        if (fresh.isEmpty()) {
            error("No teams returned (season ${FtcApiConfig.SEASON}, country ${FtcApiConfig.COUNTRY})")
        }
        saveCachedTeams(context, fresh)
        fresh.map { it.toTeam() }
    }

    private fun loadCachedTeams(context: Context): List<Team>? = runCatching {
        val file = File(context.filesDir, CACHE_FILE)
        if (!file.exists()) return@runCatching null
        json.decodeFromString<List<FtcTeam>>(file.readText()).map { it.toTeam() }
    }.getOrNull()

    private fun saveCachedTeams(context: Context, teams: List<FtcTeam>) {
        runCatching {
            File(context.filesDir, CACHE_FILE).writeText(json.encodeToString(teams))
        }
    }

    private fun isCacheFresh(context: Context): Boolean {
        val file = File(context.filesDir, CACHE_FILE)
        if (!file.exists()) return false
        return System.currentTimeMillis() - file.lastModified() < CACHE_MAX_AGE_MS
    }

    private fun loadTeamsFromAssets(context: Context): List<Team> =
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
