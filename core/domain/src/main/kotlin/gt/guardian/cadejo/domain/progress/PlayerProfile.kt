package gt.guardian.cadejo.domain.progress

/** What a coin unlock affects. */
enum class UnlockCategory { SKIN, MODIFIER }

/**
 * Everything permanently purchasable with coins. Cost and category live on the
 * enum so the shop, the economy and the save format all agree on one source of
 * truth. Adding an item is a single line here.
 */
enum class UnlockId(
    val cost: Int,
    val category: UnlockCategory,
) {
    SKIN_JADE(150, UnlockCategory.SKIN),
    SKIN_OBSIDIAN(200, UnlockCategory.SKIN),
    SKIN_DAWN(250, UnlockCategory.SKIN),
    MODIFIER_SWIFT(400, UnlockCategory.MODIFIER), // -1 turn on every cooldown
    MODIFIER_WARD(400, UnlockCategory.MODIFIER), // each level starts with a shield
}

/**
 * The player's permanent progression. Immutable; the repository returns updated
 * copies. `coins`, `unlocks` and `adsRemoved` are the *sensitive* fields protected
 * by the HMAC signature (see the integrity package) — tampering with them on disk
 * is detected and reset.
 */
data class PlayerProfile(
    val coins: Long = 0,
    val unlocks: Set<UnlockId> = emptySet(),
    val selectedSkin: UnlockId? = null,
    val adsRemoved: Boolean = false,
    val totalRuns: Int = 0,
    val bestScore: Int = 0,
) {
    fun owns(id: UnlockId): Boolean = id in unlocks

    val ownedModifiers: Set<UnlockId> get() = unlocks.filter { it.category == UnlockCategory.MODIFIER }.toSet()

    companion object {
        /** The safe default a fresh — or tamper-reset — profile falls back to. */
        val INITIAL = PlayerProfile()
    }
}

/** How a finished run pays out. Kept deterministic so tests can assert exact coins. */
object Economy {
    fun coinsForRun(
        score: Int,
        reachedLevel: Int,
    ): Long =
        (score / 10L) + (reachedLevel * 5L)
}

/** The result of attempting to buy an unlock. */
sealed interface PurchaseResult {
    data class Success(
        val profile: PlayerProfile,
    ) : PurchaseResult

    data object AlreadyOwned : PurchaseResult

    data object InsufficientCoins : PurchaseResult
}
