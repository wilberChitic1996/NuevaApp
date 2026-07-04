package gt.guardian.cadejo.domain.progress

import kotlinx.coroutines.flow.Flow

/**
 * The progression store, as seen by the rest of the app. The interface lives in
 * the domain; :core:data provides the Room-backed, HMAC-signed implementation.
 * Features depend only on this abstraction (Dependency Inversion).
 */
interface ProgressRepository {

    /** The current profile, emitting on every change. */
    val profile: Flow<PlayerProfile>

    /** Pay out a finished run: add coins, bump stats, append to history. Returns the new profile. */
    suspend fun awardRun(record: RunRecord): PlayerProfile

    /** Add coins directly (e.g. a rewarded "double coins" ad). Returns the new profile. */
    suspend fun addCoins(amount: Long): PlayerProfile

    /** Attempt to buy an unlock with coins. */
    suspend fun purchase(id: UnlockId): PurchaseResult

    /** Equip a purchased skin (or null to use the default). No-op if not owned. */
    suspend fun selectSkin(id: UnlockId?)

    /** Mark ads as removed (set after a verified "remove ads" purchase in Phase 5). */
    suspend fun setAdsRemoved(removed: Boolean)

    /** The most recent runs, newest first. */
    fun recentRuns(limit: Int = 20): Flow<List<RunRecord>>
}
