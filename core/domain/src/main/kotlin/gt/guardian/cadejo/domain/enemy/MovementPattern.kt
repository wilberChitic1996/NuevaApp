package gt.guardian.cadejo.domain.enemy

import gt.guardian.cadejo.domain.hex.Hex
import gt.guardian.cadejo.domain.model.Board
import gt.guardian.cadejo.domain.model.Enemy
import gt.guardian.cadejo.domain.model.GameState
import gt.guardian.cadejo.domain.model.PatternType

/**
 * Strategy for advancing one enemy. Every implementation must be deterministic:
 * same [GameState] + same [Enemy] -> same result. Ties break by [Hex.DIRECTIONS]
 * order so a run never depends on iteration order or hashing.
 *
 * Returns the moved [Enemy] (not just a hex) so stateful patterns like patrol can
 * advance their own waypoint index purely.
 */
fun interface MovementPattern {
    fun step(
        state: GameState,
        enemy: Enemy,
        target: Hex,
        blocked: Set<Hex>,
    ): Enemy
}

/** Shared BFS helpers used by the chase-like patterns. */
internal object HexPathing {
    /** BFS distance from [from] to every reachable walkable hex, respecting [blocked]. */
    fun distances(
        board: Board,
        from: Hex,
        blocked: Set<Hex>,
    ): Map<Hex, Int> {
        val dist = HashMap<Hex, Int>()
        val queue = ArrayDeque<Hex>()
        dist[from] = 0
        queue.add(from)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val d = dist.getValue(current)
            for (next in current.neighbors()) {
                if (next in dist || !board.isWalkable(next) || next in blocked) continue
                dist[next] = d + 1
                queue.add(next)
            }
        }
        return dist
    }

    /** The adjacent walkable hex from [from] that most reduces BFS distance to [target]; else [from]. */
    fun stepFrom(
        board: Board,
        from: Hex,
        target: Hex,
        blocked: Set<Hex>,
    ): Hex {
        val dist = distances(board, from = target, blocked = blocked)
        val candidates = from.neighbors().filter { board.isWalkable(it) && it !in blocked }
        val best = candidates.minByOrNull { dist[it] ?: Int.MAX_VALUE }
        val bestDist = best?.let { dist[it] ?: Int.MAX_VALUE } ?: Int.MAX_VALUE
        val hereDist = dist[from] ?: Int.MAX_VALUE
        return if (best != null && bestDist < hereDist) best else from
    }

    fun stepToward(
        state: GameState,
        enemy: Enemy,
        target: Hex,
        blocked: Set<Hex>,
    ): Hex =
        stepFrom(state.board, enemy.position, target, blocked)
}

/** Chase: shortest-path step toward the target (the traveler). */
object ChasePattern : MovementPattern {
    override fun step(
        state: GameState,
        enemy: Enemy,
        target: Hex,
        blocked: Set<Hex>,
    ): Enemy =
        enemy.copy(position = HexPathing.stepToward(state, enemy, target, blocked))
}

/**
 * Patrol: cycle through a fixed route. Steps toward the current waypoint; on
 * reaching it, advances to the next. Predictable by design — the player can learn
 * and exploit the loop.
 */
object PatrolPattern : MovementPattern {
    override fun step(
        state: GameState,
        enemy: Enemy,
        target: Hex,
        blocked: Set<Hex>,
    ): Enemy {
        if (enemy.patrolRoute.isEmpty()) return ChasePattern.step(state, enemy, target, blocked)
        val waypoint = enemy.patrolRoute[enemy.patrolIndex % enemy.patrolRoute.size]
        return if (enemy.position == waypoint) {
            val nextIndex = (enemy.patrolIndex + 1) % enemy.patrolRoute.size
            val next = enemy.patrolRoute[nextIndex]
            val moved = HexPathing.stepToward(state, enemy.copy(), next, blocked)
            enemy.copy(position = moved, patrolIndex = nextIndex)
        } else {
            enemy.copy(position = HexPathing.stepToward(state, enemy, waypoint, blocked))
        }
    }
}

/**
 * Mirror (the black Cadejo): repeats the player's last move direction. If the
 * player waited or that direction is blocked, it falls back to chasing — so it is
 * never fully passive.
 */
object MirrorPattern : MovementPattern {
    override fun step(
        state: GameState,
        enemy: Enemy,
        target: Hex,
        blocked: Set<Hex>,
    ): Enemy {
        val delta = state.lastPlayerStep
        if (delta != null) {
            val mirrored = enemy.position + delta
            if (state.board.isWalkable(mirrored) && mirrored !in blocked) {
                return enemy.copy(position = mirrored)
            }
        }
        return ChasePattern.step(state, enemy, target, blocked)
    }
}

/** Resolves a [PatternType] to its strategy. */
object Patterns {
    fun of(type: PatternType): MovementPattern =
        when (type) {
            PatternType.CHASE -> ChasePattern
            PatternType.PATROL -> PatrolPattern
            PatternType.MIRROR -> MirrorPattern
        }
}
