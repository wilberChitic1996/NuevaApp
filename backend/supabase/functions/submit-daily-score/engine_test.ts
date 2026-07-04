// Cross-validation of the TypeScript engine port against the Kotlin golden vectors.
// Run with:  deno test backend/supabase/functions/submit-daily-score/engine_test.ts
//
// Expected values come from the Kotlin `GoldenVectorTest`
// (./gradlew :core:domain:test --tests "*GoldenVectorTest*"). If either engine
// changes, regenerate the Kotlin golden and update the constants below. These were
// verified to match with Node's --experimental-strip-types during development.

import { assertEquals } from "https://deno.land/std@0.224.0/assert/mod.ts";
import { decodeIntents, replayRun, seedForDate } from "./engine.ts";

// Kotlin prints the seed as a SIGNED 64-bit Long: -6065933189065925002.
// seedForDate returns the same 64 bits as an UNSIGNED u64 BigInt.
const EXPECTED_DAILY_SEED = (1n << 64n) - 6065933189065925002n;

Deno.test("daily seed matches Kotlin golden for 2026-07-04", () => {
  assertEquals(seedForDate("2026-07-04"), EXPECTED_DAILY_SEED);
});

Deno.test("seed 1 greedy run matches Kotlin golden (FAILED, level 3, score 615)", () => {
  // The exact tape the Kotlin greedy policy produces for seed 1.
  const tape =
    "M2,0;M1,0;M0,0;M-1,0;M-2,0;M-3,0;M1,-3;M0,-2;M-1,-1;M-2,0;M-3,1;M-4,2;M-4,3;M-4,4;M-4,1;M-3,1;M-2,1";
  const result = replayRun(1n, decodeIntents(tape));
  assertEquals(result.status, "FAILED");
  assertEquals(result.reachedLevel, 3);
  assertEquals(result.score, 615);
});
