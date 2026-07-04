package gt.guardian.cadejo.domain.run

import gt.guardian.cadejo.domain.model.GameState
import gt.guardian.cadejo.domain.model.Intent

/** Overall outcome of a 10-level run. */
enum class RunStatus { RUNNING, COMPLETED, FAILED }

/**
 * A full run: the current level's [GameState] plus the run-level bookkeeping that
 * survives across levels. The ordered [intents] are the run's **replay tape** —
 * seed + intents fully reconstruct the run, which is exactly what the leaderboard
 * backend re-executes to validate a submitted score without trusting the client.
 *
 * @param mode distinguishes a normal roguelite run from a daily-challenge run.
 */
data class RunState(
    val seed: Long,
    val current: GameState,
    val levelIndex: Int,
    val status: RunStatus = RunStatus.RUNNING,
    val mode: RunMode = RunMode.ROGUE,
    val intents: List<Intent> = emptyList(),
) {
    /** Total accumulated score (carried through completed levels). */
    val score: Int get() = current.score

    val isOver: Boolean get() = status != RunStatus.RUNNING
}

enum class RunMode { ROGUE, DAILY }
