# CLAUDE.md: CrewPulse (NASA Space Apps 2026)

## Project
- **Challenge:** Create Health Monitoring Software for Astronauts on Space Missions.
- **Idea:** offline Android app for astronaut self-assessment: measure → see the status of each body system → know what to do, even when Earth answers in 20+ minutes.
- **Judges:** scientists. Scientific validity matters most: every threshold and number has a source.
- **Priority:** a working demo flow beats feature count.
- **Submission:** 2026-11-15 23:55 (Kyiv). Defense 2026-11-17/18.
- **Language:** everything in the repo is in English (docs, code, comments, commits, PRs).

## Stack
- Kotlin, Jetpack Compose, Material 3, Navigation Compose.
- Room (local DB), kotlinx.serialization (JSON from `assets/`).
- Manual DI through `AppContainer`. Do not add Hilt.
- NASA DONKI via `HttpURLConnection` + kotlinx.serialization (no Retrofit), offline cache in `assets/donki_cache.json`.
- Backend engine: `docs/BACKEND.md` (how it works and how screens use it).
- WorkManager for background reminders from `report.plan`.
- Planned: CameraX + ML Kit (camera pulse), Vico (charts).
- Python 3.12 in `science/` for data and algorithm checks.

## Zones (edit only yours)
| Path | Owner |
|---|---|
| `android/app/src/main/java/space/crewpulse/ui/` | @harvio1 |
| `android/app/src/main/java/space/crewpulse/data/`, `domain/` | @jarororoor |
| `android/app/src/main/assets/`, `science/` | @LoranSent, @interpolfuckyou |
| `docs/` (design, slides, texts) | @gutsonithemvp |
| root, `android/*.kts`, `gradle/`, `.github/`, `.claude/`, `.cursor/`, `docs/PLAN.md` | team lead |

## Data contract
- JSON format: `docs/DATA_CONTRACT.md`; Kotlin models: `data/model/Contract.kt`, `data/model/EngineRules.kt`.
- Change both together and only with team lead approval.
- The demo never depends on live APIs: everything needed is in `assets/`; the network is a bonus with a cache.

## Code rules
- Code, names, comments and UI texts in English.
- Small composables; state in ViewModels (`StateFlow`).
- New dependencies go into `gradle/libs.versions.toml` only after the team lead agrees.
- Secrets only in `android/local.properties` (not committed).
- Cover `domain/` logic with unit tests in `app/src/test/`.

## Git
- Branch `feat/<nick>-<task>`, PR into `main`, the team lead merges every 1–2 hours.
- Commit before a large agent edit.
- CI builds the APK and runs tests on every PR. Do not merge a red CI.

## Commands
```bash
cd android && ./gradlew testDebugUnitTest assembleDebug
python science/generate_demo_mission.py
python science/fetch_donki.py     # refresh the NASA DONKI offline cache (key in android/local.properties)
```

## How agents should work
- Read the relevant files and `docs/PLAN.md` first.
- Do only what is asked, no unrelated refactoring.
- Do not touch other zones or the contract without an explicit request.
- After changes run tests and the build, report the result briefly.
- Never invent medical numbers: no source → mark `TODO(science)`.
