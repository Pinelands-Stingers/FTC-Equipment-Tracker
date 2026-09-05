package za.org.team25310.ftcequipmenttracker.data

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Credentials
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import za.org.team25310.ftcequipmenttracker.data.model.FtcTeam
import za.org.team25310.ftcequipmenttracker.data.model.FtcTeamsResponse

object FtcEventsApi {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    private val client = OkHttpClient()

    fun fetchTeams(): List<FtcTeam> {
        val all = mutableListOf<FtcTeam>()
        var page = 1
        while (true) {
            val response = fetchPage(page)
            all.addAll(response.teams)
            if (response.teams.isEmpty() || page >= response.pageTotal) break
            page++
        }
        return all.filter { it.country == FtcApiConfig.COUNTRY }
    }

    private fun fetchPage(page: Int): FtcTeamsResponse {
        val url = FtcApiConfig.BASE_URL.toHttpUrl().newBuilder()
            .addPathSegment(FtcApiConfig.SEASON)
            .addPathSegment("teams")
            .addQueryParameter("country", FtcApiConfig.COUNTRY)
            .addQueryParameter("page", page.toString())
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Authorization", Credentials.basic(FtcApiConfig.USERNAME, FtcApiConfig.TOKEN))
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("FTC API request failed: HTTP ${response.code}")
            }
            val body = response.body?.string() ?: error("Empty FTC API response")
            return json.decodeFromString<FtcTeamsResponse>(body)
        }
    }
}
