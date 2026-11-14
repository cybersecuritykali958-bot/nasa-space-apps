# Scientific basis

Owner: @LoranSent. The judges are scientists, so **every number in the app needs a source**, and `verified: true` in the JSON files is set only after checking.

## Verification status
| Indicator / parameter | Current value | Source | Verified |
|---|---|---|---|
| Systolic / diastolic blood pressure | 90–130 / 60–80 normal | AHA | ☐ |
| Resting heart rate | 50–100 normal, baseline deviation 15/30 % | HRP + clinical | ☐ |
| Fasting glucose | 70–100 normal, ≥126 red | ADA | ☐ |
| Body temperature | 36.0–37.3 normal, ≥38 red | clinical | ☐ |
| Sleep | ≥7 h normal, <6 red | Watson 2015 (AASM/SRS) | ☐ |
| Mood | 1–5 scale | HRP 5 Hazards | ☐ |
| PVT | <350 ms normal, deviation 15/30 % | Basner 2015 (Cognition) | ☐ thresholds to refine |
| Career dose | 600 mSv limit, yellow from 300, red from 540 | NASA-STD-3001 Vol. 1 | ☐ |
| ISS dose | ~0.5 mSv/day (real range ~0.3–0.8) | HRP | ☐ |
| Lunar surface dose | 1.37 mSv/day | Zhang 2020, Chang'e-4 LND | ☐ |
| Earth–Mars transit | 1.84 mSv/day | Zeitlin 2013, MSL RAD | ☐ |
| Mars surface | 0.64 mSv/day | Hassler 2014, Curiosity RAD | ☐ |
| Bone loss | ~1–1.5 % per month | NASA HRP | ☐ for Digital Twin |
| CSA radiation | Monitoring radiation for astronaut health (2026) | CSA | ☐ |

## Engine methods
| Method | Parameters | Source | Verified |
|---|---|---|---|
| CUSUM | k = 0.5σ, h = 5σ | Page 1954, doi:10.1093/biomet/41.1-2.100 | ☐ |
| EWMA | λ = 0.2, L = 3.0 | Roberts 1959, doi:10.1080/00401706.1959.10489860; Lucas & Saccucci 1990, doi:10.1080/00401706.1990.10484583 | ☐ |
| Triage response window | red 30 min, yellow 24 h, ground handling 10 min | team assumption | ☐ needs a source or rationale |
| Space weather | SEP within 48 h, fast CME ≥1000 km/s within 72 h | NASA DONKI | ☐ |
| Patterns and GO / NO-GO | `engine_rules.json` | see `sources` on each rule | ☐ |

**Self-validation on the demo mission:** 10 of 14 events caught, 5 before the threshold (mean lead 16.6 days), 6 false alarms over 180 days (`docs/BACKEND.md`, section 5).

## Challenge resources
- NASA: [5 Hazards](https://www.nasa.gov/hrp/hazards/), [Risks of Human Spaceflight](https://www.nasa.gov/hrp/risks/), [Ongoing HRP Research](https://www.nasa.gov/hrp/ongoing-hrp-research/).
- CSA: [Monitoring radiation for astronaut health](https://www.asc-csa.gc.ca/eng/news/articles/2026/2026-02-26-monitoring-radiation-astronaut-health.asp), [How does radiation affect the human body in space?](https://www.asc-csa.gc.ca/eng/astronauts/space-medicine/radiation.asp); Why Do We Conduct Science Experiments in Space?

## Still needed from science
1. Go through both tables: check numbers and URLs, set `verified: true`.
2. Triage response windows: a source or a written rationale.
3. Digital Twin formulas (bone, muscle, dose) with sources, hand them to @jarororoor.
4. A "Limitations" paragraph for the submission: synthetic data, population thresholds, not a medical device.
5. Answers to likely scientist questions (in `docs/PITCH.md`).
