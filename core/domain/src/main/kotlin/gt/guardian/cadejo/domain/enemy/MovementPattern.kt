package gt.guardian.cadejo.domain.enemy

import gt.guardian.cadejo.domain.hex.Hex
import gt.guardian.cadejo.domain.model.Board
import gt.guardian.cadejo.domain.model.Enemy
import gt.guardian.cadejo.domain.model.GameState

/**
 * Strategy for deciding an enemy's next hex. Every implementation must be
 * deterministic: same [GameState] + same [Enemy] -> same result. Ties are broken
 * by [Hex.DIRECTIONS] order so a run never depends on iteration order or hashing.
 */
fun interface MovementPattern {
    /** The hex the enemy wants to move to; returns its current position to stay put. */
    fun nextMove(state: GameState, enemy: Enemy): Hex
}

/**
 * Chase: step one hex along the shortest walkable path toward the player.
 *
 * Implemented with a breadth-first search from the player outward (a single BFS
 * yields the distance field for the whole board), then the enemy picks the
 * adjacent hex with the smallest distance-to-player. BFS from the player rather
 * than the enemy means we could reuse the same field for many enemies later.
 */
object ChasePattern : MovementPattern {
    override fun nextMove(state: GameState, enemy: Enemy): Hex {
        val board = state.board
        val distances = bfsDistances(board, from = state.player, blocked = occupiedByOthers(state, enemy))

        // Candidate steps: adjacent walkable hexes not occupied by another enemy.
        val candidates = enemy.position.neighbors()
            .filter { board.isWalkable(it) && it !in occupiedByOthers(state, enemy) }

        // Choose the neighbour closest to the player. Unreachable hexes (no BFS
        // entry) are treated as +infinity so they're never preferred.
        val best = candidates.minByOrNull { distances[it] ?: Int.MAX_VALUE }
        // If no candidate makes progress (fully boxed in), stay put.
        val bestDist = best?.let { distances[it] ?: Int.MAX_VALUE } ?: Int.MAX_VALUE
        val hereDist = distances[enemy.position] ?: Int.MAX_VALUE
        return if (best != null && bestDist < hereDist) best else enemy.position
    }

    private fun occupiedByOthers(state: GameState, self: Enemy): Set<Hex> =
        state.enemies.asSequence().filter { it.id != self.id }.map { it.position }.toSet()

    /** BFS distance from [from] to every reachable walkable hex, respecting [blocked]. */
    private fun bfsDistances(board: Board, from: Hex, blocked: Set<Hex>): Map<Hex, Int> {
        val dist = HashMap<Hex, Int>()
        val queue = ArrayDeque<Hex>()
        dist[from] = 0
        queue.add(from)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val d = dist.getValue(current)
            for (next in current.neighbors()) {
                if (next in dist) continue
                if (!board.isWalkable(next)) continue
                if (next in blocked) continue
                dist[next] = d + 1
                queue.add(next)
            }
        }
        return dist
    }
}

/** Resolves a [gt.guardian.cadejo.domain.model.PatternType] to its strategy. */
object Patterns {
    fun of(type: gt.guardian.cadejo.domain.model.PatternType): MovementPattern = when (type) {
        gt.guardian.cadejo.domain.model.PatternType.CHASE -> ChasePattern
        // Phase 2 will supply real implementations; until then they chase.
        gt.guardian.cadejo.domain.model.PatternType.PATROL -> ChasePattern
        gt.guardian.cadejo.domain.model.PatternType.MIRROR -> ChasePattern
    }
}
