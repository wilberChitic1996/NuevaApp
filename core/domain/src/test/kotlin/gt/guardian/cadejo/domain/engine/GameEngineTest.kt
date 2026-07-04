package gt.guardian.cadejo.domain.engine

import gt.guardian.cadejo.domain.hex.Hex
import gt.guardian.cadejo.domain.model.AbilityId
import gt.guardian.cadejo.domain.model.Board
import gt.guardian.cadejo.domain.model.Enemy
import gt.guardian.cadejo.domain.model.GameState
import gt.guardian.cadejo.domain.model.GameStatus
import gt.guardian.cadejo.domain.model.Intent
import gt.guardian.cadejo.domain.model.PatternType
import gt.guardian.cadejo.domain.model.SpiritKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {
    private fun baseState(
        player: Hex = Hex(0, 0),
        traveler: Hex = Hex(-1, 0),
        goal: Hex = Hex(3, 0),
        enemies: List<Enemy> = emptyList(),
    ) = GameState(
        board = Board.hexagon(4),
        player = player,
        traveler = traveler,
        enemies = enemies,
        goal = goal,
        seed = 1,
        rngState = 0,
    )

    private fun enemy(
        pos: Hex,
        pattern: PatternType = PatternType.CHASE,
    ) =
        Enemy("e", SpiritKind.LLORONA, pos, pattern)

    @Test
    fun `legal move relocates the player and advances the turn`() {
        val next = GameEngine.reduce(baseState(), Intent.Move(Hex(1, 0)))
        assertEquals(Hex(1, 0), next.player)
        assertEquals(1, next.turn)
        assertEquals(GameStatus.PLAYING, next.status)
    }

    @Test
    fun `non-adjacent move is a no-op that does not spend a turn`() {
        val state = baseState()
        val next = GameEngine.reduce(state, Intent.Move(Hex(3, 0)))
        assertSame(state, next)
    }

    @Test
    fun `moving onto the traveler is illegal`() {
        val state = baseState(player = Hex(0, 0), traveler = Hex(1, 0))
        val next = GameEngine.reduce(state, Intent.Move(Hex(1, 0)))
        assertSame(state, next)
    }

    @Test
    fun `reaching the goal wins and awards score`() {
        val state = baseState(player = Hex(2, 0), goal = Hex(3, 0))
        val next = GameEngine.reduce(state, Intent.Move(Hex(3, 0)))
        assertEquals(GameStatus.WON, next.status)
        assertTrue("score should be positive after a win", next.score > 0)
    }

    @Test
    fun `game over is a fixed point`() {
        val won = baseState().copy(status = GameStatus.WON)
        assertSame(won, GameEngine.reduce(won, Intent.Move(Hex(1, 0))))
    }

    @Test
    fun `stepping onto an enemy loses`() {
        val state = baseState(enemies = listOf(enemy(Hex(1, 0))))
        val next = GameEngine.reduce(state, Intent.Move(Hex(1, 0)))
        assertEquals(GameStatus.LOST, next.status)
    }

    @Test
    fun `the traveler trails toward the player`() {
        val state = baseState(player = Hex(0, 0), traveler = Hex(-3, 0), goal = Hex(3, 0))
        val next = GameEngine.reduce(state, Intent.Move(Hex(1, 0)))
        assertTrue(
            "traveler should close distance to the player",
            next.traveler.distanceTo(next.player) < Hex(-3, 0).distanceTo(Hex(1, 0)),
        )
    }

    @Test
    fun `howl stuns nearby enemies which then hold position`() {
        val e = enemy(Hex(2, 0)) // distance 2 from player at origin
        val state = baseState(player = Hex(0, 0), traveler = Hex(-1, 0), goal = Hex(-4, 0), enemies = listOf(e))
        val next = GameEngine.reduce(state, Intent.UseAbility(AbilityId.HOWL))

        val stunned = next.enemies.first()
        assertEquals("stunned enemy holds position", Hex(2, 0), stunned.position)
        assertTrue("stun timer is set", stunned.stunnedTurns >= 1)
        assertTrue("howl went on cooldown", next.ability(AbilityId.HOWL)!!.remaining > 0)
    }

    @Test
    fun `leap moves within range and goes on cooldown`() {
        val state = baseState(player = Hex(0, 0), traveler = Hex(-1, 0), goal = Hex(4, 0))
        val target = Hex(2, 0) // distance 2 — within LEAP_RANGE
        val next = GameEngine.reduce(state, Intent.UseAbility(AbilityId.LEAP, target))
        assertEquals(target, next.player)
        assertTrue(next.ability(AbilityId.LEAP)!!.remaining > 0)
    }

    @Test
    fun `leap beyond range is rejected`() {
        val state = baseState(player = Hex(0, 0), goal = Hex(4, 0))
        val next = GameEngine.reduce(state, Intent.UseAbility(AbilityId.LEAP, Hex(3, 0)))
        assertSame(state, next)
    }

    @Test
    fun `protective light shields the traveler`() {
        val state = baseState()
        val next = GameEngine.reduce(state, Intent.UseAbility(AbilityId.PROTECTIVE_LIGHT))
        assertTrue("shield is active", next.travelerShield > 0)
        assertTrue("light went on cooldown", next.ability(AbilityId.PROTECTIVE_LIGHT)!!.remaining > 0)
    }

    @Test
    fun `an ability on cooldown is a no-op`() {
        val cooling =
            baseState().let { s ->
                s.copy(abilities = s.abilities.map { if (it.id == AbilityId.HOWL) it.triggered() else it })
            }
        val next = GameEngine.reduce(cooling, Intent.UseAbility(AbilityId.HOWL))
        assertSame(cooling, next)
    }

    @Test
    fun `a fixed sequence of intents is deterministic`() {
        val start =
            baseState(
                player = Hex(0, 0),
                traveler = Hex(-1, 0),
                goal = Hex(-4, 0),
                enemies = listOf(enemy(Hex(3, 0), PatternType.CHASE)),
            )
        val intents =
            listOf(
                Intent.Move(Hex(-1, 1)),
                Intent.UseAbility(AbilityId.HOWL),
                Intent.Wait,
                Intent.Move(Hex(-2, 1)),
            )
        val a = intents.fold(start) { s, i -> GameEngine.reduce(s, i) }
        val b = intents.fold(start) { s, i -> GameEngine.reduce(s, i) }
        assertEquals(a, b)
    }

    @Test
    fun `a passive escort is eventually caught`() {
        var s =
            GameState(
                board = Board.hexagon(3),
                player = Hex(0, 0),
                traveler = Hex(1, 0),
                enemies = listOf(enemy(Hex(2, 0), PatternType.CHASE)),
                goal = Hex(-3, 0),
                seed = 1,
                rngState = 0,
            )
        repeat(40) { if (!s.isOver) s = GameEngine.reduce(s, Intent.Wait) }
        assertEquals(GameStatus.LOST, s.status)
    }
}
