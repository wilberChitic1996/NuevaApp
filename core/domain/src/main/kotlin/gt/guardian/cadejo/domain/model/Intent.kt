package gt.guardian.cadejo.domain.model

import gt.guardian.cadejo.domain.hex.Hex

/**
 * A player action fed into the engine. Sealed so the engine's `when` is
 * exhaustive and adding an action is a compile-time checklist.
 *
 * A run is replayable from its seed + the ordered list of [Intent]s — the
 * mechanism the leaderboard backend uses to re-derive and verify a score.
 */
sealed interface Intent {
    /** Step the Cadejo to an adjacent hex. Ignored by the engine if not legal. */
    data class Move(val target: Hex) : Intent

    /** Pass the turn without moving (the traveler and enemies still act). */
    data object Wait : Intent

    /**
     * Use an ability. [target] is required for [AbilityId.LEAP] (the destination)
     * and ignored for the others (Howl centres on the player, Light on the traveler).
     */
    data class UseAbility(val id: AbilityId, val target: Hex? = null) : Intent
}
