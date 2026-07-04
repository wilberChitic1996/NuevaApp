package gt.guardian.cadejo.core.data.daily

import gt.guardian.cadejo.domain.daily.DateProvider
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

/**
 * Today's date in UTC as ISO `YYYY-MM-DD`. UTC (not local time) so the daily puzzle
 * flips at the same instant worldwide and the seed matches the server's. minSdk 26
 * gives us java.time without desugaring.
 */
class DailyDateProvider @Inject constructor() : DateProvider {
    override fun todayUtcIso(): String = LocalDate.now(ZoneOffset.UTC).toString()
}
