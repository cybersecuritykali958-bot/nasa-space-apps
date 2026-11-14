# Science base

Owner: @LoranSent. The judges are scientists, so **every number in the app must have a source**. In `indicators.json`, `verified: true` is set only after the source has been checked.

## Verification status
| Indicator / parameter | Current value | Source | Verified |
|---|---|---|---|
| Systolic / diastolic blood pressure | 90–130 / 60–80 normal; 130 and 80 = stage 1, 140 and 90 = stage 2 | AHA (categories confirmed, unchanged in the 2025 guideline) | ☑ AHA calls 120–129 "elevated"; kept green on purpose to avoid false alarms |
| Resting heart rate | ≤40 red, 41–49 yellow, 50–99 normal, 100–130 yellow, ≥131 red; deviation from baseline 15/30 % | AHA (60–100, trained people down to ~40), NEWS2 (≤40 and ≥131 = 3 points), Buchheit 2014 (measurement error ~10 %) | ◐ see "Rationale", item 1 |
| Fasting glucose | <54 red, 54–70 yellow, 70–100 normal, 100–125 yellow, ≥126 red | ADA Diagnosis + ADA Standards of Care 2024 (level 1 and level 2 hypoglycemia) | ☑ |
| Body temperature | ≤35.0 red, 35.1–35.6 yellow, 35.7–37.3 normal, 37.4–37.9 yellow, ≥38 red | Obermeyer 2017 BMJ (95 % of healthy adults 35.7–37.3), CDC (fever ≥38), NEWS2 and Brown 2012 (≤35.0), Stahn 2017 (astronaut core temperature +1 °C) | ☑ |
| Sleep | ≥7 h normal, <6 red | Watson 2015 (AASM/SRS, ≥7 h), Van Dongen 2003 (≤6 h for two weeks = up to 2 nights without sleep), Barger 2014 (~6 h on the ISS) | ☑ |
| Mood | 1–5 scale | HRP 5 Hazards | ✖ no validated single-item scale, see "Limitations", item 1 |
| PVT | <355 ms normal, 355–499 yellow, ≥500 red; deviation 15/30 % | Basner 2011 PVT-B (lapse ≥355 ms), Basner & Dinges 2011 (lapse ≥500 ms), Basner 2015 | ✖ metric choice and 15/30 % need a team decision, see "Limitations", item 2 |
| Career dose | 600 mSv limit, yellow from 300, red from 540 | NASA OCHMO brief, NASA-STD-3001 Vol. 1 [V1 4030], ALARA principle | ◐ see "Rationale", item 2 |
| ISS dose | ~0.5 mSv/day | Simonsen 2025 NASA NTRS (0.2–0.5 mSv/day), Berger 2017 DOSIS 3D (up to 0.65 mSv/day in Columbus) | ☑ upper end of the NASA range, a conservative estimate |
| Lunar surface dose | 1.37 mSv/day | Zhang 2020, Chang'e-4 LND | ☑ DOI checked |
| Earth–Mars transit | 1.84 mSv/day | Zeitlin 2013, MSL RAD | ☑ DOI checked |
| Mars surface | 0.64 mSv/day | Hassler 2014, Curiosity RAD | ☑ DOI checked |
| Bone loss | 1.25 %/month without countermeasures, 0.5 %/month with ARED | NASA HRP Evidence Report, NASA/SP-20250000273, Smith 2012 | ☑ see "Digital Twin" |
| CSA radiation | Monitoring radiation for astronaut health (2026-02-26) | CSA | ☑ link replaced with the exact article |
| Camera pulse accuracy | contact PPG: mean absolute error 2–4.5 bpm vs ECG | Coppetti 2017, Eur J Prev Cardiol, https://doi.org/10.1177/2047487317702044 | ☑ for the defense Q&A |
| `risks.json` | 9 risks, each linked to its own Human Research Roadmap page; names and summaries follow the official HRP risk statements; `fitness` added for the Digital Twin muscle model | NASA Human Research Roadmap | ☑ checked 2026-11-14 |

Legend: ☑ value taken from the source · ◐ derived from the source by the rule below · ✖ no source, this is a limitation.

## Rationale (◐)
1. **Heart rate.** The red limits are the NEWS2 3-point bands (≤40, ≥131). Green 50–99 is the AHA normal range of 60–100 extended down to 50, because trained people, astronauts included, have a lower resting heart rate. Deviation from the personal baseline: the day-to-day measurement error of resting heart rate is ~10 % (Buchheit 2014). Yellow starts at 15 % (≈1.5 × error), red at 30 % (≈3 × error), i.e. a change larger than measurement noise.
2. **Dose.** The 600 mSv career limit comes from NASA-STD-3001 [V1 4030]. The standard also requires doses to be kept as low as reasonably achievable (ALARA). Yellow therefore starts at 50 % of the limit (time to reduce exposure) and red at 90 % (last margin before the limit). The fractions themselves are a team decision.

