# Team KHNUIA roles

| # | Role | Who | Zone | Tool |
|---|---|---|---|---|
| 1 | Team lead / architect | Claude Code (driven by @lazzzzz1) | root, Gradle, CI, `docs/PLAN.md` | Claude Code |
| 2 | Frontend (screens) | @harvio1 | `ui/` | Cursor / Android Studio |
| 3 | Backend (on-device logic) | @jarororoor | `data/`, `domain/`, `docs/BACKEND.md` | Claude Code / Cursor |
| 4 | Science | @LoranSent | `assets/indicators.json`, `risks.json`, `environments.json`, `engine_rules.json` (rules), `docs/SCIENCE.md` | Claude Code |
| 5 | Data engineering | @interpolfuckyou | `science/`, `assets/demo_mission.json`, `assets/donki_cache.json` | Claude Code |
| 6 | Design and presentation | @gutsonithemvp | `docs/DESIGN.md`, `docs/PITCH.md`, `docs/SUBMISSION.md`, slides, video | Figma / Canva |

Paths `ui/`, `data/`, `domain/` are relative to `android/app/src/main/java/space/crewpulse/`.

## Responsibilities

**Team lead.** Skeleton, contract, merges every 1–2 hours, APK build, submission, the talk at the defense.

**@harvio1, frontend.** Body figure (home screen), check-in form, "Why?" card, trend charts, triage block, readiness, measurement plan, gamification header, space weather banner. All data comes from `HomeViewModel.state.report` (see `docs/BACKEND.md`).

**@jarororoor, backend.** Room storage, personal baseline, health engine (early warning, confidence, patterns, triage, readiness), NASA DONKI, measurement plan, gamification, self-validation. Next: PVT logic, camera pulse (if time allows).

**@LoranSent, science.** Verify every threshold and source in `indicators.json` and set `verified: true`. Review pattern rules, triage windows and readiness gates in `engine_rules.json`. Digital Twin formulas. Source table in `docs/SCIENCE.md`. Answers to scientists at the defense.

**@interpolfuckyou, data.** Demo mission generator (`science/generate_demo_mission.py`), DONKI cache (`science/fetch_donki.py`), rPPG camera pulse prototype in Python before porting to Kotlin.

**@gutsonithemvp, design.** Mockups of the key screens, body figure SVG with system zones, app icon, 7 slides, 30-second video with English subtitles, Project Page texts.

## Pairs
- @gutsonithemvp → @harvio1: mockups and SVG → screens.
- @LoranSent → @jarororoor: thresholds and rules → engine.
- @interpolfuckyou → @jarororoor: demo data, DONKI cache, pulse algorithm.
- @LoranSent + @gutsonithemvp + team lead: science → slides and pitch.
