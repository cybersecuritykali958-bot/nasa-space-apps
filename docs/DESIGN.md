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
Navigation: bottom bar with five tabs; detail screens open on top. Settings via the gear on Home.

| Tab / screen | File | Data |
|---|---|---|
| **Home** (daily briefing): greeting, mission line, one status row, one warning only when needed, today's plan (tap opens check-in or the reaction test). No measurements on Home | `ui/home/HomeScreen.kt` | `state.report`, `state.environment` |
| **Health**: all body systems and indicators, entry to check-in and trends | `ui/health/HealthScreen.kt` | `state.systems` |
| **Readiness**: Earth response vs response window, EVA / landing GO-NO-GO with reasons, space weather, radiation budget | `ui/readiness/ReadinessScreen.kt` | `report.triage`, `readiness`, `spaceWeather`, `radiation` |
| **Twin**: skeleton, bone / muscle / dose charts, day slider, exercise switch, scenarios | `ui/twin/TwinScreen.kt` | `projectTwin` |
| **Mission**: level, streak, today's mission, reminder, badges | `ui/mission/MissionScreen.kt` | `report.game`, `report.plan` |
| Why this matters (detail): value, chart with threshold and forecast, advice, sources, confidence | `ui/indicator/IndicatorScreen.kt` | evaluation, history, alerts |
| Daily check-in (detail): validated by the backend, saved to Room | `ui/checkin/` | `InputValidator`, `ReadingRepository` |
| Reaction test (detail, PVT-B) | `ui/pvt/PvtScreen.kt` | `PvtSession`, `savePvt` |
| Trends (detail) | `ui/trends/TrendsScreen.kt` | `state.history` |
| Settings (detail): language, demo / my data, mission environment, about | `ui/settings/SettingsScreen.kt` | `SettingsStore` |

Shared components: `BodyFigure` (Canvas), `LineChart` (Canvas, no chart library), `Pill`/`StatusPill`/`VerdictPill`/`ConfidencePill`, `InfoRow`, `CrewCard`, `CrewScaffold`.

**Languages:** English, Ukrainian, Japanese, German. Switch in Settings (gear on Home); the choice is saved and the screen is recreated. Translations live in `res/values*/strings.xml` (machine-assisted, please review: especially Japanese).

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
