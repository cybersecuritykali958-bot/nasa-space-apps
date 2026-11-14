---
name: health-engine
description: How the CrewPulse backend engine works and how to use or extend it: HomeViewModel API for screens, EngineReport fields, engine_rules.json, adding indicators, patterns, readiness gates and new domain logic. Use for any work that reads engine output or changes data/ or domain/.
---

# CrewPulse health engine

Full guide: `docs/BACKEND.md`. Data format: `docs/DATA_CONTRACT.md`.

## Using it from screens (ui/)
- Get state: `val state by home.state.collectAsStateWithLifecycle()` (`HomeViewModel`, shared in `AppNavHost`).
- Everything is in `state.report: EngineReport`: `systems`, `alerts`, `confidence`, `patterns`, `triage`, `readiness`, `plan`, `game`, `spaceWeather`, `radiation`.
- Actions: `saveCheckIn(map)` (returns `ValidationResult`, show `errors`), `setBaseline`, `clearBaseline`, `setProfile(Profile.ME|DEMO)`, `setEnvironment(id)`, `refreshSpaceWeather()`, `runBenchmark()`, `savePvt(result)`, `projectTwin(adherence, days|phases)`, `sendReminderNow()`.
- PVT screen: drive `PvtSession` (UI owns the clock), see `docs/BACKEND.md` section 3.
- Never re-implement engine logic in composables; show what the report says.

## Extending without code
- New multi-indicator rule: add to `patterns` in `assets/engine_rules.json`.
- New GO/NO-GO gate: add to `readiness[].gates`.
- New indicator: add to `assets/indicators.json` (+ `schedule.intervalDays`).
- Every number needs a source; unverified ones stay `"verified": false`.

## Extending with code (domain/)
1. Pure Kotlin object in `domain/`, input as plain data, no Android imports.
2. Add the result to `EngineReport` and compute it in `HealthEngine.analyze`.
3. Config goes to `EngineRules` (with defaults) + `engine_rules.json`.
4. Unit test in `app/src/test/`; `AssetContractTest` must stay green.
5. Run `./gradlew testDebugUnitTest assembleDebug` in `android/`.
