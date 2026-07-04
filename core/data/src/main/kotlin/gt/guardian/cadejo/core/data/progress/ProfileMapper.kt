package gt.guardian.cadejo.core.data.progress

import gt.guardian.cadejo.core.data.db.ProfileEntity
import gt.guardian.cadejo.core.data.db.RunHistoryEntity
import gt.guardian.cadejo.domain.integrity.SaveIntegrity
import gt.guardian.cadejo.domain.integrity.Signer
import gt.guardian.cadejo.domain.progress.PlayerProfile
import gt.guardian.cadejo.domain.progress.RunOutcome
import gt.guardian.cadejo.domain.progress.RunRecord
import gt.guardian.cadejo.domain.progress.UnlockId
import gt.guardian.cadejo.domain.run.RunMode

/** Mapping between Room entities and domain models, including signing/verification. */
internal object ProfileMapper {

    fun toDomainUnsafe(entity: ProfileEntity): PlayerProfile = PlayerProfile(
        coins = entity.coins,
        unlocks = parseUnlocks(entity.unlocksCsv),
        selectedSkin = entity.selectedSkin?.let { runCatching { UnlockId.valueOf(it) }.getOrNull() },
        adsRemoved = entity.adsRemoved,
        totalRuns = entity.totalRuns,
        bestScore = entity.bestScore,
    )

    /** Load a profile and reset it to the safe default if its signature doesn't check out. */
    fun toDomainVerified(entity: ProfileEntity, signer: Signer): PlayerProfile {
        val loaded = toDomainUnsafe(entity)
        return SaveIntegrity.sanitized(loaded, entity.signature, signer)
    }

    fun toEntity(profile: PlayerProfile, signer: Signer): ProfileEntity = ProfileEntity(
        coins = profile.coins,
        unlocksCsv = profile.unlocks.map(UnlockId::name).sorted().joinToString(","),
        selectedSkin = profile.selectedSkin?.name,
        adsRemoved = profile.adsRemoved,
        totalRuns = profile.totalRuns,
        bestScore = profile.bestScore,
        signature = SaveIntegrity.sign(profile, signer),
    )

    private fun parseUnlocks(csv: String): Set<UnlockId> =
        csv.split(",").mapNotNull { name ->
            if (name.isBlank()) null else runCatching { UnlockId.valueOf(name) }.getOrNull()
        }.toSet()

    fun toEntity(record: RunRecord, finishedAt: Long): RunHistoryEntity = RunHistoryEntity(
        seed = record.seed,
        mode = record.mode.name,
        reachedLevel = record.reachedLevel,
        score = record.score,
        outcome = record.outcome.name,
        durationMs = record.durationMs,
        finishedAt = finishedAt,
    )

    fun toDomain(entity: RunHistoryEntity): RunRecord = RunRecord(
        seed = entity.seed,
        mode = runCatching { RunMode.valueOf(entity.mode) }.getOrDefault(RunMode.ROGUE),
        reachedLevel = entity.reachedLevel,
        score = entity.score,
        outcome = runCatching { RunOutcome.valueOf(entity.outcome) }.getOrDefault(RunOutcome.FAILED),
        durationMs = entity.durationMs,
    )
}
