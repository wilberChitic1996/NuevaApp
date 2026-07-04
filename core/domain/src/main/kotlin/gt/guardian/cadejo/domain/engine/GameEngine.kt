package gt.guardian.cadejo.domain.engine

import gt.guardian.cadejo.domain.enemy.HexPathing
import gt.guardian.cadejo.domain.enemy.Patterns
import gt.guardian.cadejo.domain.hex.Hex
import gt.guardian.cadejo.domain.model.AbilityId
import gt.guardian.cadejo.domain.model.Balance
import gt.guardian.cadejo.domain.model.GameState
import gt.guardian.cadejo.domain.model.GameStatus
import gt.guardian.cadejo.domain.model.Intent

/**
 * The pure, deterministic game loop, expressed as a **reducer**:
 * `GameState --Intent--> reduce --> GameState`.
 *
 * No I/O, no clock, no global RNG. That purity is what makes the whole game
 * unit-testable on the JVM and replayable for server-side score validation.
 *
 * Turn order for a single [reduce]:
 *  1. Apply the player's action (illegal actions are rejected — the turn does not
 *     advance, so a misplaced tap never costs the player).
 *  2. Win check: did the Cadejo reach the goal? (Leads the traveler to safety.)
 *  3. The traveler trails one step toward the Cadejo.
 *  4. Every un-stunned enemy advances toward the traveler by its pattern.
 *  5. Lose check: an enemy on the Cadejo, or on the unshielded traveler.
 *  6. Tick cooldowns and the shield; advance the turn counter.
 */
object GameEngine {

    fun reduce(state: GameState, intent: Intent): GameState {
        if (state.isOver) return state

        // Step 1 — player. Null means the action was illegal: no-op, no turn spent.
        val afterPlayer = applyPlayerAction(state, intent) ?: return state

        // Step 2 — win: the Cadejo reaches the goal, leading the traveler out.
        if (afterPlayer.player == afterPlayer.goal) {
            return afterPlayer.copy(
                status = GameStatus.WON,
                turn = afterPlayer.turn + 1,
                score = afterPlayer.score + levelCompletionScore(afterPlayer),
                abilities = afterPlayer.abilities.map { it.ticked() },
            )
        }

        // The Cadejo walking into an enemy is fatal.
        if (afterPlayer.enemies.any { it.position == afterPlayer.player }) {
            return afterPlayer.copy(status = GameStatus.LOST, turn = afterPlayer.turn + 1)
        }

        // Step 3 — traveler trails the Cadejo.
        val afterTraveler = moveTraveler(afterPlayer)

        // Step 4 — enemies hunt the traveler.
        val afterEnemies = moveEnemies(afterTraveler)

        // Step 5 — resolve captures.
        val caughtPlayer = afterEnemies.enemies.any { it.position == afterEnemies.player }
        val caughtTraveler = afterEnemies.travelerShield == 0 &&
            afterEnemies.enemies.any { it.position == afterEnemies.traveler }
        val status = if (caughtPlayer || caughtTraveler) GameStatus.LOST else GameStatus.PLAYING

        // Step 6 — tick timers, advance the turn.
        return afterEnemies.copy(
            status = status,
            turn = afterEnemies.turn + 1,
            abilities = afterEnemies.abilities.map { it.ticked() },
            travelerShield = (afterEnemies.travelerShield - 1).coerceAtLeast(0),
        )
    }

    // --- Player action ------------------------------------------------------

    private fun applyPlayerAction(state: GameState, intent: Intent): GameState? = when (intent) {
        is Intent.Wait -> state.copy(lastPlayerStep = null)
        is Intent.Move -> applyMove(state, intent.target)
        is Intent.UseAbility -> applyAbility(state, intent)
    }

    private fun applyMove(state: GameState, target: Hex): GameState? {
        val legal = target in state.player.neighbors() &&
            state.board.isWalkable(target) &&
            target != state.traveler
        if (!legal) return null
        return state.copy(player = target, lastPlayerStep = target - state.player)
    }

    private fun applyAbility(state: GameState, intent: Intent.UseAbility): GameState? {
        val ability = state.ability(intent.id)?.takeIf { it.isReady } ?: return null
        val triggered = state.abilities.map { if (it.id == ability.id) it.triggered() else it }

        return when (intent.id) {
            AbilityId.HOWL -> {
                val stunned = state.enemies.map { enemy ->
                    if (enemy.position.distanceTo(state.player) <= Balance.HOWL_RADIUS) {
                        enemy.copy(stunnedTurns = maxOf(enemy.stunnedTurns, Balance.HOWL_STUN_TURNS + 1))
                    } else {
                        enemy
                    }
                }
                state.copy(enemies = stunned, abilities = triggered, lastPlayerStep = null)
            }

            AbilityId.PROTECTIVE_LIGHT ->
                state.copy(
                    travelerShield = Balance.LIGHT_SHIELD_TURNS + 1,
                    abilities = triggered,
                    lastPlayerStep = null,
                )

            AbilityId.LEAP -> {
                val target = intent.target ?: return null
                val distance = target.distanceTo(state.player)
                val legal = distance in 1..Balance.LEAP_RANGE &&
                    state.board.isWalkable(target) &&
                    target != state.traveler
                if (!legal) return null
                // A leap has no single direction, so it doesn't feed the mirror.
                state.copy(player = target, abilities = triggered, lastPlayerStep = null)
            }
        }
    }

    // --- World reaction -----------------------------------------------------

    private fun moveTraveler(state: GameState): GameState {
        // The traveler trails the Cadejo, avoiding walls, enemies and the Cadejo's cell.
        val blocked = buildSet {
            state.enemies.forEach { add(it.position) }
            add(state.player)
        }
        val next = HexPathing.stepFrom(state.board, state.traveler, target = state.player, blocked = blocked)
        return state.copy(traveler = next)
    }

    private fun moveEnemies(state: GameState): GameState {
        var working = state
        val updated = working.enemies.map { enemy ->
            if (enemy.stunnedTurns > 0) {
                enemy.copy(stunnedTurns = enemy.stunnedTurns - 1)
            } else {
                val blocked = buildSet {
                    working.enemies.forEach { if (it.id != enemy.id) add(it.position) }
                    if (working.travelerShield > 0) add(working.traveler)
                }
                val moved = Patterns.of(enemy.pattern).step(working, enemy, working.traveler, blocked)
                working = working.copy(enemies = working.enemies.map { if (it.id == moved.id) moved else it })
                moved
            }
        }
        return working.copy(enemies = updated)
    }

    // --- Scoring ------------------------------------------------------------

    /** Points awarded for clearing a level: base + level bonus + a speed bonus. */
    private fun levelCompletionScore(state: GameState): Int {
        val base = 100
        val levelBonus = state.level * 25
        val speedBonus = (Balance.PAR_TURNS - state.turn).coerceAtLeast(0) * 5
        val shieldSaved = if (state.travelerShield > 0) 15 else 0
        return base + levelBonus + speedBonus + shieldSaved
    }
}
