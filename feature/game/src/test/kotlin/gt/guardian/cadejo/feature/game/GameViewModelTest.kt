package gt.guardian.cadejo.feature.game

import app.cash.turbine.test
import gt.guardian.cadejo.domain.model.GameStatus
import gt.guardian.cadejo.domain.session.SeedSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameViewModelTest {

    // Fixed seed => deterministic starting board, so assertions are stable.
    private fun viewModel(seed: Long = 777L) =
        GameViewModel(SeedSource { seed })

    @Test
    fun `starts on a playing state at level one`() = runTest {
        val vm = viewModel()
        vm.state.test {
            val initial = awaitItem()
            assertEquals(GameStatus.PLAYING, initial.status)
            assertEquals(1, initial.level)
            assertEquals(0, initial.turn)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `a legal move emits a new state with an advanced turn`() = runTest {
        val vm = viewModel()
        vm.state.test {
            val initial = awaitItem()
            val destination = initial.legalMoves().first()

            vm.onHexTap(destination)

            val next = awaitItem()
            assertEquals(destination, next.player)
            assertEquals(1, next.turn)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `waiting advances the turn and moves enemies`() = runTest {
        val vm = viewModel()
        vm.state.test {
            val initial = awaitItem()
            val enemyBefore = initial.enemies.first().position

            vm.onWait()

            val next = awaitItem()
            assertEquals(1, next.turn)
            // The chaser should have closed distance (it isn't already adjacent-blocked).
            assertTrue(next.enemies.first().position != enemyBefore || next.isOver)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `restart produces a fresh playing state`() = runTest {
        val vm = viewModel()
        vm.state.test {
            awaitItem() // initial
            vm.onWait()
            awaitItem() // after wait

            vm.onRestart()
            val restarted = awaitItem()
            assertEquals(GameStatus.PLAYING, restarted.status)
            assertEquals(0, restarted.turn)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `taps are ignored once the game is over`() = runTest {
        // Same fixed seed each run: drive it to a terminal state by waiting, then
        // confirm further input is a no-op.
        val vm = viewModel()
        // Wait until the chaser catches a stationary Cadejo (bounded loop).
        repeat(30) { if (!vm.state.value.isOver) vm.onWait() }
        val terminal = vm.state.value
        assertTrue("expected the run to end when standing still", terminal.isOver)

        vm.onHexTap(terminal.legalMoves().firstOrNull() ?: terminal.player)
        assertEquals(terminal, vm.state.value)
    }
}
