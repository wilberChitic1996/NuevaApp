package gt.guardian.cadejo.domain.level

import gt.guardian.cadejo.domain.hex.Hex
import gt.guardian.cadejo.domain.model.Board
import gt.guardian.cadejo.domain.model.Cell
import gt.guardian.cadejo.domain.model.Enemy
import gt.guardian.cadejo.domain.model.GameState
import gt.guardian.cadejo.domain.model.PatternType
import gt.guardian.cadejo.domain.model.SpiritKind
import gt.guardian.cadejo.domain.model.Terrain
import gt.guardian.cadejo.domain.random.SeededRng

/**
 * Builds a level purely from a seed. **Same `(seed, levelIndex)` -> byte-for-byte
 * identical [GameState]** — the property the tests pin down and the daily mode +
 * server validation rely on.
 *
 * Phase 1 scope: a filled-hexagon board (radius grows slightly with the level),
 * the Cadejo starting on one rim, the goal on the far rim, and a single chasing
 * enemy placed roughly midway. A handful of walls are scattered deterministically,
 * but only if they leave the goal reachable — the generator self-validates so it
 * can never emit an unsolvable board.
 */
object LevelGenerator {

    private const val MIN_RADIUS = 3
    private const val MAX_RADIUS = 6

    fun generate(seed: Long, levelIndex: Int): GameState {
        // Mixing the level into the seed makes each level of a run distinct while
        // keeping the whole run reproducible from the single run seed.
        val rng = SeededRng(seed = seed * 0x9E3779B1L + levelIndex)

        val radius = (MIN_RADIUS + levelIndex / 3).coerceAtMost(MAX_RADIUS)
        val allHexes = Hex.disc(Hex.ORIGIN, radius).sortedWith(hexOrder)
        val rim = allHexes.filter { it.distanceTo(Hex.ORIGIN) == radius }

        val player = rng.pick(rim) ?: Hex.ORIGIN
        // Goal: the rim hex farthest from the player (deterministic tie-break by order).
        val goal = rim.filter { it != player }
            .maxWithOrNull(compareBy({ it.distanceTo(player) }, { hexOrder.compare(it, player) }))
            ?: Hex.ORIGIN

        val reserved = mutableSetOf(player, goal)

        // Scatter walls (more with higher levels), keeping player/goal clear and
        // re-checking that the goal stays reachable from the player.
        val wallBudget = (levelIndex).coerceAtMost(allHexes.size / 6)
        val walls = mutableSetOf<Hex>()
        val wallCandidates = allHexes.filter { it !in reserved }
        var attempts = 0
        while (walls.size < wallBudget && attempts < wallBudget * 4) {
            attempts++
            val candidate = rng.pick(wallCandidates) ?: break
            if (candidate in walls) continue
            val trial = walls + candidate
            if (isGoalReachable(radius, trial, player, goal)) {
                walls += candidate
            }
        }

        // Enemy: a floor hex away from player and goal, biased toward the middle.
        val enemyCandidates = allHexes.filter {
            it !in reserved && it !in walls && it.distanceTo(player) >= 2
        }
        val enemyStart = rng.pick(enemyCandidates) ?: goal
        val enemy = Enemy(
            id = "enemy-0",
            kind = SpiritKind.CADEJO_NEGRO,
            position = enemyStart,
            pattern = PatternType.CHASE,
        )

        val board = buildBoard(radius, walls, goal)

        return GameState(
            board = board,
            player = player,
            enemies = listOf(enemy),
            goal = goal,
            seed = seed,
            rngState = rng.state,
            level = levelIndex,
        )
    }

    private fun buildBoard(radius: Int, walls: Set<Hex>, goal: Hex): Board {
        val cells = Hex.disc(Hex.ORIGIN, radius).associateWith { hex ->
            val terrain = when {
                hex == goal -> Terrain.GOAL
                hex in walls -> Terrain.WALL
                else -> Terrain.FLOOR
            }
            Cell(hex, terrain)
        }
        return Board(cells)
    }

    /** BFS reachability check used to reject walls that would seal off the goal. */
    private fun isGoalReachable(radius: Int, walls: Set<Hex>, from: Hex, goal: Hex): Boolean {
        val onBoard = Hex.disc(Hex.ORIGIN, radius)
        val visited = HashSet<Hex>()
        val queue = ArrayDeque<Hex>()
        visited += from
        queue += from
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current == goal) return true
            for (next in current.neighbors()) {
                if (next in visited) continue
                if (next !in onBoard) continue
                if (next in walls) continue
                visited += next
                queue += next
            }
        }
        return false
    }

    /** Total order on hexes so all iteration is deterministic regardless of hashing. */
    private val hexOrder: Comparator<Hex> = compareBy({ it.q }, { it.r })
}
