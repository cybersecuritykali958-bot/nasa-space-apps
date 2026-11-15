# CrewPulse

**NASA Space Apps Challenge 2026 · Dnipro · team KHNUIA**
Challenge: *Create Health Monitoring Software for Astronauts on Space Missions*

CrewPulse is an offline-first Android app that lets astronauts run a quick self-exam, see the status of each body system at a glance and know what to do. It keeps working when Earth is 20 minutes away. Every threshold is traced to a NASA or peer-reviewed source.

## Structure

| Folder | What | Owner |
|---|---|---|
| `android/` | Android app (Kotlin, Jetpack Compose) | whole dev team, see zones below |
| `android/app/src/main/java/space/crewpulse/ui/` | Screens | @harvio |
| `android/app/src/main/java/space/crewpulse/data/`, `domain/` | Room, data loading, status engine, sensors, NASA API | @creawer |
| `android/app/src/main/assets/` | Science data (JSON) | @LoranSent, @toma322 |
| `science/` | Python: demo data, models, algorithm checks | @LoranSent, @toma322 |
| `docs/` | Plan, roles, contract, design, pitch, submission | | @creawer, @gutsoni |
| root, `.github/`, `.claude/`, `.cursor/` | Configs, CI | | @creawer |

## Quick start

1. Install Android Studio (it ships its own JDK).
2. `gh repo clone cybersecuritykali958-bot/nasa-space-apps`
3. Android Studio → **Open** → select the `android/` folder (not the repo root).
4. Wait for Gradle sync, pick an emulator or a phone, press **Run**.

Command line:
```bash
cd android
./gradlew testDebugUnitTest   # unit tests
./gradlew assembleDebug       # APK in app/build/outputs/apk/debug/
python ../science/generate_demo_mission.py   # regenerate demo data
```

Optional NASA API key: add `NASA_API_KEY=...` to `android/local.properties` (never committed). Without it the app uses `DEMO_KEY`.

## Docs

- [docs/ONBOARDING.md](docs/ONBOARDING.md): set up your machine
- [docs/PLAN.md](docs/PLAN.md): step-by-step plan and timeline
- [docs/TEAM_ROLES.md](docs/TEAM_ROLES.md): who does what
- [docs/DATA_CONTRACT.md](docs/DATA_CONTRACT.md): JSON format
- [docs/SCIENCE.md](docs/SCIENCE.md): sources and verification status
- [docs/DESIGN.md](docs/DESIGN.md): design brief
- [docs/SUBMISSION.md](docs/SUBMISSION.md): Space Apps project page checklist
- [docs/PITCH.md](docs/PITCH.md): demo script and defense

## Disclaimer

CrewPulse is a decision-support and training prototype, not a medical device. Demo data is synthetic.
