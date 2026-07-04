package gt.guardian.cadejo.domain.engine

import gt.guardian.cadejo.domain.enemy.Patterns
import gt.guardian.cadejo.domain.model.GameState
import gt.guardian.cadejo.domain.model.GameStatus
import gt.guardian.cadejo.domain.model.Intent

/**
 * The pure, deterministic game loop, expressed as a **reducer**:
 *
 * ```
 * GameState  --Intent-->  GameEngine.reduce  -->  GameState
 * ```
 *
 * [reduce] takes the current state and a player [Intent] and returns the next
 * state. It performs no I/O, reads no clock, and touches no global RNG — all
 * randomness flows through [GameState.rngState]. That purity is what makes the
 * whole game unit-testable on the JVM and replayable for score validation.
 *
 * Turn order for a single [reduce]:
 *  1. Apply the player's intent (illegal moves are ignored, leaving state as-is).
 *  2. Win check: did the Cadejo reach the goal?
 *  3. Move every (un-stunned) enemy according to its pattern.
 *  4. Lose check: does an enemy now share the Cadejo's hex?
 *  5. Decrement stun timers and advance the turn counter.
 */
object GameEngine {

    fun reduce(state: GameState, intent: Intent): GameState {
        if (state.isOver) return state

        val afterPlayer = applyPlayerIntent(state, intent)

        // Reaching the goal wins immediately, before enemies get to act.
        if (afterPlayer.player == afterPlayer.goal) {
            return afterPlayer.copy(status = GameStatus.WON, turn = afterPlayer.turn + 1)
        }

        // Walking onto an enemy is fatal.
        if (afterPlayer.enemies.any { it.position == afterPlayer.player }) {
            return afterPlayer.copy(status = GameStatus.LOST, turn = afterPlayer.turn + 1)
        }

        val afterEnemies = moveEnemies(afterPlayer)

        val captured = afterEnemies.enemies.any { it.position == afterEnemies.player }
        val status = if (captured) GameStatus.LOST else GameStatus.PLAYING

        return afterEnemies.copy(status = status, turn = afterEnemies.turn + 1)
    }

    private fun applyPlayerIntent(state: GameState, intent: Intent): GameState = when (intent) {
        is Intent.Wait -> state
        is Intent.Move -> {
            val target = intent.target
            val legal = target in state.player.neighbors() && state.board.isWalkable(target)
            if (legal) state.copy(player = target) else state
        }
    }

    private fun moveEnemies(state: GameState): GameState {
        // Move enemies one at a time so each sees the updated positions of those
        // that already moved this turn — order is stable (list order) for determinism.
        var working = state
        val updated = working.enemies.map { enemy ->
            if (enemy.stunnedTurns > 0) {
                enemy.copy(stunnedTurns = enemy.stunnedTurns - 1)
            } else {
                val next = Patterns.of(enemy.pattern).nextMove(working, enemy)
                val moved = enemy.copy(position = next)
                // Reflect this enemy's new position into the working state for the next enemy.
                working = working.copy(
                    enemies = working.enemies.map { if (it.id == moved.id) moved else it },
                )
                moved
            }
        }
        return working.copy(enemies = updated)
    }
}
