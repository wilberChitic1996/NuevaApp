# Publishing checklist — Google Play

Everything needed to take Cadejo from this repo to a Play release. Items marked
**(you)** require your Google/account credentials and can't be automated here.

## Build & signing

- [x] `targetSdk` = current (35), `minSdk` = 26.
- [x] R8 full-mode shrinking + obfuscation on release (`app/proguard-rules.pro`);
      `mapping.txt` is archived by CI for crash de-obfuscation.
- [x] Release signing wired from secrets (`RELEASE_*`); build with `./gradlew bundleRelease`.
- [ ] **(you)** Enroll in **Play App Signing**; upload key is the one configured via
      `RELEASE_KEYSTORE_BASE64` in CI secrets.
- [ ] **(you)** Generate the upload keystore and add these GitHub secrets:
      `RELEASE_KEYSTORE_BASE64`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`,
      `RELEASE_KEY_PASSWORD`, plus `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `ADMOB_APP_ID`.

## Store listing (you)

- [ ] App name, short & full description (ES + EN — strings already localized).
- [ ] Screenshots (phone + tablet), feature graphic, icon (adaptive icon already in repo).
- [ ] Category: Games → Puzzle. Content rating questionnaire.
- [ ] Countries/regions, pricing (free with IAP).

## Data Safety form (you)

Declare exactly what the code collects (see `PRIVACY.md`):
- **Location / contacts / photos / files:** none.
- **Device or other IDs:** advertising ID (via AdMob, consent-gated); integrity token.
- **App activity / in-app purchases:** purchase info via Play Billing; daily score
  + user-chosen display name to the leaderboard backend.
- **Data is encrypted in transit; leaderboard rows carry a non-identifying device hash.**
- Users can play fully offline; ads require consent.

## Policy compliance (you)

- [ ] Ads policy: only rewarded ads, UMP consent implemented — confirm in console.
- [ ] Families policy: if targeting children, disable personalized ads + AD_ID.
- [ ] Complete the backend deploy (`backend/README.md`) and the Play Integrity
      verification (`verifyIntegrity` TODO) before enabling the leaderboard.

## Quality (in-repo)

- **Tests:** `./gradlew test testDebugUnitTest` (deterministic domain + view models).
  Compose smoke test in `feature/game` androidTest (run on an emulator/CI device).
- **Lint:** `./gradlew ktlintCheck detekt`.
- **Performance — how to measure:**
  - Enable Compose **compiler metrics** (`-P` reports) and check for unstable params.
  - Use the **Layout Inspector → recomposition counts** to spot over-recomposition;
    the board reads immutable state and animates via `animate*AsState`, so only the
    changed pieces recompose.
  - Add a **Macrobenchmark** module to measure frame timing / startup on a mid device;
    target 60 fps with no jank during turns.
- **Accessibility:** `contentDescription` on interactive elements, ≥48dp touch
  targets, and colorblind mode distinguishing spirits by **shape** as well as colour.

## Pre-launch

- [ ] Internal testing track → closed testing → production, per Play's staged rollout.
- [ ] Pre-launch report (Play automatically exercises the app on real devices).
