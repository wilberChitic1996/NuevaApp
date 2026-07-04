package gt.guardian.cadejo.domain.engine

import gt.guardian.cadejo.domain.hex.Hex
import gt.guardian.cadejo.domain.model.Board
import gt.guardian.cadejo.domain.model.Enemy
import gt.guardian.cadejo.domain.model.GameState
import gt.guardian.cadejo.domain.model.GameStatus
import gt.guardian.cadejo.domain.model.Intent
import gt.guardian.cadejo.domain.model.PatternType
import gt.guardian.cadejo.domain.model.SpiritKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Test

class GameEngineTest {

    private fun baseState(
        player: Hex = Hex(0, 0),
        goal: Hex = Hex(3, 0),
        enemies: List<Enemy> = emptyList(),
    ) = GameState(
        board = Board.hexagon(3),
        player = player,
        enemies = enemies,
        goal = goal,
        seed = 1,
        rngState = 0,
    )

    @Test
    fun `legal move relocates the player and advances the turn`() {
        val state = baseState()
        val target = Hex(1, 0)
        val next = GameEngine.reduce(state, Intent.Move(target))

        assertEquals(target, next.player)
        assertEquals(1, next.turn)
        assertEquals(GameStatus.PLAYING, next.status)
    }

    @Test
    fun `illegal move to a non-adjacent hex is ignored`() {
        val state = baseState()
        val next = GameEngine.reduce(state, Intent.Move(Hex(3, 0)))
        // Player unchanged; with no enemies the state is effectively identical.
        assertEquals(state.player, next.player)
    }

    @Test
    fun `move off the board is ignored`() {
        val state = baseState(player = Hex(3, 0)) // on the rim of a radius-3 board
        val offBoard = Hex(4, 0)
        val next = GameEngine.reduce(state, Intent.Move(offBoard))
        assertEquals(Hex(3, 0), next.player)
    }

    @Test
    fun `reaching the goal wins`() {
        val state = baseState(player = Hex(2, 0), goal = Hex(3, 0))
        val next = GameEngine.reduce(state, Intent.Move(Hex(3, 0)))
        assertEquals(GameStatus.WON, next.status)
    }

    @Test
    fun `game over state is a fixed point`() {
        val won = baseState().copy(status = GameStatus.WON)
        val next = GameEngine.reduce(won, Intent.Move(Hex(1, 0)))
        assertSame(won, next)
    }

    @Test
    fun `stepping onto an enemy loses`() {
        val enemy = Enemy("e", SpiritKind.CADEJO_NEGRO, Hex(1, 0), PatternType.CHASE)
        val state = baseState(player = Hex(0, 0), enemies = listOf(enemy))
        val next = GameEngine.reduce(state, Intent.Move(Hex(1, 0)))
        assertEquals(GameStatus.LOST, next.status)
    }

    @Test
    fun `enemy catching the player after its move loses`() {
        // Enemy adjacent to where the player waits; on the enemy's turn it steps in.
        val enemy = Enemy("e", SpiritKind.LLORONA, Hex(2, 0), PatternType.CHASE)
        val state = baseState(player = Hex(0, 0), goal = Hex(-3, 1), enemies = listOf(enemy))

        // Player waits; enemy moves from (2,0) -> (1,0), still not adjacent-capture.
        val afterWait = GameEngine.reduce(state, Intent.Wait)
        assertEquals(GameStatus.PLAYING, afterWait.status)
        assertEquals(1, afterWait.enemies.first().position.distanceTo(Hex(0, 0)))

        // Player waits again; enemy steps onto the player.
        val afterSecond = GameEngine.reduce(afterWait, Intent.Wait)
        assertEquals(GameStatus.LOST, afterSecond.status)
    }

    @Test
    fun `stunned enemy does not move and its timer decrements`() {
        val enemy = Enemy("e", SpiritKind.SOMBRERON, Hex(2, 0), PatternType.CHASE, stunnedTurns = 1)
        val state = baseState(player = Hex(0, 0), goal = Hex(-3, 0), enemies = listOf(enemy))
        val next = GameEngine.reduce(state, Intent.Wait)

        val movedEnemy = next.enemies.first()
        assertEquals(Hex(2, 0), movedEnemy.position) // stayed put
        assertEquals(0, movedEnemy.stunnedTurns)     // timer ticked down
    }

    @Test
    fun `a fixed sequence of intents is fully deterministic`() {
        val start = baseState(player = Hex(0, 0), goal = Hex(-3, 0), enemies = listOf(
            Enemy("e", SpiritKind.CADEJO_NEGRO, Hex(3, 0), PatternType.CHASE),
        ))
        val intents = listOf(
            Intent.Move(Hex(-1, 0)),
            Intent.Move(Hex(-1, 0)),
            Intent.Wait,
            Intent.Move(Hex(-1, 1)),
        )
        val runA = intents.fold(start) { s, i -> GameEngine.reduce(s, i) }
        val runB = intents.fold(start) { s, i -> GameEngine.reduce(s, i) }
        assertEquals(runA, runB)
    }

    @Test
    fun `enemy actually advances toward a waiting player`() {
        val enemy = Enemy("e", SpiritKind.LLORONA, Hex(3, 0), PatternType.CHASE)
        val state = baseState(player = Hex(0, 0), goal = Hex(-3, 0), enemies = listOf(enemy))
        val next = GameEngine.reduce(state, Intent.Wait)
        assertNotEquals(Hex(3, 0), next.enemies.first().position)
        assertEquals(2, next.enemies.first().position.distanceTo(Hex(0, 0)))
    }
}
