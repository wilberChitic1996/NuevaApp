# Privacy Policy — Cadejo: Guardián de la Noche

_Draft. Review with counsel before publishing. Last updated: 2026-07-04._

This policy describes what the game collects and why. It reflects what the app
**actually** does in code; keep it in sync as features change.

## Summary

Cadejo is a single-player puzzle game. Most data never leaves your device. The
only data sent off-device is what's needed for **rewarded ads** (optional),
**in-app purchases**, and the **optional daily leaderboard**. We never request
dangerous permissions and never collect your location, contacts, photos, or files.

## What stays on your device

- **Game progress & stats** (coins, unlocks, run history) — stored locally with
  Room, integrity-protected with a device-bound HMAC key. Not uploaded.
- **Settings** (language, audio, colorblind mode, etc.) — stored locally with
  DataStore.

## What may be sent off-device

| Data | When | Where | Why |
|------|------|-------|-----|
| Advertising identifier & ad-interaction data | Only if you consent (UMP) and watch a rewarded ad | Google AdMob | Show and measure rewarded ads |
| Purchase tokens / order info | When you make a purchase | Google Play Billing | Complete and validate purchases |
| App/device integrity token | When you submit a daily score | Google Play Integrity → our backend | Prove the score came from a genuine app/device |
| Display name, score, date, non-identifying device hash | When you submit a daily score | Our Supabase backend | Show the daily leaderboard |

Notes:
- The **display name** is chosen by you and shown publicly on the leaderboard.
- The **device hash** is a truncated SHA-256, not an advertising or hardware id; it
  only lets us keep one best score per device per day.
- We do **not** upload your local progress or settings.

## Consent (GDPR / CCPA)

Before any ad loads, we show Google's **UMP** consent form where required. If you
decline, ads are not requested. You can revisit consent from your device/EU region
settings.

## Children

If we target the Families program, we will disable personalized ads and the
advertising-id collection accordingly, and complete the Play "Teacher Approved" /
Families requirements. Configure this before release if the audience includes
children.

## Crash reporting

Crash reporting (Crashlytics) is **off by default** and only enabled with your
explicit consent.

## Your choices

- Decline ad consent (no ad requests).
- Buy "remove ads" to stop rewarded-ad prompts.
- Play entirely offline — the game is fully playable with no network.

## Contact

Add your support contact / email here before publishing.
