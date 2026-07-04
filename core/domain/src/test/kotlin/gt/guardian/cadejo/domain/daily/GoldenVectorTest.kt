package gt.guardian.cadejo.domain.daily

import gt.guardian.cadejo.domain.model.Intent
import gt.guardian.cadejo.domain.run.RunEngine
import gt.guardian.cadejo.domain.run.RunMode
import gt.guardian.cadejo.domain.run.RunStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Golden vectors that pin the deterministic engine so the TypeScript port used by
 * the Supabase Edge Function can't silently drift out of sync (see backend/README.md
 * and engine_test.ts). If these values change, the TS port and its test must be
 * updated in lockstep — that's the whole point of the daily-leaderboard security.
 */
class GoldenVectorTest {

    @Test
    fun `daily seed for 2026-07-04 is stable`() {
        assertEquals(-6065933189065925002L, DailySeed.seedForDate("2026-07-04"))
    }

    @Test
    fun `seed 1 greedy run matches the TS cross-check vector`() {
        // A greedy "step toward the goal" policy over seed 1 wins levels 1-2 and
        // dies on level 3 — exercising generation, wins, scoring and chaining.
        var run = RunEngine.newRun(1L, RunMode.DAILY)
        repeat(400) {
            if (!run.isOver) {
                val s = run.current
                val step = s.legalMoves().minByOrNull { it.distanceTo(s.goal) }
                run = RunEngine.apply(run, if (step != null) Intent.Move(step) else Intent.Wait)
            }
        }

        // The exact values asserted here are mirrored by engine_test.ts.
        assertEquals(RunStatus.FAILED, run.status)
        assertEquals(3, run.levelIndex)
        assertEquals(615, run.score)

        // Replaying the recorded tape reproduces the identical result.
        val replayed = RunEngine.replay(1L, run.intents, RunMode.DAILY)
        assertEquals(run.status, replayed.status)
        assertEquals(run.score, replayed.score)
        assertEquals(run.levelIndex, replayed.levelIndex)
    }
}
