package gt.guardian.cadejo.domain.random

/**
 * A tiny, fully deterministic PRNG (SplitMix64).
 *
 * We deliberately do NOT use [kotlin.random.Random] or anything backed by the
 * system clock. Determinism is a hard requirement of this game:
 *  - procedural levels must be reproducible from a seed,
 *  - the daily puzzle must be identical for every player,
 *  - the backend must be able to re-run a submitted run to validate its score.
 *
 * The generator's whole state is a single [Long]. That [state] is carried inside
 * [gt.guardian.cadejo.domain.model.GameState] so the engine stays a pure function:
 * feed the same state in, get the same numbers out.
 */
class SeededRng(seed: Long) {

    /** The full mutable state. Snapshot it into GameState to preserve determinism. */
    var state: Long = seed
        private set

    fun nextLong(): Long {
        state += GOLDEN_GAMMA
        var z = state
        z = (z xor (z ushr 30)) * MIX_1
        z = (z xor (z ushr 27)) * MIX_2
        return z xor (z ushr 31)
    }

    /** Uniform-ish int in `[0, bound)`. */
    fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be positive, was $bound" }
        return ((nextLong() ushr 1) % bound).toInt()
    }

    /** Int in the inclusive range `[min, max]`. */
    fun nextIntInRange(min: Int, max: Int): Int {
        require(max >= min) { "max ($max) must be >= min ($min)" }
        return min + nextInt(max - min + 1)
    }

    fun nextBoolean(): Boolean = nextLong() and 1L == 1L

    /** Returns a random element of [list], or null if empty. */
    fun <T> pick(list: List<T>): T? = if (list.isEmpty()) null else list[nextInt(list.size)]

    private companion object {
        // Constants from the reference SplitMix64. Written as unsigned literals
        // and reinterpreted to Long so the high bit is set correctly.
        const val GOLDEN_GAMMA: Long = -0x61c8864680b583ebL // 0x9E3779B97F4A7C15
        const val MIX_1: Long = -0x40a7b892e31b1a47L        // 0xBF58476D1CE4E5B9
        const val MIX_2: Long = -0x6b2fb644ecceee15L        // 0x94D049BB133111EB
    }
}
