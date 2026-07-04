package gt.guardian.cadejo.domain.model

import gt.guardian.cadejo.domain.hex.Hex

/**
 * A player action fed into the engine. Modelled as a sealed hierarchy so the
 * `when` in the engine is exhaustive and adding an action (e.g. an ability) is a
 * compile-time checklist rather than a runtime surprise.
 *
 * The full run is replayable from its seed + the ordered list of [Intent]s — the
 * mechanism the leaderboard backend uses to re-derive and verify a score.
 */
sealed interface Intent {
    /** Step the Cadejo to an adjacent hex. Ignored by the engine if not legal. */
    data class Move(val target: Hex) : Intent

    /** Pass the turn without moving (enemies still act). */
    data object Wait : Intent
}
