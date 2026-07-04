package gt.guardian.cadejo.domain.level

import gt.guardian.cadejo.domain.hex.Hex
import gt.guardian.cadejo.domain.model.GameState
import gt.guardian.cadejo.domain.model.GameStatus
import gt.guardian.cadejo.domain.model.Terrain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelGeneratorTest {

    @Test
    fun `same seed and level produce identical state`() {
        assertEquals(
            LevelGenerator.generate(12345L, 1),
            LevelGenerator.generate(12345L, 1),
        )
    }

    @Test
    fun `different seeds generally differ`() {
        assertNotEquals(
            LevelGenerator.generate(1L, 1),
            LevelGenerator.generate(2L, 1),
        )
    }

    @Test
    fun `carries the starting score forward`() {
        val state = LevelGenerator.generate(seed = 5L, levelIndex = 3, startingScore = 250)
        assertEquals(250, state.score)
    }

    @Test
    fun `generated level is internally consistent across levels`() {
        for (level in 1..10) {
            val s = LevelGenerator.generate(seed = 9001L, levelIndex = level)

            assertEquals("level index recorded", level, s.level)
            assertEquals("starts playing", GameStatus.PLAYING, s.status)
            assertTrue("player on walkable cell", s.board.isWalkable(s.player))
            assertTrue("traveler on walkable cell", s.board.isWalkable(s.traveler))
            assertEquals("goal marked GOAL", Terrain.GOAL, s.board.terrainAt(s.goal))
            assertTrue("has at least one enemy", s.enemies.isNotEmpty())
            assertTrue("has three abilities ready", s.abilities.size == 3 && s.abilities.all { it.isReady })

            val occupied = buildList { add(s.player); add(s.traveler); add(s.goal); addAll(s.enemies.map { it.position }) }
            assertEquals("no two pieces share a cell", occupied.size, occupied.toSet().size)
            s.enemies.forEach { assertTrue("enemy on walkable cell", s.board.isWalkable(it.position)) }
        }
    }

    @Test
    fun `harder levels never shrink the board and add enemies`() {
        val early = LevelGenerator.generate(seed = 42L, levelIndex = 1)
        val late = LevelGenerator.generate(seed = 42L, levelIndex = 9)
        assertTrue(late.board.cells.size >= early.board.cells.size)
        assertTrue(late.enemies.size >= early.enemies.size)
    }

    @Test
    fun `goal is always reachable from the player`() {
        for (seed in 0L until 150L) {
            val s = LevelGenerator.generate(seed = seed, levelIndex = 6)
            assertTrue("seed $seed unsolvable", reachable(s))
        }
    }

    private fun reachable(s: GameState): Boolean {
        val visited = HashSet<Hex>()
        val queue = ArrayDeque<Hex>()
        visited += s.player
        queue += s.player
        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            if (cur == s.goal) return true
            for (n in cur.neighbors()) {
                if (n in visited || !s.board.isWalkable(n)) continue
                visited += n
                queue += n
            }
        }
        return false
    }
}
