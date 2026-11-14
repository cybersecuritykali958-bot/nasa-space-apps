# AGENTS.md

Instructions for AI agents (Cursor, Codex, etc.). Read `CLAUDE.md` first: it has the full rules.

- Project: CrewPulse, offline-first Android app (Kotlin, Jetpack Compose) for astronaut self-assessment. NASA Space Apps 2026.
- Gradle project lives in `android/`. Python data scripts in `science/`.
- Zones: `ui/` → frontend, `data/` + `domain/` → backend, `assets/` + `science/` → science, `docs/` → design. Edit only your zone.
- Data contract: `docs/DATA_CONTRACT.md` + `data/model/Contract.kt`. Change only with team lead approval.
- No live network dependency in the demo. Secrets only in `android/local.properties`.
- Every medical number needs a source. If unsure, leave `TODO(science)`.
- After changes run `./gradlew testDebugUnitTest assembleDebug` in `android/` and report the result.
