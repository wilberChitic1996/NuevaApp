package gt.guardian.cadejo.domain.run

import gt.guardian.cadejo.domain.engine.GameEngine
import gt.guardian.cadejo.domain.level.LevelGenerator
import gt.guardian.cadejo.domain.model.Balance
import gt.guardian.cadejo.domain.model.GameStatus
import gt.guardian.cadejo.domain.model.Intent

/**
 * Chains ten procedural levels into a single run, threading the score forward and
 * recording every intent. Like [GameEngine], it is a pure function of its inputs,
 * so a run replays identically anywhere — client or server.
 */
object RunEngine {

    fun newRun(seed: Long, mode: RunMode = RunMode.ROGUE): RunState {
        val first = LevelGenerator.generate(seed = seed, levelIndex = 1, startingScore = 0)
        return RunState(seed = seed, current = first, levelIndex = 1, mode = mode)
    }

    /**
     * Applies one player [intent] to the run. When a level is cleared, the next
     * level is generated (carrying the score); clearing level 10 completes the run,
     * and any loss fails it.
     */
    fun apply(run: RunState, intent: Intent): RunState {
        if (run.isOver) return run

        val next = GameEngine.reduce(run.current, intent)

        // Record only intents that actually advanced the game, keeping the replay
        // tape minimal and faithful (illegal taps are no-ops the server can skip).
        val advanced = next.turn != run.current.turn || next.status != run.current.status
        val recorded = if (advanced) run.intents + intent else run.intents

        return when (next.status) {
            GameStatus.PLAYING -> run.copy(current = next, intents = recorded)

            GameStatus.LOST -> run.copy(current = next, status = RunStatus.FAILED, intents = recorded)

            GameStatus.WON -> {
                if (run.levelIndex >= Balance.LEVELS_PER_RUN) {
                    run.copy(current = next, status = RunStatus.COMPLETED, intents = recorded)
                } else {
                    val nextLevel = run.levelIndex + 1
                    val generated = LevelGenerator.generate(
                        seed = run.seed,
                        levelIndex = nextLevel,
                        startingScore = next.score,
                    )
                    run.copy(current = generated, levelIndex = nextLevel, intents = recorded)
                }
            }
        }
    }

    /**
     * Replays a run from scratch given its seed and intent tape, returning the
     * final run state. This is the canonical scoring path the backend uses: it
     * trusts only the seed + intents, never a client-reported score.
     */
    fun replay(seed: Long, intents: List<Intent>, mode: RunMode = RunMode.ROGUE): RunState =
        intents.fold(newRun(seed, mode)) { run, intent -> apply(run, intent) }
}
