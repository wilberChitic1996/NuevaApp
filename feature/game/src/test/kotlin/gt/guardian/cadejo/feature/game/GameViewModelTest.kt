package gt.guardian.cadejo.feature.game

import app.cash.turbine.test
import gt.guardian.cadejo.domain.model.AbilityId
import gt.guardian.cadejo.domain.monetization.RewardOutcome
import gt.guardian.cadejo.domain.monetization.RewardedAdService
import gt.guardian.cadejo.domain.progress.PlayerProfile
import gt.guardian.cadejo.domain.progress.ProgressRepository
import gt.guardian.cadejo.domain.progress.PurchaseResult
import gt.guardian.cadejo.domain.progress.RunRecord
import gt.guardian.cadejo.domain.progress.UnlockId
import gt.guardian.cadejo.domain.run.RunStatus
import gt.guardian.cadejo.domain.session.SeedSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GameViewModelTest {

    // viewModelScope dispatches on Main; back it with a test dispatcher in unit tests.
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    // A no-op progress repository so the ViewModel can be tested without persistence.
    private val fakeProgress = object : ProgressRepository {
        override val profile: Flow<PlayerProfile> = flowOf(PlayerProfile.INITIAL)
        override suspend fun awardRun(record: RunRecord): PlayerProfile = PlayerProfile.INITIAL
        override suspend fun addCoins(amount: Long): PlayerProfile = PlayerProfile.INITIAL
        override suspend fun purchase(id: UnlockId): PurchaseResult = PurchaseResult.AlreadyOwned
        override suspend fun selectSkin(id: UnlockId?) {}
        override suspend fun setAdsRemoved(removed: Boolean) {}
        override fun recentRuns(limit: Int): Flow<List<RunRecord>> = flowOf(emptyList())
    }

    private val fakeAds = object : RewardedAdService {
        override fun preload() {}
        override suspend fun showRewarded(): RewardOutcome = RewardOutcome.UNAVAILABLE
    }

    // Fixed seed => deterministic starting run, so assertions are stable.
    private fun viewModel(seed: Long = 777L) = GameViewModel(SeedSource { seed }, fakeProgress, fakeAds)

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
        // Walk the Cadejo into an enemy to force a deterministic loss.
        repeat(60) {
            val s = vm.uiState.value
            if (!s.run.isOver) {
                val enemy = s.run.current.enemies.first().position
                val step = s.run.current.legalMoves().minByOrNull { it.distanceTo(enemy) }
                if (step != null) vm.onHexTap(step) else vm.onWait()
            }
        }
        val terminal = vm.uiState.value
        assertTrue("chasing into an enemy should end the run", terminal.run.isOver)

        vm.onWait()
        assertEquals(terminal, vm.uiState.value)
    }
}
