# Submission on spaceappschallenge.org

Everything in **English**. Deadline 15.11 23:55; we submit by 23:00.

- [ ] **Project Name:** CrewPulse
- [ ] **High-Level Summary:** what we built, which problem it solves, how it answers the challenge, why it matters
- [ ] **Link to Project Demo:** **max 7 slides** (including the title) **or a video of max 30 s**. Exceeding the limit removes us from Global Judging
  - Slides: 1 team, 2 members, 3 challenge, 4 visual, 5 how it works, 6 user experience, 7 NASA / partner data
- [ ] **Link to Final Project:** public link without login (APK on GitHub Releases or Google Drive + video)
- [ ] **Detailed Description:** what it does, how it works, benefits, goals, technologies (Kotlin, Jetpack Compose, Room, Python)
- [ ] **NASA Data:** NASA DONKI (api.nasa.gov: SEP and CME → "go to shielded area" alert, spacewalk GO / NO-GO), HRP 5 Hazards and Risks (indicators linked to risks), NASA-STD-3001 (600 mSv career limit), MSL RAD (dose per environment): what, where from, how used
- [ ] **Space Agency Partner & Other Data:** CSA radiation monitoring; AHA, ADA, AASM, Basner 2015, Zhang 2020; Page 1954, Roberts 1959, Lucas & Saccucci 1990 (methods)
- [ ] **AI use:** Claude Code and Cursor: what for and how
- [ ] **Images:** .png screenshots up to 10 MB
- [ ] All links checked in incognito mode

## Summary draft
> CrewPulse is an offline-first Android app that lets astronauts run a quick self-exam, see the status of each body system at a glance and know what to do, even when Earth is up to 22 minutes away. It tracks ten indicators, from blood pressure, heart rate, sleep and reaction time to radiation dose and the WHO-5 well-being index, and compares every reading with clinical and NASA limits and with the astronaut's own baseline. Heart rate can also be measured with the phone camera. EWMA and CUSUM trend detection warn about gradual changes before a threshold is crossed. Findings are linked to nine NASA Human Research Program risks; triage takes the communication delay into account, and a compact report for the flight surgeon shows when Earth can answer. Real NASA DONKI space-weather data drives spacewalk GO / NO-GO, and a Digital Twin projects bone, muscle and radiation dose for ISS and Mars missions against NASA-STD-3001 limits and computes how much exercise is needed to stay within them. Every threshold links to its source. CrewPulse is a research prototype, not a medical device; demo astronaut data is synthetic.
