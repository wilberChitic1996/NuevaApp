package gt.guardian.cadejo.domain.model

import gt.guardian.cadejo.domain.hex.Hex

/** Outcome of the current level. */
enum class GameStatus { PLAYING, WON, LOST }

/**
 * A complete, immutable snapshot of a single level in progress.
 *
 * This is the heart of the game: a run is nothing more than a sequence of
 * [GameState]s, each produced from the previous one by
 * [gt.guardian.cadejo.domain.engine.GameEngine.reduce]. Everything the engine
 * needs to advance a turn lives here — including [rngState], so the engine can
 * stay a pure function with no hidden global randomness.
 *
 * @param player position of the white Cadejo (the piece the user controls).
 * @param goal the hex the Cadejo must reach to win the level.
 * @param rngState the PRNG state carried between turns for determinism.
 * @param seed the seed the level was generated from (kept for replay/validation).
 */
data class GameState(
    val board: Board,
    val player: Hex,
    val enemies: List<Enemy>,
    val goal: Hex,
    val seed: Long,
    val rngState: Long,
    val level: Int = 1,
    val turn: Int = 0,
    val status: GameStatus = GameStatus.PLAYING,
) {
    val isOver: Boolean get() = status != GameStatus.PLAYING

    /** Hexes the player may legally step to this turn (adjacent, on-board, not a wall). */
    fun legalMoves(): List<Hex> =
        player.neighbors().filter { board.isWalkable(it) }
}