## Limitations (✖), to state openly at the defense
1. **Mood 1–5.** This is a screening question, not a validated scale. Isolation studies (Antarctica) use multi-item instruments: the Mental Health Checklist (23 items, Alfano), PANAS, POMS. The 2.5 and 3.5 cut-offs are scale midpoints. Future work: replace with a short validated scale.
2. **PVT mean reaction time.** Basner & Dinges 2011 showed that mean RT is less sensitive to sleep loss than the number of lapses (≥355 ms on the PVT-B) and response speed 1/RT. The 355 and 500 ms cut-offs are lapse thresholds applied to mean RT; the 15/30 % deviation has no source. Switching to lapse count would be more correct, but it changes the PVT screen and the demo generator, so it needs a decision from the team lead, @jarororoor and @interpolfuckyou.
3. Thresholds are population-based (terrestrial clinical) values, not spaceflight standards; demo data are synthetic; the app is not a medical device.

## Digital Twin (for @jarororoor)

Reference implementation: `science/digital_twin.py`, which prints test vectors. The Kotlin port in `domain/` must reproduce them (unit test). For now, hard-code the constants in `domain/` with a source comment; moving them into JSON is a contract change and goes through the team lead.

**Inputs:** mission phases (`environmentId`, number of days) → `gravityG` and `radiationMsvPerDay` from `environments.json`; resistive-exercise adherence `a` from 0 to 1 (1 = full ARED protocol); starting dose. Solar particle events from `demo_mission.events` are added to the dose on their day.

**Every day of a phase** (unloading `u = 1 − gravityG`: no loss on Earth, full loss on the ISS and in transit, 62 % on Mars):

| Quantity | Formula | Constants | Source |
|---|---|---|---|
| Bone, hip BMD, % of pre-flight | `bone −= r(a) · u / 30.44` | `r(1) = 0.5`, `r(0) = 1.25` %/month, `r(a)` linear in between | NASA HRP Evidence Report (1–1.5 %/month without countermeasures); NASA/SP-20250000273 (ARED era 0.5–1.0 %/month); Smith 2012 (ARED + nutrition preserve BMD) |
| Muscle, leg strength, % | `muscle ·= exp(−k(a) · u)` | `k(1) = −ln(0.91)/180`, `k(0) = −ln(0.68)/180` per day, `k(a)` linear | NASA/SP-20250000273 (ARED: −9 % leg strength over ~6 months); Trappe 2009 (−32 % calf peak power over 6 months, worst case) |
| Dose, mSv | `dose += radiationMsvPerDay` (+ solar events) | from `environments.json` | see the table above |
| Days to limit | `(600 − dose) / radiationMsvPerDay` | 600 mSv | NASA-STD-3001 Vol. 1 |
| REID, % | `3 · dose / 600` | 600 mSv ↔ 3 % REID | NASA OCHMO: the limit was set at 3 % risk of exposure-induced cancer death (35-year-old female); linear approximation |
| Bone after landing | `100 − L0 · 0.5^(t / 255)` | `L0` = loss on landing day, 50 % recovery time 255 days | Sibonga 2007 (trochanter, the slowest site) |

**Statuses:**

| Quantity | Green | Yellow | Red | Source |
|---|---|---|---|---|
| Bone | ≥ 95 % | 90–95 % | < 90 % | NASA-STD-3001 Vol. 1 Rev B: hip and spine ≥ 95 %, femoral neck ≥ 90 % |
| Muscle | ≥ 90 % | 80–90 % | < 80 % | NASA-STD-3001 Vol. 1 Rev B: strength ≥ 80 %; yellow at half the margin is a team rule |
| Dose | < 300 | 300–540 | ≥ 540 mSv | same as `indicators.json` |

**`extrapolated` flag:** after 438 days (14.4 months) there are no human data (LeBlanc 2000: flights of 4–14.4 months). From that day on, the screen should show "extrapolation" and a dashed line instead of a solid one.

**Test vectors** (`python science/digital_twin.py`):

