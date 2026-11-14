# CrewPulse backend: Autonomous Health Reasoning Engine

Owner: @jarororoor. Code: `android/app/src/main/java/space/crewpulse/data/` and `domain/`.

## 1. Concept

An on-device engine that **notices a problem days before a threshold, explains why, and says what to do when Earth cannot answer in time**. It works offline, and every threshold and rule has a source.

| Astronaut's question | Engine's answer | Code |
|---|---|---|
| What is happening? | Status of every indicator against population norms **and** the personal baseline. Early warning on trends. Confidence in the data | `StatusEngine`, `EarlyWarning`, `ConfidenceEstimator` |
| What does it mean? | Multi-indicator patterns with an evidence chain, including a discordance detector ("I say I'm fine, the data disagrees") | `PatternMatcher` |
| What do I do now? | Who acts (self / crew medical officer / Earth / onboard protocol) given the communication delay. GO / NO-GO before a spacewalk or landing | `Triage`, `Readiness` |
| When should I measure? | Adaptive schedule: problems often, stable things rarely. Shows crew time saved | `MeasurementPlanner` |
| Why measure every day? | Streaks, points, levels, badges, daily mission | `Gamification` |
| What about space weather? | Real NASA DONKI events → "go to the shielded area" + radiation budget to the 600 mSv career limit | `SpaceWeather`, `SpaceWeatherRepository` |
| Can I trust the engine? | Self-validation: replay of the 180-day mission, lead time, false alarms | `EngineBenchmark` |

## 2. How the app works (data flow)

```
 Check-in / PVT / camera ──► ReadingRepository ──► Room (readings)
 Demo mission (assets) ──► DemoSeeder ─────────────┘     │
 Personal baseline ──► BaselineRepository ──► Room (baselines)
 NASA DONKI ──► SpaceWeatherRepository (live → cache → assets)
                                                          ▼
 engine_rules.json + indicators.json ──►  HealthEngine.analyze(EngineInput)
                                                          │
                                                          ▼
                                       EngineReport  ──► HomeViewModel.state ──► screens
```

1. The user saves a check-in → values go to Room.
2. Room notifies `HomeViewModel` (Flow); it builds an `EngineInput` and calls `HealthEngine.analyze`.
3. The engine returns one `EngineReport` with everything: statuses, alerts, patterns, triage, readiness, plan, game, space weather.
4. Screens only draw `state.report`. No logic in screens.

Switches (`setProfile`, `setEnvironment`) persist across launches and recompute the report immediately.

## 3. How the frontend uses the backend

Everything goes through `HomeViewModel` (one per app, created in `AppNavHost`).

### Actions
```kotlin
home.saveCheckIn(mapOf("systolic_bp" to 121.0, "diastolic_bp" to 78.0, "heart_rate" to 64.0,
                       "glucose" to 90.0, "body_temp" to 36.6, "sleep_hours" to 7.2, "mood" to 4.0))
home.setProfile(Profile.ME)           // or Profile.DEMO
home.setEnvironment("mars_surface")   // id from environments.json
home.refreshSpaceWeather()            // fresh NASA data, safe offline
home.runBenchmark()                   // engine self-validation → state.benchmark
```

### Reading (`val state by home.state.collectAsStateWithLifecycle()`)
| Field | What it is | UI example |
|---|---|---|
| `state.systems` | Body systems and their color (`SystemStatus`) | Body figure |
| `state.evaluation("heart_rate")` | Value, color, deviation from the personal baseline | "Why?" card |
| `state.report.alerts` | Early warnings (`Alert.message`, `daysToThreshold`) | Banner "blood pressure rising, threshold in ~7 days" |
| `state.report.confidence["heart_rate"]` | `HIGH/MEDIUM/LOW` + reasons | "Re-measure" badge |
| `state.report.patterns` | Matched patterns + `evidence` + advice | "Cardiovascular strain" card |
| `state.report.triage` | `responder`, `headline`, `reason`, `earthResponseMin` | Main "what to do" block |
| `state.report.readiness` | `GO / CAUTION / NO_GO` for spacewalk and landing | Readiness screen |
| `state.report.plan` | What to measure in the next 24 h, minutes saved | Task list |
| `state.report.game` | Points, level, streak, badges, daily mission | Profile header |
| `state.report.spaceWeather` | `QUIET/WATCH/WARNING` + data source | Space weather banner |
| `state.report.radiation` | Dose, remaining to 600 mSv, days left | Radiation budget |
| `state.environment`, `state.environments` | Current and all environments | ISS / Moon / Mars switch |
| `state.benchmark` | Self-validation report (`summary()`) | Validation screen |

### Example: triage block and early-warning banner
```kotlin
@Composable
fun TriageCard(state: HomeUiState) {
    val t = state.report.triage
    Card {
        Text(t.headline, style = MaterialTheme.typography.titleLarge)
        Text(t.reason)
        t.earthResponseMin?.let { Text("Earth can answer in ~${it.roundToInt()} min") }
    }
    state.report.alerts.firstOrNull()?.let { alert ->
        Text("⚠ ${alert.message}")
    }
}
```

