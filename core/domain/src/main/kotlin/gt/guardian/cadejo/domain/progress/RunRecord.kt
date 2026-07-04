package gt.guardian.cadejo.domain.progress

import gt.guardian.cadejo.domain.run.RunMode

enum class RunOutcome { COMPLETED, FAILED }

/** A finished run, stored in history and used to pay out coins and update stats. */
data class RunRecord(
    val seed: Long,
    val mode: RunMode,
    val reachedLevel: Int,
    val score: Int,
    val outcome: RunOutcome,
    val durationMs: Long = 0,
)
