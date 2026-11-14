# Pitch and defense

Defense: 17–18.11 at 14:00, Zoom, **5 minutes** + questions. Owners: team lead + @gutsonithemvp.

## Structure (240 seconds + demo)
1. **Hook (45 s):** "Mars. The nearest doctor is 12 minutes away, one way. The astronaut is their own doctor."
2. **Problem (60 s):** NASA HRP 5 hazards; what they do to the body; why autonomous self-assessment is needed.
3. **Solution + live demo (90 s):** check-in → body figure → early warning "blood pressure still normal, but drifting; threshold in ~7 days" → triage → switch environment Mars ↔ ISS and the decision changes → spacewalk NO-GO on a real NASA DONKI particle event.
4. **Impact (45 s):** Artemis and Mars missions, analog stations, data for HRP research, Earth use (polar stations, submarines).

## Key claims (each backed by the app)
- "We compare not only with population norms but with the astronaut's own baseline."
- "EWMA and CUSUM are classic statistical process control methods used in patient monitoring; we apply them to in-flight self-assessment."
- "The engine knows the communication delay and decides whether the crew can wait for Earth."
- "Every conclusion is explainable: you see which indicators and why. No black box."
- "We validated the engine: on the 180-day mission it caught gradual changes 7–30 days before the threshold, with 6 false alarms."

## Likely questions from scientists
| Question | Answer (prepared by @LoranSent) |
|---|---|
| Where do the thresholds come from? | _table in SCIENCE.md_ |
| Why a personal baseline and not only norms? | _…_ |
| How accurate is the camera pulse? | _…_ |
| Is the data real? | Demo astronaut data is synthetic, built on published rates and labeled as such. Space weather is real NASA DONKI data |
| How is this different from a fitness tracker? | HRP space risks, early warning against personal variation, autonomous triage with communication delay, scientific sources |
| How did you validate the early warning? | Day-by-day replay of the mission: events caught, lead time, false alarms (`docs/BACKEND.md`, section 5) |