| Scenario | a | Day | Bone | Muscle | Dose |
|---|---|---|---|---|---|
| ISS 180 days | 1.0 | 180 | 97.04 % 🟢 | 91.00 % 🟢 | 90.0 mSv 🟢 |
| ISS 180 days | 0.0 | 180 | 92.61 % 🟡 | 68.00 % 🔴 | 90.0 mSv 🟢 |
| Mars transit 180 days (demo) | 1.0 | 180 | 97.04 % 🟢 | 91.00 % 🟢 | 331.2 mSv 🟡 |
| Mars: 180 + 500 on the surface + 180 | 1.0 | 860 | 88.99 % 🔴 | 70.40 % 🔴 | 982.4 mSv 🔴 |
| 2 years in transit | 1.0 | 730 | 88.01 % 🔴 | 68.22 % 🔴 | 1343.2 mSv 🔴 |

**Model check against real data:** ISS 6 months with ARED → bone −3 %; Scott 2023 crews averaged −3 % at the femoral neck (NASA/SP-20250000273). The Mars mission dose of 982 mSv agrees with MSL RAD: the round-trip transit alone is ~660 mSv (Zeitlin 2013).

**Model assumptions (to state at the defense):**
1. Unloading is linear in gravity, `1 − g`. There are no human data at 0.38 g; in rats, bone loss grows as loading decreases (Ko 2020, 20/40/70 % of body weight).
2. In-flight bone loss is linear, as in LeBlanc 2000 up to 14.4 months; whether it plateaus later is unknown.
3. Muscle loss is exponential so that it slows down and never goes below zero. The worst case uses power (Trappe 2009), not strength, because there are no direct data on strength without exercise on long flights.
4. Partial gravity does not restore bone or muscle, it only slows the loss. Recovery is modeled only after landing on Earth.

## Submission text: Limitations (ready to paste)

> CrewPulse is a research prototype, not a medical device, and it does not replace a flight surgeon. The demo astronaut is synthetic: 180 days of data generated from published rates of change around a fixed baseline, and labelled as synthetic in the app. Most thresholds are terrestrial clinical references (AHA, ADA, CDC, NEWS2, AASM); spaceflight-specific limits are used where NASA publishes them (NASA-STD-3001 for radiation dose, bone density and muscle strength). Every threshold in the app links to its source, and values that we derived ourselves (for example the 15 % / 30 % deviation from personal baseline) are marked as such. The mood check-in is a screening question, not a validated psychological scale, and the reaction-time test reports mean response time, which is less sensitive to sleep loss than lapse counts. The Digital Twin is a simple deterministic model: it assumes losses scale linearly with gravity, has no human data for Moon or Mars gravity, and extrapolates beyond 14.4 months, the longest period covered by the bone data we use. It shows trends and orders of magnitude, not individual predictions.

## Science highlights for slides (for @gutsonithemvp and the team lead)

Short facts for slide 7 ("NASA and partner data") and the pitch. Each one is sourced above.

1. **A Mars mission exceeds NASA's career radiation limit.** ~980 mSv for 2 × 180 days of transit + 500 days on the surface, vs a 600 mSv limit (MSL RAD: Zeitlin 2013, Hassler 2014; NASA-STD-3001).
2. **Our bone model matches ISS data.** 6 months with ARED → −3 % hip bone density; recent ISS crews averaged −3 % at the femoral neck (NASA/SP-20250000273).
3. **Earth is up to 22 minutes away.** One-way signal delay to Mars is ~3–22 minutes, so the crew has to assess itself first.
4. **Astronauts sleep ~6 hours.** ISS crews average about 6 h per night (Barger 2014; NASA HRP), and 6 h or less for two weeks impairs attention like up to two nights without sleep (Van Dongen 2003). That is why a 3-minute PVT (Basner 2011) is in the app.
5. **Every threshold has a source.** 9 indicators, 9 NASA HRP risks, 4 environments; NASA HRP, NASA-STD-3001, MSL RAD, Chang'e-4 LND, CSA, AHA, ADA, CDC, AASM, NEWS2.

## Defense Q&A (prepared by @LoranSent)

Short answers for the 5-minute defense. Every claim points to a source in this file.

