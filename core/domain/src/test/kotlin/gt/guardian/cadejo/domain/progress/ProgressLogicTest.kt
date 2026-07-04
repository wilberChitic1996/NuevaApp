package gt.guardian.cadejo.domain.progress

import gt.guardian.cadejo.domain.run.RunMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressLogicTest {

    private fun record(score: Int, level: Int) =
        RunRecord(seed = 1, mode = RunMode.ROGUE, reachedLevel = level, score = score, outcome = RunOutcome.FAILED)

    @Test
    fun `award adds deterministic coins and updates stats`() {
        val start = PlayerProfile(coins = 100)
        val after = ProgressLogic.award(start, record(score = 350, level = 4))
        // 350/10 + 4*5 = 35 + 20 = 55
        assertEquals(155, after.coins)
        assertEquals(1, after.totalRuns)
        assertEquals(350, after.bestScore)
    }

    @Test
    fun `best score only increases`() {
        val start = PlayerProfile(bestScore = 500)
        val after = ProgressLogic.award(start, record(score = 300, level = 2))
        assertEquals(500, after.bestScore)
    }

    @Test
    fun `purchase succeeds when affordable and not owned`() {
        val start = PlayerProfile(coins = 1000)
        val result = ProgressLogic.purchase(start, UnlockId.SKIN_JADE)
        assertTrue(result is PurchaseResult.Success)
        val profile = (result as PurchaseResult.Success).profile
        assertEquals(1000L - UnlockId.SKIN_JADE.cost, profile.coins)
        assertTrue(profile.owns(UnlockId.SKIN_JADE))
        assertEquals(UnlockId.SKIN_JADE, profile.selectedSkin) // auto-equipped
    }

    @Test
    fun `purchase rejects when already owned`() {
        val start = PlayerProfile(coins = 1000, unlocks = setOf(UnlockId.SKIN_JADE))
        assertEquals(PurchaseResult.AlreadyOwned, ProgressLogic.purchase(start, UnlockId.SKIN_JADE))
    }

    @Test
    fun `purchase rejects when too poor`() {
        val start = PlayerProfile(coins = 10)
        assertEquals(PurchaseResult.InsufficientCoins, ProgressLogic.purchase(start, UnlockId.MODIFIER_SWIFT))
    }

    @Test
    fun `select skin requires ownership`() {
        val notOwned = ProgressLogic.selectSkin(PlayerProfile(), UnlockId.SKIN_DAWN)
        assertEquals(null, notOwned.selectedSkin)

        val owned = PlayerProfile(unlocks = setOf(UnlockId.SKIN_DAWN))
        assertEquals(UnlockId.SKIN_DAWN, ProgressLogic.selectSkin(owned, UnlockId.SKIN_DAWN).selectedSkin)
    }
}
