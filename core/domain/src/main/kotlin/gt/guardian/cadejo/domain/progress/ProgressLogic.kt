package gt.guardian.cadejo.domain.progress

/**
 * Pure progression rules. The repository persists whatever these functions
 * compute, so the economy (coin payouts, purchase validation) is fully unit-tested
 * on the JVM with no database in the loop.
 */
object ProgressLogic {
    /** Apply a finished run to the profile: coins earned, stats, best score. */
    fun award(
        profile: PlayerProfile,
        record: RunRecord,
    ): PlayerProfile {
        val earned = Economy.coinsForRun(record.score, record.reachedLevel)
        return profile.copy(
            coins = profile.coins + earned,
            totalRuns = profile.totalRuns + 1,
            bestScore = maxOf(profile.bestScore, record.score),
        )
    }

    /** Validate and apply a purchase, returning a typed outcome. */
    fun purchase(
        profile: PlayerProfile,
        id: UnlockId,
    ): PurchaseResult =
        when {
            profile.owns(id) -> PurchaseResult.AlreadyOwned
            profile.coins < id.cost -> PurchaseResult.InsufficientCoins
            else ->
                PurchaseResult.Success(
                    profile.copy(
                        coins = profile.coins - id.cost,
                        unlocks = profile.unlocks + id,
                        // Auto-equip a newly bought skin for immediate feedback.
                        selectedSkin = if (id.category == UnlockCategory.SKIN) id else profile.selectedSkin,
                    ),
                )
        }

    /** Equip a skin only if it's owned; null clears the selection. */
    fun selectSkin(
        profile: PlayerProfile,
        id: UnlockId?,
    ): PlayerProfile =
        when {
            id == null -> profile.copy(selectedSkin = null)
            profile.owns(id) && id.category == UnlockCategory.SKIN -> profile.copy(selectedSkin = id)
            else -> profile
        }
}
