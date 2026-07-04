package gt.guardian.cadejo.feature.game

import app.cash.turbine.test
import gt.guardian.cadejo.domain.model.AbilityId
import gt.guardian.cadejo.domain.run.RunStatus
import gt.guardian.cadejo.domain.session.SeedSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameViewModelTest {

    // Fixed seed => deterministic starting run, so assertions are stable.
    private fun viewModel(seed: Long = 777L) = GameViewModel(SeedSource { seed })

    @Test
    fun `starts running on level one`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            val initial = awaitItem()
            assertEquals(RunStatus.RUNNING, initial.run.status)
            assertEquals(1, initial.run.levelIndex)
            assertEquals(0, initial.run.current.turn)
            assertFalse(initial.leapArming)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `a legal move advances the turn`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            val initial = awaitItem()
            val destination = initial.run.current.legalMoves().first()

            vm.onHexTap(destination)

            val next = awaitItem()
            assertEquals(destination, next.run.current.player)
            assertEquals(1, next.run.current.turn)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `arming leap highlights destinations and consumes the next tap`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            awaitItem() // initial

            vm.onToggleLeap()
            val armed = awaitItem()
            assertTrue(armed.leapArming)
            assertTrue(armed.highlight.isNotEmpty())

            val leapTarget = armed.highlight.first()
            vm.onHexTap(leapTarget)
            val afterLeap = awaitItem()
            assertFalse(afterLeap.leapArming)
            assertEquals(leapTarget, afterLeap.run.current.player)
            assertTrue(afterLeap.run.current.ability(AbilityId.LEAP)!!.remaining > 0)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `restart produces a fresh running run`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            awaitItem()
            vm.onWait()
            awaitItem()
            vm.onRestart()
            val restarted = awaitItem()
            assertEquals(RunStatus.RUNNING, restarted.run.status)
            assertEquals(0, restarted.run.current.turn)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `input is ignored once the run is over`() = runTest {
        val vm = viewModel()
        repeat(80) { if (!vm.uiState.value.run.isOver) vm.onWait() }
        val terminal = vm.uiState.value
        assertTrue("standing still should end the run", terminal.run.isOver)

        vm.onWait()
        assertEquals(terminal, vm.uiState.value)
    }
}
