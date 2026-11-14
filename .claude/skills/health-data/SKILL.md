---
name: health-data
description: Where CrewPulse science data lives, its JSON format and how to regenerate it. Use for any work with indicators, environments, risks or demo mission data.
---

# Health data
- Files: `android/app/src/main/assets/` (`indicators.json`, `environments.json`, `risks.json`, `demo_mission.json`).
- Format: `docs/DATA_CONTRACT.md`; Kotlin models: `data/model/Contract.kt`. Change both together, only with team lead approval.
- Regenerate demo data: `python science/generate_demo_mission.py` (never edit `demo_mission.json` by hand).
- Sources and verification status: `docs/SCIENCE.md`. Set `verified: true` only after checking a source.
- The app is offline-first: everything needed for the demo is bundled in assets.
