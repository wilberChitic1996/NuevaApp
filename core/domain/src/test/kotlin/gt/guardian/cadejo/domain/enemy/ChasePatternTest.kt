package gt.guardian.cadejo.domain.enemy

import gt.guardian.cadejo.domain.hex.Hex
import gt.guardian.cadejo.domain.model.Board
import gt.guardian.cadejo.domain.model.Cell
import gt.guardian.cadejo.domain.model.Enemy
import gt.guardian.cadejo.domain.model.GameState
import gt.guardian.cadejo.domain.model.PatternType
import gt.guardian.cadejo.domain.model.SpiritKind
import gt.guardian.cadejo.domain.model.Terrain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChasePatternTest {
    private fun stateWith(
        traveler: Hex,
        enemy: Hex,
        board: Board,
    ): GameState =
        GameState(
            board = board,
            player = Hex(0, 0),
            traveler = traveler,
            enemies = listOf(Enemy("e", SpiritKind.LLORONA, enemy, PatternType.CHASE)),
            goal = Hex(0, 0),
            seed = 0,
            rngState = 0,
        )

    @Test
    fun `chaser steps one closer to the target on open board`() {
        val board = Board.hexagon(3)
        val target = Hex(0, 0)
        val enemy = Hex(3, 0)
        val state = stateWith(traveler = target, enemy = enemy, board = board)

        val moved = ChasePattern.step(state, state.enemies.first(), target, blocked = emptySet())

        assertEquals(1, enemy.distanceTo(moved.position))
        assertEquals(2, moved.position.distanceTo(target))
    }

    @Test
    fun `chaser routes around a wall`() {
        val base = Board.hexagon(3).cells.toMutableMap()
        val wall = Hex(1, 0)
        base[wall] = Cell(wall, Terrain.WALL)
        val board = Board(base)

        val target = Hex(0, 0)
        val enemy = Hex(2, 0)
        val state = stateWith(target, enemy, board)

        val moved = ChasePattern.step(state, state.enemies.first(), target, blocked = emptySet())

        assertTrue(moved.position != wall)
        assertTrue(board.isWalkable(moved.position))
        assertEquals(1, enemy.distanceTo(moved.position))
    }

    @Test
    fun `blocked cells are respected`() {
        val board = Board.hexagon(3)
        val target = Hex(0, 0)
        val enemy = Hex(2, 0)
        val state = stateWith(target, enemy, board)

        // Block the natural next step; the chaser must pick a different neighbour.
        val naturalStep = ChasePattern.step(state, state.enemies.first(), target, emptySet()).position
        val moved = ChasePattern.step(state, state.enemies.first(), target, blocked = setOf(naturalStep))

        assertTrue(moved.position != naturalStep)
        assertEquals(1, enemy.distanceTo(moved.position))
    }

    @Test
    fun `decision is deterministic`() {
        val board = Board.hexagon(4)
        val state = stateWith(Hex(0, 0), Hex(4, -2), board)
        val a = ChasePattern.step(state, state.enemies.first(), Hex(0, 0), emptySet())
        val b = ChasePattern.step(state, state.enemies.first(), Hex(0, 0), emptySet())
        assertEquals(a, b)
    }
}