| # | Question | Answer |
|---|---|---|
| 1 | Where do your thresholds come from? | Each indicator in the app shows its sources: AHA for blood pressure and heart rate, ADA for glucose, CDC and Obermeyer 2017 for temperature, AASM/SRS and Van Dongen 2003 for sleep, Basner 2011 for the PVT, NASA-STD-3001 for radiation, bone and muscle. The verification table above marks what is taken directly from a source and what we derived. |
| 2 | Why compare with a personal baseline and not only with the normal range? | A value can be "normal" for the population and still be abnormal for this astronaut. Resting heart rate varies ~10 % day to day (Buchheit 2014), so we flag changes above 15 % and 30 % of the astronaut's own baseline. The worse of the two checks wins. |
| 3 | How accurate is the camera pulse? | We use contact photoplethysmography (finger on the camera). Against ECG, contact apps had a mean absolute error of about 2–4.5 bpm, non-contact face apps 7–8 bpm (Coppetti 2017). 2–4.5 bpm is below our 15 % yellow threshold (~9 bpm at a 62 bpm baseline). |
| 4 | Are the data real? | No. The demo astronaut is synthetic, built from published rates of change, and labelled as synthetic in the app. We had no access to astronaut medical data. |
| 5 | How is this different from a fitness tracker? | It maps measurements to NASA HRP spaceflight risks, uses NASA-STD-3001 limits, gives actions that work without Earth (Mars delay 3–22 minutes one way), and every threshold links to a scientific source. |
| 6 | Your thresholds are terrestrial. Do they hold in space? | Mostly yes as safety limits, with known caveats: the fluid shift changes blood pressure regulation, and astronaut core temperature rises by about 1 °C on long missions (Stahn 2017). That is why we combine population ranges with the personal in-flight baseline. Where NASA has spaceflight limits (dose, bone, muscle) we use them. |
| 7 | Why a 3-minute reaction test? | The PVT is the standard measure of fatigue-related alertness and is part of NASA's Cognition battery used on the ISS (Basner 2015). The 3-minute PVT-B was validated against the 10-minute version with a 355 ms lapse threshold (Basner 2011). |
| 8 | Why mean reaction time and not lapses? | Honest answer: lapses are more sensitive to sleep loss (Basner & Dinges 2011). We started with mean RT for simplicity; switching to lapse count is our next step. |
| 9 | How does the Digital Twin predict bone loss, and is it validated? | Hip bone density falls 0.5 %/month with full ARED exercise and 1.25 %/month without it (NASA HRP, NASA/SP-20250000273, Smith 2012). For a 6-month ISS mission with ARED the model gives −3 %, which matches the −3 % femoral neck average reported for recent crews. Recovery after landing follows Sibonga 2007 (50 % back in ~8.5 months). |
| 10 | How do you handle Moon and Mars gravity? | We assume losses scale with unloading (1 − g). There are no human data at 0.16 g or 0.38 g; rat studies show bone loss grows as loading decreases (Ko 2020). We say this is an assumption. |
| 11 | What about radiation on a Mars mission? | NASA's career limit is 600 mSv. With MSL RAD rates, a 180-day transit is ~330 mSv, and a full mission (2 × 180 days transit + 500 days on the surface) is ~980 mSv, above the limit. The app shows days left to the limit and an approximate cancer-risk equivalent (600 mSv ≈ 3 % REID). |
| 12 | Does the phone measure radiation? | No. Dose comes from the crew dosimeter or from published daily rates for the current environment, plus solar particle events. |
| 13 | Won't the app give too many false alarms? | We chose cut-offs to avoid alarm fatigue: for example, AHA "elevated" blood pressure (120–129) stays green, and the baseline rule requires a change larger than measurement noise. On the synthetic 180-day mission, red alerts appear only during the scripted sleep-loss week (days 40–46) and the fever (days 120–121); the blood-pressure drift and the dose after the solar event show as yellow, and a few isolated yellow days come from normal day-to-day variation, such as one short night. |
| 14 | Is the mood scale validated? | No. It is a single screening question. Validated tools used in isolation studies are the Mental Health Checklist, PANAS and POMS; replacing our item with a short validated scale is future work. |
| 15 | Is this a medical device? | No. It is a research prototype for self-monitoring that supports the crew medical officer and the flight surgeon, not replaces them. |

## Resources from the challenge description
- NASA: 5 Hazards of Human Spaceflight, Risks of Human Spaceflight, Ongoing HRP Research in Space.
- CSA: Monitoring Radiation for Astronaut Health; Why Do We Conduct Science Experiments in Space?

## Remaining science tasks
1. ~~Go through the table: check numbers and URLs, set `verified: true`.~~ Done 2026-11-14, except the limitations above.
2. ~~Digital Twin formulas (bone, muscle, dose) with sources, hand over to @jarororoor.~~ Done 2026-11-14, see "Digital Twin".
3. ~~"Limitations" paragraph for the submission: synthetic data, population thresholds, not a medical device.~~ Done 2026-11-14, see "Submission text: Limitations".
4. ~~List of likely questions from scientists and the answers.~~ Done 2026-11-14, see "Defense Q&A"; `docs/PITCH.md` can link to it.
