package gt.guardian.cadejo.domain.enemy

import gt.guardian.cadejo.domain.hex.Hex
import gt.guardian.cadejo.domain.model.Board
import gt.guardian.cadejo.domain.model.Enemy
import gt.guardian.cadejo.domain.model.GameState
import gt.guardian.cadejo.domain.model.PatternType
import gt.guardian.cadejo.domain.model.SpiritKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternsTest {

    private fun state(lastStep: Hex? = null, enemy: Enemy) = GameState(
        board = Board.hexagon(4),
        player = Hex(0, 0),
        traveler = Hex(0, 1),
        enemies = listOf(enemy),
        goal = Hex(3, 0),
        seed = 0,
        rngState = 0,
        lastPlayerStep = lastStep,
    )

    @Test
    fun `mirror repeats the player's last step direction`() {
        val e = Enemy("m", SpiritKind.CADEJO_NEGRO, Hex(0, -2), PatternType.MIRROR)
        val step = Hex(1, 0) // player moved east
        val moved = MirrorPattern.step(state(lastStep = step, enemy = e), e, target = Hex(0, 1), blocked = emptySet())
        assertEquals(Hex(0, -2) + step, moved.position)
    }

    @Test
    fun `mirror falls back to chasing when the player waited`() {
        val e = Enemy("m", SpiritKind.CADEJO_NEGRO, Hex(3, 0), PatternType.MIRROR)
        val target = Hex(0, 1)
        val moved = MirrorPattern.step(state(lastStep = null, enemy = e), e, target = target, blocked = emptySet())
        assertEquals(1, e.position.distanceTo(moved.position)) // it moved
        assertTrue(moved.position.distanceTo(target) < e.position.distanceTo(target)) // toward target
    }

    @Test
    fun `patrol advances toward its waypoint and cycles the index`() {
        val route = listOf(Hex(0, -2), Hex(2, -2), Hex(2, 0))
        val e = Enemy("p", SpiritKind.SOMBRERON, Hex(0, -2), PatternType.PATROL, patrolRoute = route, patrolIndex = 0)
        // At waypoint 0 -> should target waypoint 1 and advance the index.
        val moved = PatrolPattern.step(state(enemy = e), e, target = Hex(0, 1), blocked = emptySet())
        assertEquals(1, moved.patrolIndex)
        assertTrue("moved off the waypoint toward the next", moved.position != e.position)
    }

    @Test
    fun `patrol without a route degenerates to chase`() {
        val e = Enemy("p", SpiritKind.SOMBRERON, Hex(3, 0), PatternType.PATROL)
        val target = Hex(0, 1)
        val moved = PatrolPattern.step(state(enemy = e), e, target = target, blocked = emptySet())
        assertTrue(moved.position.distanceTo(target) < e.position.distanceTo(target))
    }
}
