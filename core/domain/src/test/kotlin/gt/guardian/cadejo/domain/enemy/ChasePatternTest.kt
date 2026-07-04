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

    private fun stateWith(player: Hex, enemy: Hex, board: Board): GameState =
        GameState(
            board = board,
            player = player,
            enemies = listOf(Enemy("e", SpiritKind.LLORONA, enemy, PatternType.CHASE)),
            goal = Hex(0, 0),
            seed = 0,
            rngState = 0,
        )

    @Test
    fun `chaser steps one closer on open board`() {
        val board = Board.hexagon(3)
        val player = Hex(0, 0)
        val enemy = Hex(3, 0)
        val state = stateWith(player, enemy, board)

        val next = ChasePattern.nextMove(state, state.enemies.first())

        assertEquals(1, enemy.distanceTo(next)) // moved exactly one step
        assertTrue(next.distanceTo(player) < enemy.distanceTo(player)) // and got closer
        assertEquals(2, next.distanceTo(player))
    }

    @Test
    fun `chaser routes around a wall instead of through it`() {
        // Build a board where a wall sits directly between enemy and player.
        val base = Board.hexagon(3).cells.toMutableMap()
        val wall = Hex(1, 0)
        base[wall] = Cell(wall, Terrain.WALL)
        val board = Board(base)

        val player = Hex(0, 0)
        val enemy = Hex(2, 0)
        val state = stateWith(player, enemy, board)

        val next = ChasePattern.nextMove(state, state.enemies.first())

        assertTrue("must not step onto the wall", next != wall)
        assertTrue("must stay on the board", board.isWalkable(next))
        assertTrue("must actually move (route around the wall)", next != enemy)
        // Note: straight-line hex distance need NOT drop when routing around an
        // obstacle — the meaningful progress is along the *path*. Here (2,0)->(2,-1)
        // keeps hex distance at 2 but shortens the walkable path from 3 to 2.
        assertEquals(1, enemy.distanceTo(next)) // exactly one step
    }

    @Test
    fun `boxed-in chaser stays put`() {
        // Enemy fully surrounded by walls has nowhere to make progress.
        val enemy = Hex(0, 0)
        val cells = mutableMapOf(enemy to Cell(enemy, Terrain.FLOOR))
        enemy.neighbors().forEach { cells[it] = Cell(it, Terrain.WALL) }
        val board = Board(cells)
        val state = stateWith(Hex(3, 0), enemy, board)

        val next = ChasePattern.nextMove(state, state.enemies.first())
        assertEquals(enemy, next)
    }

    @Test
    fun `chase decision is deterministic`() {
        val board = Board.hexagon(4)
        val state = stateWith(Hex(0, 0), Hex(4, -2), board)
        val a = ChasePattern.nextMove(state, state.enemies.first())
        val b = ChasePattern.nextMove(state, state.enemies.first())
        assertEquals(a, b)
    }
}
