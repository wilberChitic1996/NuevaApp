package gt.guardian.cadejo.domain.daily

import gt.guardian.cadejo.domain.hex.Hex
import gt.guardian.cadejo.domain.model.AbilityId
import gt.guardian.cadejo.domain.model.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DailyTest {

    @Test
    fun `seed is stable for a given date and varies by date`() {
        assertEquals(DailySeed.seedForDate("2026-07-04"), DailySeed.seedForDate("2026-07-04"))
        assertNotEquals(DailySeed.seedForDate("2026-07-04"), DailySeed.seedForDate("2026-07-05"))
    }

    @Test
    fun `intent codec round-trips every intent variant`() {
        val intents = listOf(
            Intent.Wait,
            Intent.Move(Hex(2, -3)),
            Intent.Move(Hex(-1, 0)),
            Intent.UseAbility(AbilityId.HOWL),
            Intent.UseAbility(AbilityId.PROTECTIVE_LIGHT),
            Intent.UseAbility(AbilityId.LEAP, Hex(-2, 1)),
        )
        val encoded = IntentCodec.encode(intents)
        assertEquals(intents, IntentCodec.decode(encoded))
    }

    @Test
    fun `empty tape round-trips`() {
        assertEquals(emptyList<Intent>(), IntentCodec.decode(IntentCodec.encode(emptyList())))
    }
}
