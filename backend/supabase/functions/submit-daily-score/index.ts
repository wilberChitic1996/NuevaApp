// ============================================================================
//  Cadejo — `submit-daily-score` Supabase Edge Function (Deno)
//
//  DELIVERED AS A SCRIPT ONLY — deploy it yourself with `supabase functions
//  deploy submit-daily-score`. It is never executed by the app or an agent.
//
//  Trust model — the whole point of this function:
//    The client CANNOT be trusted to report its own score. So it sends the daily
//    date + its intent tape + a Play Integrity token. This function:
//      1. Verifies the Play Integrity token (genuine app, genuine device).
//      2. Recomputes the daily seed from the date itself (FNV-1a) — it does NOT
//         trust any seed the client sent.
//      3. Re-runs the intent tape through the ported engine and RECOMPUTES the
//         score and reached level server-side.
//      4. Only if the recomputed score matches the claim does it upsert the row,
//         using the service-role key (which bypasses RLS; clients cannot write).
// ============================================================================

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { decodeIntents, replayRun, seedForDate } from "./engine.ts";

interface Payload {
  date_utc: string;
  seed: number;
  score: number;
  reached_level: number;
  intents: string;
  display_name: string;
  integrity_token: string | null;
}

const json = (status: number, body: unknown): Response =>
  new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });

/**
 * Verify a Google Play Integrity token. In production this calls the Play
 * Integrity API (decode the token, then require `appRecognitionVerdict ===
 * PLAY_RECOGNIZED` and `deviceRecognitionVerdict` to contain `MEETS_DEVICE_INTEGRITY`).
 * For local development set ALLOW_MISSING_INTEGRITY=true to bypass.
 */
async function verifyIntegrity(token: string | null): Promise<boolean> {
  if (Deno.env.get("ALLOW_MISSING_INTEGRITY") === "true") return true;
  if (!token) return false;
  // TODO(deploy): call the Play Integrity API with your service account and check
  // appIntegrity / deviceIntegrity / accountDetails verdicts here. Left as a clear
  // extension point so no half-verified token is silently accepted.
  return false;
}

/** A stable, non-identifying per-device hash to keep one best row per device/day. */
async function deviceHash(token: string | null, name: string): Promise<string> {
  const material = `${token ?? "anon"}|${name}`;
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(material));
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, "0")).join("").slice(0, 32);
}

Deno.serve(async (req: Request): Promise<Response> => {
  if (req.method !== "POST") return json(405, { error: "method not allowed" });

  let payload: Payload;
  try {
    payload = await req.json();
  } catch {
    return json(400, { error: "invalid json" });
  }

  // Basic shape checks.
  if (!payload.date_utc || typeof payload.intents !== "string") {
    return json(400, { error: "missing fields" });
  }
  if (!/^\d{4}-\d{2}-\d{2}$/.test(payload.date_utc)) {
    return json(400, { error: "bad date format" });
  }

  // 1. Integrity.
  if (!(await verifyIntegrity(payload.integrity_token))) {
    return json(401, { error: "integrity check failed" });
  }

  // 2. Authoritative seed from the date — never trust a client-sent seed.
  const seed = seedForDate(payload.date_utc);

  // 3. Re-run the tape and recompute the score.
  let result;
  try {
    result = replayRun(seed, decodeIntents(payload.intents));
  } catch (e) {
    return json(400, { error: `replay failed: ${e instanceof Error ? e.message : "unknown"}` });
  }

  // 4. The claim must match the recomputation exactly.
  if (result.score !== payload.score || result.reachedLevel !== payload.reached_level) {
    return json(409, {
      error: "score mismatch",
      recomputed: { score: result.score, reachedLevel: result.reachedLevel },
    });
  }

  // Passed validation — write with the service role (bypasses RLS).
  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
  );

  const name = (payload.display_name ?? "Guardián").slice(0, 24);
  const hash = await deviceHash(payload.integrity_token, name);

  // Upsert the best score per device per day (keep the higher of the two).
  const { data: existing } = await supabase
    .from("daily_leaderboard")
    .select("score")
    .eq("date_utc", payload.date_utc)
    .eq("device_hash", hash)
    .maybeSingle();

  if (existing && existing.score >= result.score) {
    return json(200, { status: "kept_existing", best: existing.score });
  }

  const { error } = await supabase.from("daily_leaderboard").upsert(
    {
      date_utc: payload.date_utc,
      display_name: name,
      score: result.score,
      reached_level: result.reachedLevel,
      seed: seed.toString(),
      device_hash: hash,
    },
    { onConflict: "date_utc,device_hash" },
  );

  if (error) return json(500, { error: error.message });
  return json(200, { status: "accepted", score: result.score });
});