## 4. Engine output for the demo astronaut (day 180, Earth–Mars transit)

From `DemoScenarioTest`, exactly what the screen gets:

- **Systems:** heart and vessels yellow, radiation yellow, the rest green.
- **Early warnings:** systolic blood pressure is still **in range** (129.5 < 130) but shifted +4.3 SD of the personal variation and drifting for 18 days. Same for diastolic pressure and heart rate.
- **Pattern:** "Cardiovascular strain" with evidence (pressure +10 %, heart rate +15 % vs baseline).
- **Triage:** "Talk to the crew medical officer": 2 systems need attention and a pattern was found.
- **Readiness:** spacewalk CAUTION, landing CAUTION.
- **Radiation:** 361 of 600 mSv; at 1.84 mSv/day that leaves ~130 days.
- **Plan:** 6 measurements today (~7 min), ~8 min saved vs "everything every day".
- **Space weather:** real DONKI data for today.

Defense scenario: with a red value, switch the environment from Mars to ISS. The triage decision changes from "onboard protocol" to "contact the flight surgeon".

## 5. Engine self-validation

`EngineBenchmark` replays the 180-day demo mission as if the engine ran every morning:

> Replayed 180 days: **caught 10 of 14 events, 5 before the threshold (mean lead 16.6 days), 6 false alarm(s).**

How to read it honestly:
- Gradual changes (blood pressure, heart rate, dose) are caught **7–30 days** before the threshold.
- Sudden events (a week of poor sleep, 3 days of fever) cannot be predicted; they are caught on the day. That is expected.
- 6 false alarms across 9 indicators over 180 days.
- The data is synthetic; metrics must be re-checked on real data.

Methodology is in the `EngineBenchmark` KDoc. Run: `./gradlew testDebugUnitTest --tests '*EngineBenchmarkTest*' -i`.

## 6. Algorithms and parameters

All numbers live in `assets/engine_rules.json` and change without code.

| Part | Method | Parameters | Source |
|---|---|---|---|
| Status | `bands` ranges + % deviation from personal baseline, worst wins | `indicators.json` | AHA, ADA, AASM, HRP |
| Personal baseline | Mean of the first 3 readings, or set manually | `BaselineCalculator.FIRST_READINGS` | — |
| EWMA | z = λx + (1−λ)z, alert outside L·σ·√(λ/(2−λ)) | λ = 0.2, L = 3.0 | Roberts 1959; Lucas & Saccucci 1990 |
| CUSUM | Tabular CUSUM, restarted when an episode ends | k = 0.5σ, h = 5σ | Page 1954 |
| Trend | OLS over 14 days, \|t\| ≥ 3, crossing forecast within 30 days | `trend*` | — |
| σ | Spread of the first 14 readings (personal, not population) | `baselineWindow` | — |
| Triage | Earth response = 2 × delay + 10 min handling, compared with the response window (red 30 min) | `triage` | **team assumption, pending science review** |
| Space weather | SEP within 48 h → WARNING; fast CME (≥1000 km/s) within 72 h or X-class flare → WATCH | `spaceWeather` | NASA DONKI, NASA-STD-3001 |

## 7. Extending with AI

Before any task the agent reads `CLAUDE.md`, this file and `docs/DATA_CONTRACT.md`. Claude Code also has the `health-engine` skill.

**New pattern rule (no code).** Add an object to `patterns` in `engine_rules.json`. Conditions: `minStatus`, `statusIn`, `minDeviationPct`, `maxDeviationPct`, `hasAlert`.
> Prompt: "Add a 'dehydration' pattern to engine_rules.json: heart rate ≥ +10 % vs baseline and blood pressure below normal. Source: HRP. Run the tests."

**New indicator.** Add it to `indicators.json` (and optionally `schedule.intervalDays`). Everything else picks it up.
> Prompt: "Add an SpO2 indicator (%, system cardiovascular) with thresholds and a source, measured daily. Check AssetContractTest."

**New screen on engine data.**
> Prompt for @harvio1: "Build a Readiness screen: for each `state.report.readiness` a card with the GO/CAUTION/NO_GO verdict and its reasons. Use only HomeUiState, do not duplicate logic."

**New engine logic.** New object in `domain/` → result in `EngineReport` → call in `HealthEngine.analyze` → unit test in `app/src/test/`.
> Prompt: "Add PVT logic in domain/: a class that takes reaction times and computes the mean, lapses > 500 ms and false starts. Cover it with tests."

**Rules:** do not change the JSON format without the team lead, never invent medical numbers (`verified: false` + `TODO(science)`), run `./gradlew testDebugUnitTest assembleDebug`.

## 8. Limitations (stated openly to the judges)
- Demo astronaut data is synthetic. Thresholds and rules stay `verified: false` until the science review.
- The triage response window is a team assumption.
- Decision support and training, not a medical device.
- The DONKI flare feed (FLR) currently returns 404; SEP and CME are used. The code already handles FLR when the feed returns.
