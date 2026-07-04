package gt.guardian.cadejo.domain.model

import gt.guardian.cadejo.domain.hex.Hex

/** Outcome of the current level. */
enum class GameStatus { PLAYING, WON, LOST }

/**
 * A complete, immutable snapshot of a single level in progress.
 *
 * The game is a sequence of these, each produced from the previous by
 * [gt.guardian.cadejo.domain.engine.GameEngine.reduce]. Everything needed to
 * advance a turn lives here — board, the Cadejo, the traveler being escorted, the
 * enemies, ability cooldowns, and the PRNG state — so the engine is a pure
 * function with no hidden global state.
 *
 * Escort model: the Cadejo ([player]) leads the way to the [goal]; the [traveler]
 * trails behind, and the enemies hunt the traveler. The player wins by reaching
 * the goal (leading the traveler to safety) and loses if an enemy catches the
 * player or the unshielded traveler.
 */
data class GameState(
    val board: Board,
    val player: Hex,
    val traveler: Hex,
    val enemies: List<Enemy>,
    val goal: Hex,
    val seed: Long,
    val rngState: Long,
    val level: Int = 1,
    val turn: Int = 0,
    val status: GameStatus = GameStatus.PLAYING,
    val travelerShield: Int = 0,
    val abilities: List<AbilityState> = Balance.defaultAbilities(),
    val score: Int = 0,
    val lastPlayerStep: Hex? = null,
) {
    val isOver: Boolean get() = status != GameStatus.PLAYING

    fun ability(id: AbilityId): AbilityState? = abilities.firstOrNull { it.id == id }

    /** Hexes the player may legally step to this turn (adjacent, on-board, not a wall, not the traveler). */
    fun legalMoves(): List<Hex> =
        player.neighbors().filter { board.isWalkable(it) && it != traveler }
}
