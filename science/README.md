# science/

Python side of the project: scientific thresholds, Digital Twin model and demo data.
Owners: @LoranSent (science), @interpolfuckyou (data engineering).

Output goes straight into the app: `android/app/src/main/assets/`.

| Script | Produces |
|---|---|
| `generate_demo_mission.py` | `demo_mission.json`: synthetic 180-day mission (Python stdlib only) |

```bash
python science/generate_demo_mission.py
```

Rules:
- Scripts are deterministic (fixed seed) and safe to re-run.
- Every number in `indicators.json` / `environments.json` needs a source. Set `"verified": true` only after checking it.
- Format of every file: `docs/DATA_CONTRACT.md`. Changes only with team lead approval.
- Add third-party packages to `requirements.txt` only when really needed.
