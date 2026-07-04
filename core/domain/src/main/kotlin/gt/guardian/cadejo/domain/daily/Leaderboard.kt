package gt.guardian.cadejo.domain.daily

import kotlinx.coroutines.flow.Flow

/**
 * A score the client submits for the daily challenge. Crucially it carries the
 * [intentsEncoded] tape and [seed], NOT trust in [score]: the backend re-runs the
 * tape from the seed and recomputes the score itself, accepting the submission
 * only if the numbers match and the [integrityToken] proves a genuine app/device.
 */
data class ScoreSubmission(
    val dateUtc: String,
    val seed: Long,
    val reachedLevel: Int,
    val score: Int,
    val intentsEncoded: String,
    val displayName: String,
    val integrityToken: String? = null,
)

/** One row of the daily leaderboard. */
data class LeaderboardEntry(
    val rank: Int,
    val displayName: String,
    val score: Int,
)

/** Outcome of a submission attempt. */
sealed interface SubmitResult {
    data object Accepted : SubmitResult

    data class Rejected(
        val reason: String,
    ) : SubmitResult

    /** The leaderboard backend isn't configured in this build. */
    data object Disabled : SubmitResult
}

/**
 * Daily leaderboard access. The implementation in :core:data talks to the Supabase
 * Edge Function when configured, and no-ops (Disabled) otherwise so the app stays
 * fully playable without a backend.
 */
interface LeaderboardRepository {
    val isEnabled: Boolean

    suspend fun submit(submission: ScoreSubmission): SubmitResult

    fun topScores(
        dateUtc: String,
        limit: Int = 20,
    ): Flow<List<LeaderboardEntry>>
}
