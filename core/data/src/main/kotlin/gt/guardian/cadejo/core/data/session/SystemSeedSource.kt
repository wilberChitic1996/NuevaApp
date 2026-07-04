package gt.guardian.cadejo.core.data.session

import gt.guardian.cadejo.domain.session.SeedSource
import javax.inject.Inject

/**
 * The production [SeedSource]: derives a fresh seed from the system nanoseconds.
 *
 * This is deliberately the ONLY place in the run pipeline that reads the clock.
 * Keeping the non-determinism at this single, injectable edge means tests can
 * swap in a fixed seed and reproduce any run exactly.
 */
class SystemSeedSource @Inject constructor() : SeedSource {
    override fun newSeed(): Long = System.nanoTime()
}
