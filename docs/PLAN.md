# CrewPulse implementation plan

Submission: **2026-11-15 23:55**. All times Kyiv.

## MVP (never cut)
1. Check-in of 6 indicators (blood pressure, heart rate, glucose, temperature, sleep, mood).
2. Body figure with each system colored green / yellow / red.
3. "Why?" card: what to do, why it matters in space, how to measure, source.
4. Trends over the 180-day demo mission.
5. Demo astronaut (synthetic data, clearly labeled).

## Backend engine (done, see `docs/BACKEND.md`)
Early warning, confidence, patterns with discordance detector, triage with communication delay, GO / NO-GO, NASA DONKI space weather, radiation budget, adaptive measurement plan, gamification, self-validation.

## Wow modules (in order)
1. PVT reaction test.
2. Digital Twin: projected bone, muscle and dose up to the 600 mSv limit.
3. ~~Environment switch and communication delay~~ (backend done, needs UI).
4. ~~NASA DONKI~~ (backend done, needs UI).
5. Camera pulse (rPPG). If it does not make it, manual input stays.

## Steps
| # | When | What | Who | Done when |
|---|---|---|---|---|
| 0 | 14.11 by 11:00 | Android Studio, emulator, repo access | all | the app runs for everyone |
| 1–3 | 14.11 | Skeleton, data contract, mocks | team lead | ✅ merged |
| 4 | 14.11 by 15:00 | Mockups of key screens, body figure SVG | @gutsonithemvp | Figma link in `docs/DESIGN.md` |
| 5 | 14.11 11:00–21:00 | MVP in parallel: screens / engine / threshold review / demo data | all | a check-in changes a system color |
| — | 14.11 13:00 | Mentors: scientific logic of thresholds and early warning | team lead, @LoranSent | — |
| 6 | 14.11 22:00 | First end-to-end build | team lead | MVP flow works on a phone |
| 7 | 15.11 09:00–19:00 | Wow modules 1→5, UI for engine output | see roles | each module in `main` without breaking MVP |
| — | 15.11 14:00 | Mentors: show the working app | all | — |
| 8 | 15.11 19:00 | **Feature freeze**, bug fixes only, APK release | team lead | APK in GitHub Releases |
| 9 | 15.11 19:00–22:00 | Slides / 30 s video, Project Page texts | @gutsonithemvp, team lead | `docs/SUBMISSION.md` complete |
| 10 | 15.11 22:00–23:00 | Submit, check links in incognito | team lead | submitted before 23:00 |
| 11 | 16–18.11 | Rehearsals, defense on 17–18.11 at 14:00 | all | — |

## If we run late
Cut from the end: camera pulse → Digital Twin → PVT. MVP and the engine UI (triage, alerts) stay.
