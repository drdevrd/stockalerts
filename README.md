# Stock Alerts (com.drdevrd.stockalerts)

Native Android app: daily NSE + US closing-price alerts, with a repeating
notification until you dismiss it.

## How it works

- **NSE stocks** (default: Nifty 50) — fetched from NSE India's free public
  endpoint at ~3:30 PM IST. No API key needed.
- **US stocks** (default: ~30 large caps) — fetched from Finnhub's `/quote`
  endpoint at 4:00 PM America/New_York (handles EST/EDT automatically, so
  this stays correct year-round without manual IST-offset math).
- Add any other symbol yourself from the **+** button — NSE or US, with an
  optional target price and a choice of "daily close", "target crossed", or
  both.
- When an alert fires, the notification **repeats every N minutes** (set in
  Settings, default 15) until you open the app or tap "Dismiss reminders".

## Before you build

1. Open **Settings** in the app (after first install) and paste your free
   Finnhub API key (finnhub.io → sign up → dashboard → API key). This is
   only needed for US stocks; NSE works with no key.
2. Note: Finnhub's **free tier does not cover real-time NSE/international
   quotes** — that's why NSE goes through its own free endpoint instead.
   Don't pay for Finnhub Enterprise just for NSE; it's not needed.

## Building via your existing GitHub Actions pipeline

This repo includes `.github/workflows/build-release.yml`, wired to your
existing signing secrets:
- `KEYSTORE_BASE64`
- `KEY_ALIAS`
- `KEY_PASSWORD`
- `STORE_PASSWORD`

Steps:
1. Push this project to a new (or existing) GitHub repo under `drdevrd`.
2. Make sure the four secrets above are set on that repo (Settings →
   Secrets and variables → Actions) — same as your other apps.
3. Push to `main`, or trigger manually via the "Run workflow" button.
4. Download the signed APK from the workflow run's **Artifacts** section.

## Building locally instead (Android Studio)

1. Open the project folder in Android Studio.
2. Let Gradle sync (uses Gradle 8.7, AGP 8.5, Kotlin 1.9.24 — all standard,
   no unusual setup needed).
3. Run on a device/emulator, or Build > Generate Signed Bundle/APK using
   your existing keystore.

## Known caveats (real-world Android behaviour)

- Some OEMs (Xiaomi/MIUI, Oppo, Vivo, Realme) aggressively kill background
  alarms unless the app is manually whitelisted from battery optimization —
  the app prompts for this on first launch, but double-check it stuck on
  your specific phone.
- The NSE endpoint is unofficial/undocumented. It works today (verified
  live), but NSE can change or rate-limit it without notice — if NSE alerts
  silently stop one day, that's the likely cause, not a bug in the app.
- Weekends are skipped automatically (NSE/US both closed), but market
  holidays are not currently detected — you'll just get a silent skip that
  day (no crash, just nothing to report).
