package gt.guardian.cadejo.domain.level

import gt.guardian.cadejo.domain.model.GameStatus
import gt.guardian.cadejo.domain.model.Terrain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelGeneratorTest {

    @Test
    fun `same seed and level produce identical state`() {
        val a = LevelGenerator.generate(seed = 12345L, levelIndex = 1)
        val b = LevelGenerator.generate(seed = 12345L, levelIndex = 1)
        assertEquals(a, b)
    }

    @Test
    fun `different seeds generally produce different states`() {
        val a = LevelGenerator.generate(seed = 1L, levelIndex = 1)
        val b = LevelGenerator.generate(seed = 2L, levelIndex = 1)
        assertNotEquals(a, b)
    }

    @Test
    fun `different levels of the same run differ`() {
        val l1 = LevelGenerator.generate(seed = 777L, levelIndex = 1)
        val l2 = LevelGenerator.generate(seed = 777L, levelIndex = 2)
        assertNotEquals(l1, l2)
    }

    @Test
    fun `generated level is internally consistent`() {
        for (level in 1..10) {
            val state = LevelGenerator.generate(seed = 9001L, levelIndex = level)

            assertEquals("level index is recorded", level, state.level)
            assertEquals("starts playing", GameStatus.PLAYING, state.status)
            assertTrue("player is on a walkable cell", state.board.isWalkable(state.player))
            assertEquals("goal cell is marked GOAL", Terrain.GOAL, state.board.terrainAt(state.goal))
            assertTrue("exactly one enemy in phase 1", state.enemies.size == 1)

            val enemy = state.enemies.first()
            assertTrue("enemy is on a walkable cell", state.board.isWalkable(enemy.position))
            assertNotEquals("enemy does not start on the player", state.player, enemy.position)
            assertNotEquals("player and goal are distinct", state.player, state.goal)
        }
    }

    @Test
    fun `goal is always reachable from the player`() {
        // The generator promises solvable boards; verify with an independent BFS.
        for (seed in 0L until 200L) {
            val state = LevelGenerator.generate(seed = seed, levelIndex = 5)
            assertTrue("seed $seed produced an unsolvable board", reachable(state))
        }
    }

    private fun reachable(state: gt.guardian.cadejo.domain.model.GameState): Boolean {
        val visited = HashSet<gt.guardian.cadejo.domain.hex.Hex>()
        val queue = ArrayDeque<gt.guardian.cadejo.domain.hex.Hex>()
        visited += state.player
        queue += state.player
        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            if (cur == state.goal) return true
            for (n in cur.neighbors()) {
                if (n in visited) continue
                if (!state.board.isWalkable(n)) continue
                visited += n
                queue += n
            }
        }
        return false
    }
}
