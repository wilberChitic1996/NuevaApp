package gt.guardian.cadejo.domain.level

import gt.guardian.cadejo.domain.hex.Hex
import gt.guardian.cadejo.domain.model.Balance
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
 * identical [GameState]** — the property the daily mode and server validation rely
 * on. Difficulty scales with [levelIndex]: bigger boards, more walls, and more
 * (and nastier) spirits.
 *
 * The generator self-validates: it never emits a board whose goal is unreachable,
 * and it re-rolls enemy/traveler placement so nothing spawns on top of anything.
 */
object LevelGenerator {
    private const val MIN_RADIUS = 3
    private const val MAX_RADIUS = 6
    private const val MAX_ENEMIES = 4

    fun generate(
        seed: Long,
        levelIndex: Int,
        startingScore: Int = 0,
    ): GameState {
        val rng = SeededRng(seed = seed * 0x9E3779B1L + levelIndex)

        val radius = (MIN_RADIUS + levelIndex / 2).coerceIn(MIN_RADIUS, MAX_RADIUS)
        val allHexes = Hex.disc(Hex.ORIGIN, radius).sortedWith(hexOrder)
        val rim = allHexes.filter { it.distanceTo(Hex.ORIGIN) == radius }

        val player = rng.pick(rim) ?: Hex.ORIGIN
        val goal =
            rim
                .filter { it != player }
                .maxWithOrNull(compareBy({ it.distanceTo(player) }, { hexOrder.compare(it, player) }))
                ?: Hex.ORIGIN

        // The traveler starts adjacent to the Cadejo (behind it), never on the goal.
        val traveler =
            player
                .neighbors()
                .filter { it in allHexes && it != goal }
                .minByOrNull { it.distanceTo(goal) } // slightly ahead, toward the goal
                ?: player

        val reserved = mutableSetOf(player, goal, traveler)

        val walls = scatterWalls(rng, allHexes, reserved, levelIndex, radius, player, goal)

        val enemies = spawnEnemies(rng, allHexes, walls, reserved, traveler, levelIndex)

        val board = buildBoard(radius, walls, goal)

        return GameState(
            board = board,
            player = player,
            traveler = traveler,
            enemies = enemies,
            goal = goal,
            seed = seed,
            rngState = rng.state,
            level = levelIndex,
            abilities = Balance.defaultAbilities(),
            score = startingScore,
        )
    }

    private fun scatterWalls(
        rng: SeededRng,
        allHexes: List<Hex>,
        reserved: Set<Hex>,
        levelIndex: Int,
        radius: Int,
        player: Hex,
        goal: Hex,
    ): Set<Hex> {
        val budget = levelIndex.coerceAtMost(allHexes.size / 5)
        val walls = mutableSetOf<Hex>()
        val candidates = allHexes.filter { it !in reserved }
        var attempts = 0
        while (walls.size < budget && attempts < budget * 5) {
            attempts++
            val candidate = rng.pick(candidates) ?: break
            if (candidate in walls) continue
            val trial = walls + candidate
            if (isReachable(radius, trial, player, goal)) walls += candidate
        }
        return walls
    }

    private fun spawnEnemies(
        rng: SeededRng,
        allHexes: List<Hex>,
        walls: Set<Hex>,
        reserved: Set<Hex>,
        traveler: Hex,
        levelIndex: Int,
    ): List<Enemy> {
        val count = (1 + levelIndex / 3).coerceIn(1, MAX_ENEMIES)
        val taken = reserved.toMutableSet()
        val enemies = mutableListOf<Enemy>()
        repeat(count) { i ->
            val open =
                allHexes.filter {
                    it !in walls && it !in taken && it.distanceTo(traveler) >= 3
                }
            val start = rng.pick(open) ?: allHexes.firstOrNull { it !in walls && it !in taken } ?: return@repeat
            taken += start
            val (kind, pattern) = enemyArchetype(i, levelIndex)
            val route = if (pattern == PatternType.PATROL) patrolRoute(start, walls, allHexes) else emptyList()
            enemies +=
                Enemy(
                    id = "enemy-$i",
                    kind = kind,
                    position = start,
                    pattern = pattern,
                    patrolRoute = route,
                )
        }
        return enemies
    }

    /** Which spirit spawns in slot [i]; variety grows with the level. */
    private fun enemyArchetype(
        i: Int,
        levelIndex: Int,
    ): Pair<SpiritKind, PatternType> =
        when {
            i == 0 -> SpiritKind.CADEJO_NEGRO to PatternType.MIRROR
            i == 1 -> SpiritKind.LLORONA to PatternType.CHASE
            i == 2 -> SpiritKind.SOMBRERON to PatternType.PATROL
            else ->
                if (levelIndex % 2 == 0) {
                    SpiritKind.LLORONA to PatternType.CHASE
                } else {
                    SpiritKind.SOMBRERON to PatternType.PATROL
                }
        }

    /** A small local loop around [start] for patrolling spirits. */
    private fun patrolRoute(
        start: Hex,
        walls: Set<Hex>,
        onBoard: List<Hex>,
    ): List<Hex> {
        val ring = start.neighbors().filter { it in onBoard && it !in walls }
        return if (ring.size >= 2) listOf(start) + ring.take(3) else listOf(start)
    }

    private fun buildBoard(
        radius: Int,
        walls: Set<Hex>,
        goal: Hex,
    ): Board {
        val cells =
            Hex.disc(Hex.ORIGIN, radius).associateWith { hex ->
                val terrain =
                    when {
                        hex == goal -> Terrain.GOAL
                        hex in walls -> Terrain.WALL
                        else -> Terrain.FLOOR
                    }
                Cell(hex, terrain)
            }
        return Board(cells)
    }

    /** BFS reachability check used to reject walls that would seal off the goal. */
    private fun isReachable(
        radius: Int,
        walls: Set<Hex>,
        from: Hex,
        goal: Hex,
    ): Boolean {
        val onBoard = Hex.disc(Hex.ORIGIN, radius)
        val visited = HashSet<Hex>()
        val queue = ArrayDeque<Hex>()
        visited += from
        queue += from
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current == goal) return true
            for (next in current.neighbors()) {
                if (next in visited || next !in onBoard || next in walls) continue
                visited += next
                queue += next
            }
        }
        return false
    }

    private val hexOrder: Comparator<Hex> = compareBy({ it.q }, { it.r })
}
