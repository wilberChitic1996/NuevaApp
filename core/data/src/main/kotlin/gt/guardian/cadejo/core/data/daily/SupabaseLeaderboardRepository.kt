package gt.guardian.cadejo.core.data.daily

import gt.guardian.cadejo.domain.daily.LeaderboardEntry
import gt.guardian.cadejo.domain.daily.LeaderboardRepository
import gt.guardian.cadejo.domain.daily.ScoreSubmission
import gt.guardian.cadejo.domain.daily.SubmitResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/** Base URL + anon key for the Supabase project; blank => leaderboard disabled. */
data class LeaderboardConfig(val baseUrl: String, val anonKey: String) {
    val isConfigured: Boolean get() = baseUrl.isNotBlank() && anonKey.isNotBlank()
}

/**
 * Talks to the Supabase Edge Function (`submit-daily-score`) and REST table for the
 * daily leaderboard. When the project isn't configured (default dev builds) every
 * call is a safe no-op so the game stays fully playable offline.
 *
 * The client sends the seed + intent tape + a Play Integrity token, NOT a trusted
 * score: the Edge Function re-runs the tape and recomputes the score server-side.
 * See `backend/supabase/functions/submit-daily-score` for the validation logic.
 */
@Singleton
class SupabaseLeaderboardRepository @Inject constructor(
    private val config: LeaderboardConfig,
) : LeaderboardRepository {

    override val isEnabled: Boolean get() = config.isConfigured

    override suspend fun submit(submission: ScoreSubmission): SubmitResult {
        if (!config.isConfigured) return SubmitResult.Disabled
        return withContext(Dispatchers.IO) {
            runCatching {
                val body = JSONObject().apply {
                    put("date_utc", submission.dateUtc)
                    put("seed", submission.seed)
                    put("score", submission.score)
                    put("reached_level", submission.reachedLevel)
                    put("intents", submission.intentsEncoded)
                    put("display_name", submission.displayName)
                    put("integrity_token", submission.integrityToken ?: JSONObject.NULL)
                }
                val (code, response) = post("${config.baseUrl}/functions/v1/submit-daily-score", body.toString())
                if (code in 200..299) {
                    SubmitResult.Accepted
                } else {
                    val reason = runCatching { JSONObject(response).optString("error", "HTTP $code") }.getOrDefault("HTTP $code")
                    SubmitResult.Rejected(reason)
                }
            }.getOrElse { SubmitResult.Rejected(it.message ?: "network error") }
        }
    }

    override fun topScores(dateUtc: String, limit: Int): Flow<List<LeaderboardEntry>> = flow {
        if (!config.isConfigured) {
            emit(emptyList())
            return@flow
        }
        val encodedDate = URLEncoder.encode(dateUtc, "UTF-8")
        val url = "${config.baseUrl}/rest/v1/daily_leaderboard" +
            "?date_utc=eq.$encodedDate&order=score.desc&limit=$limit" +
            "&select=display_name,score"
        val (code, response) = get(url)
        if (code !in 200..299) {
            emit(emptyList())
            return@flow
        }
        val array = JSONArray(response)
        val entries = (0 until array.length()).map { i ->
            val row = array.getJSONObject(i)
            LeaderboardEntry(rank = i + 1, displayName = row.optString("display_name", "—"), score = row.optInt("score"))
        }
        emit(entries)
    }.flowOn(Dispatchers.IO).catch { emit(emptyList()) }

    // --- Minimal HTTP (no extra dependency) --------------------------------

    private fun post(urlStr: String, json: String): Pair<Int, String> {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 10_000
            readTimeout = 10_000
            applyAuthHeaders()
            setRequestProperty("Content-Type", "application/json")
        }
        conn.outputStream.use { it.write(json.encodeToByteArray()) }
        return conn.readResult()
    }

    private fun get(urlStr: String): Pair<Int, String> {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            applyAuthHeaders()
        }
        return conn.readResult()
    }

    private fun HttpURLConnection.applyAuthHeaders() {
        setRequestProperty("apikey", config.anonKey)
        setRequestProperty("Authorization", "Bearer ${config.anonKey}")
    }

    private fun HttpURLConnection.readResult(): Pair<Int, String> {
        val code = responseCode
        val stream = if (code in 200..299) inputStream else errorStream
        val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
        disconnect()
        return code to body
    }
}
