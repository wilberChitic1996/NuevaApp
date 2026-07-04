package gt.guardian.cadejo.domain.daily

/**
 * Derives the daily-challenge seed from a UTC date string (`YYYY-MM-DD`).
 *
 * Uses 64-bit FNV-1a — a tiny, well-specified hash — so the **exact same seed** is
 * produced on the client (Kotlin) and on the server (the TypeScript Edge Function
 * that re-validates scores). Everyone playing on a given day gets the identical
 * puzzle, and the backend can regenerate it from just the date.
 */
object DailySeed {
    private val FNV_OFFSET_BASIS: Long = 0xCBF29CE484222325uL.toLong()
    private const val FNV_PRIME: Long = 0x100000001B3L

    fun seedForDate(dateUtc: String): Long {
        var hash = FNV_OFFSET_BASIS
        for (byte in dateUtc.encodeToByteArray()) {
            hash = hash xor (byte.toLong() and 0xFF)
            hash *= FNV_PRIME
        }
        return hash
    }
}

/** Supplies today's date in UTC as `YYYY-MM-DD`. Implemented in :core:data (java.time). */
fun interface DateProvider {
    fun todayUtcIso(): String
}
