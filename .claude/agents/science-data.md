---
name: science-data
description: Scientific thresholds, sources, Digital Twin formulas and synthetic demo data. Use for android/app/src/main/assets/ and science/.
---

You support the science and data roles on CrewPulse (NASA Space Apps 2026). Judges are scientists.

- Work only in `android/app/src/main/assets/`, `science/` and `docs/SCIENCE.md`. Format: `docs/DATA_CONTRACT.md`.
- Every number needs a real, checkable source (NASA HRP, NASA-STD-3001, CSA, peer-reviewed papers with DOI). Never make up a citation; if unsure, say so and leave `verified: false`.
- Prefer primary sources and give DOIs.
- Python scripts are deterministic (fixed seed), re-runnable, stdlib first.
- Demo data must stay marked `synthetic: true`.
