-- ============================================================================
--  Cadejo — Daily leaderboard schema (Supabase / Postgres)
--
--  DELIVERED AS A SCRIPT ONLY. Do not run this from the app or an agent — review
--  it and apply it yourself via `supabase db push` or the SQL editor.
--
--  Security model:
--   * Clients may READ the leaderboard (anon SELECT).
--   * Clients may NOT write it directly. Rows are inserted ONLY by the
--     `submit-daily-score` Edge Function using the service-role key, and only
--     after it has re-run the submitted intent tape and recomputed the score.
--     RLS below denies all client writes; the service role bypasses RLS.
-- ============================================================================

create table if not exists public.daily_leaderboard (
    id            uuid primary key default gen_random_uuid(),
    date_utc      date        not null,
    display_name  text        not null check (char_length(display_name) between 1 and 24),
    score         integer     not null check (score >= 0),
    reached_level integer     not null check (reached_level between 1 and 10),
    seed          bigint      not null,
    -- A per-device hash (not a raw identifier) so we keep one best row per device
    -- per day without storing anything personally identifying.
    device_hash   text        not null,
    created_at    timestamptz not null default now()
);

-- One best score per device per day; the Edge Function upserts on this key.
create unique index if not exists daily_leaderboard_device_day
    on public.daily_leaderboard (date_utc, device_hash);

-- Fast "top N for a day" reads.
create index if not exists daily_leaderboard_day_score
    on public.daily_leaderboard (date_utc, score desc);

-- ----------------------------------------------------------------------------
--  Row Level Security
-- ----------------------------------------------------------------------------
alter table public.daily_leaderboard enable row level security;

-- Anyone (anon key) can read the board.
drop policy if exists "leaderboard_public_read" on public.daily_leaderboard;
create policy "leaderboard_public_read"
    on public.daily_leaderboard
    for select
    using (true);

-- No client-side writes. (No INSERT/UPDATE/DELETE policies are created, so with
-- RLS enabled every write from the anon/authenticated roles is denied. Only the
-- service-role key used by the Edge Function can write, since it bypasses RLS.)
