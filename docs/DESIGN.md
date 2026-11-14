# Design brief

Owner: @gutsonithemvp. Figma link: _add here_

## Screens (priority)
1. **Home:** body figure with system zones colored by status; mission day and environment on top; triage block ("what to do now"); space weather banner; buttons Check-in / PVT / Twin / Trends.
2. **Check-in:** 6 fields with a "how to measure" hint each, large buttons (gloves, microgravity).
3. **Indicator card ("Why?"):** value + color, what to do, why it matters in space, source, confidence, early warning ("trending up, threshold in ~7 days").
4. **Trends:** chart with colored range bands and event markers.
5. **Readiness:** GO / CAUTION / NO-GO for spacewalk and landing with reasons.
6. **Today's mission:** measurement plan, streak, points, badges.
7. Later: PVT, Digital Twin (mission day slider), validation screen.

## Implemented (Compose, matches the mockup)
| Screen | File | Data |
|---|---|---|
| Home: mission banner (tap to change environment), demo/my data switch, body figure, what to do, early warning, space weather, quick actions | `ui/home/HomeScreen.kt` | `state.report`, `state.environment` |
| Daily check-in (validated by the backend, saves to Room) | `ui/checkin/` | `InputValidator`, `ReadingRepository` |
| Why this matters: value, chart with threshold and forecast, advice, sources, confidence | `ui/indicator/IndicatorScreen.kt` | evaluation, history, alerts |
| Readiness & triage: Earth response vs window, EVA / landing GO-NO-GO, space weather, radiation budget | `ui/readiness/ReadinessScreen.kt` | `report.triage`, `readiness`, `spaceWeather`, `radiation` |
| Digital Twin: skeleton, bone / muscle / dose charts, day slider, exercise switch, scenarios | `ui/twin/TwinScreen.kt` | `projectTwin` |
| Reaction test (PVT-B) | `ui/pvt/PvtScreen.kt` | `PvtSession`, `savePvt` |
| Mission & you: level, streak, today's mission, reminder, badges | `ui/mission/MissionScreen.kt` | `report.game`, `report.plan` |
| Trends | `ui/trends/TrendsScreen.kt` | `state.history` |

Shared components: `BodyFigure` (Canvas), `LineChart` (Canvas, no chart library), `Pill`/`StatusPill`/`VerdictPill`/`ConfidencePill`, `InfoRow`, `CrewCard`, `CrewScaffold`.

**Languages:** English, Ukrainian, Japanese, German. Switch with the language button on Home (top right); the choice is saved and the screen is recreated. Translations live in `res/values*/strings.xml` (machine-assisted, please review: especially Japanese).

## Body figure
- SVG, each system is a separate `path` with an `id`: `cardiovascular`, `metabolic`, `immune`, `behavioral`, `musculoskeletal`, `vision`, `radiation` (radiation is a halo around the figure).
- Hand it to @harvio1 in `android/app/src/main/res/drawable/` (frontend converts it to a VectorDrawable).

## Style
- Dark theme (cabin, night), high contrast, readable on a projector.
- Status is green / yellow / red **plus** an icon or text (never color alone, for color-blind users).
- All UI texts in English.
- Placeholder colors are in `ui/theme/Theme.kt`; hand final colors to @harvio1.
