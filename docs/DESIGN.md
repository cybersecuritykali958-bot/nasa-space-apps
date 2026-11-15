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
Navigation: launch flow Loading → NFC badge sign-in → watch (optional) → app; bottom bar with four tabs; detail screens open on top. Every tab has a language dropdown in the top-right corner.

| Tab / screen | File | Data |
|---|---|---|
| **Operations** (Daily Operations): greeting, mission route, qualification level / compliance points / continuity, one status row, one warning only when needed, today's protocol (tap opens check-in, the reaction test or WHO-5), reminder, commendations | `ui/home/HomeScreen.kt`, `ui/mission/MissionScreen.kt` (`GameSummary`, `Commendations`) | `state.report`, `state.environment` |
| **Physiology** (Physiological Status): *Current State* = holographic 3D body, systems and indicators, entry to check-in, camera pulse, WHO-5 and trends; *Projection* = Digital Twin with the countermeasure coach | `ui/health/HealthScreen.kt`, `ui/body3d/`, `ui/twin/TwinScreen.kt` | `state.systems`, `projectTwin` |
| **Readiness** (Fitness for Duty): Earth response vs response window, report for Earth, EVA / landing GO-NO-GO with reasons, space weather, radiation budget | `ui/readiness/ReadinessScreen.kt` | `report.triage`, `readiness`, `spaceWeather`, `radiation` |
| **Profile** (Crew Member Profile): badge holder, watch, demo / my data, mission environment, about, sign out | `ui/settings/SettingsScreen.kt` | `SettingsStore`, `BadgeRepository` |
| Why this matters (detail): value, chart with threshold and forecast, advice, sources, confidence | `ui/indicator/IndicatorScreen.kt` | evaluation, history, alerts |
| Daily check-in (detail): validated by the backend, saved to Room; the mood card unfolds an optional WHO-5 test | `ui/checkin/` | `InputValidator`, `ReadingRepository` |
| WHO-5 (detail) | `ui/who5/Who5Screen.kt` | `Who5`, `saveWho5` |
| Camera pulse (detail), with a simulated-finger demo mode | `ui/pulse/CameraPulseScreen.kt` | `PulseFromCamera`, `saveCameraPulse` |
| Report for Earth (detail) | `ui/earth/EarthReportScreen.kt` | `EarthPacket`, `report.triage` |
| Reaction test (detail, PVT-B) | `ui/pvt/PvtScreen.kt` | `PvtSession`, `savePvt` |
| Trends (detail) | `ui/trends/TrendsScreen.kt` | `state.history` |

Shared components: `BodyFigure` (Canvas), `LineChart` (Canvas, no chart library), `Pill`/`StatusPill`/`VerdictPill`/`ConfidencePill`, `InfoRow`, `CrewCard`, `CrewScaffold`.

**Languages:** English, Ukrainian, Japanese, German. Switch with the language dropdown in the top-right corner of every tab; the choice is saved and the screen is recreated. Translations live in `res/values*/strings.xml` (machine-assisted, please review: especially Japanese).

## Body figure
- SVG, each system is a separate `path` with an `id`: `cardiovascular`, `metabolic`, `immune`, `behavioral`, `musculoskeletal`, `vision`, `radiation` (radiation is a halo around the figure).
- Hand it to @harvio1 in `android/app/src/main/res/drawable/` (frontend converts it to a VectorDrawable).

## Style
- Dark theme (cabin, night), high contrast, readable on a projector.
- Status is green / yellow / red **plus** an icon or text (never color alone, for color-blind users).
- All UI texts in English.
- Placeholder colors are in `ui/theme/Theme.kt`; hand final colors to @harvio1.

## Visual style (Material 3, calm dark)
- Background graphite `#1B1E21`, cards `#24272B` without borders or glow, one accent: soft teal `#6BD3C3`.
- Text `#E3E5E8`, secondary `#9AA0A6`. Status colors are desaturated: green `#7FC8A0`, amber `#E6B566`, red `#E58A8A`.
- Status is a filled chip with dark text; icons are outlined (Material Icons Extended), the selected tab icon is filled.
- Shared building blocks: `CrewCard`, `ListRow` / `CardRow`, `Pill`, `StatusMark`, `CrewIcons`.

## Readability rules
- The app root is a `Surface` with `onBackground` content color: plain `Text` is light by default. Never use `Color.Black` for text.
- Secondary text uses `onSurfaceVariant`; status always comes with a word (pill), not color alone.
- Engine output is phrased in the UI from structured fields (`ui/i18n/Labels.kt`), so it is translated.
