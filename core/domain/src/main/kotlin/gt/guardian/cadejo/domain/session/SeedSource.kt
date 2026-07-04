package gt.guardian.cadejo.domain.session

/**
 * Supplies the seed for a brand-new run.
 *
 * This is the boundary between the pure, deterministic engine and the outside
 * world. Randomness for *starting* a run is genuinely non-deterministic (we want
 * a different run each time), so it lives behind this interface and its
 * implementation sits in :core:data. Everything downstream of the seed — level
 * generation, enemy moves, scoring — is fully deterministic from that one value,
 * which is what makes runs replayable and server-verifiable.
 */
fun interface SeedSource {
    fun newSeed(): Long
}
