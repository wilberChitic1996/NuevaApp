package gt.guardian.cadejo.core.data.progress

import gt.guardian.cadejo.core.data.db.ProfileDao
import gt.guardian.cadejo.core.data.db.RunHistoryDao
import gt.guardian.cadejo.domain.integrity.Signer
import gt.guardian.cadejo.domain.progress.PlayerProfile
import gt.guardian.cadejo.domain.progress.ProgressLogic
import gt.guardian.cadejo.domain.progress.ProgressRepository
import gt.guardian.cadejo.domain.progress.PurchaseResult
import gt.guardian.cadejo.domain.progress.RunRecord
import gt.guardian.cadejo.domain.progress.UnlockId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed progression store. Every read verifies the HMAC signature and every
 * write re-signs, so on-disk tampering is caught on the next load and reset. A
 * [Mutex] serialises read-modify-write cycles so concurrent awards/purchases can't
 * race and lose coins.
 */
@Singleton
class RoomProgressRepository @Inject constructor(
    private val profileDao: ProfileDao,
    private val runHistoryDao: RunHistoryDao,
    private val signer: Signer,
) : ProgressRepository {
    private val writeLock = Mutex()

    override val profile: Flow<PlayerProfile> =
        profileDao.observe().map { entity ->
            entity?.let { ProfileMapper.toDomainVerified(it, signer) } ?: PlayerProfile.INITIAL
        }

    override fun recentRuns(limit: Int): Flow<List<RunRecord>> =
        runHistoryDao.recent(limit).map { rows -> rows.map(ProfileMapper::toDomain) }

    override suspend fun awardRun(record: RunRecord): PlayerProfile =
        writeLock.withLock {
            val current = currentProfile()
            val updated = ProgressLogic.award(current, record)
            persist(updated)
            runHistoryDao.insert(ProfileMapper.toEntity(record, System.currentTimeMillis()))
            updated
        }

    override suspend fun addCoins(amount: Long): PlayerProfile =
        writeLock.withLock {
            val current = currentProfile()
            val updated = current.copy(coins = (current.coins + amount).coerceAtLeast(0))
            persist(updated)
            updated
        }

    override suspend fun purchase(id: UnlockId): PurchaseResult =
        writeLock.withLock {
            when (val result = ProgressLogic.purchase(currentProfile(), id)) {
                is PurchaseResult.Success -> {
                    persist(result.profile)
                    result
                }
                else -> result
            }
        }

    override suspend fun selectSkin(id: UnlockId?) =
        writeLock.withLock {
            persist(ProgressLogic.selectSkin(currentProfile(), id))
        }

    override suspend fun setAdsRemoved(removed: Boolean) =
        writeLock.withLock {
            persist(currentProfile().copy(adsRemoved = removed))
        }

    /** Read the trusted current profile, creating a signed default row if none exists yet. */
    private suspend fun currentProfile(): PlayerProfile {
        val entity = profileDao.get()
        if (entity == null) {
            persist(PlayerProfile.INITIAL)
            return PlayerProfile.INITIAL
        }
        return ProfileMapper.toDomainVerified(entity, signer)
    }

    private suspend fun persist(profile: PlayerProfile) {
        profileDao.upsert(ProfileMapper.toEntity(profile, signer))
    }
}
