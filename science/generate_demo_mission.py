"""Generate the synthetic 180-day demo mission used by the app.

The output is clearly marked as synthetic. Trends follow published rates where we have them
(radiation dose rate from environments.json); everything else is plausible noise around a baseline
plus scripted events for the demo story.

Usage: python science/generate_demo_mission.py
"""

from __future__ import annotations

import json
import random
from pathlib import Path

ASSETS = Path(__file__).resolve().parent.parent / "android" / "app" / "src" / "main" / "assets"
OUT = ASSETS / "demo_mission.json"

DAYS = 180
SEED = 42
ENVIRONMENT_ID = "mars_transit"

BASELINE = {
    "systolic_bp": 118.0,
    "diastolic_bp": 76.0,
    "heart_rate": 62.0,
    "glucose": 88.0,
    "body_temp": 36.6,
    "sleep_hours": 7.4,
    "mood": 4.2,
    "pvt_mean_rt": 260.0,
}

NOISE = {
    "systolic_bp": 3.0,
    "diastolic_bp": 2.0,
    "heart_rate": 2.0,
    "glucose": 3.0,
    "body_temp": 0.15,
    "sleep_hours": 0.35,
    "mood": 0.3,
    "pvt_mean_rt": 12.0,
}

# Story beats shown in the demo.
POOR_SLEEP = range(40, 47)
SOLAR_EVENT_DAY = 92
SOLAR_EVENT_EXTRA_MSV = 30.0
FEVER_DAYS = range(120, 123)
BP_DRIFT_START = 150


def daily_dose_rate() -> float:
    envs = json.loads((ASSETS / "environments.json").read_text(encoding="utf-8"))["environments"]
    return next(e["radiationMsvPerDay"] for e in envs if e["id"] == ENVIRONMENT_ID)


def build() -> dict:
    rng = random.Random(SEED)
    dose_rate = daily_dose_rate()
    dose = 0.0
    days = []

    for day in range(1, DAYS + 1):
        r = {k: BASELINE[k] + rng.gauss(0, NOISE[k]) for k in BASELINE}

        if day in POOR_SLEEP:
            r["sleep_hours"] = 5.2 + rng.gauss(0, 0.3)
            r["mood"] -= 1.0
            r["pvt_mean_rt"] += 70
            r["heart_rate"] += 6

        if day in FEVER_DAYS:
            r["body_temp"] = 38.1 + rng.gauss(0, 0.2)
            r["heart_rate"] += 12

        if day >= BP_DRIFT_START:
            drift = (day - BP_DRIFT_START) / (DAYS - BP_DRIFT_START)
            r["systolic_bp"] += 16 * drift
            r["diastolic_bp"] += 7 * drift
            r["heart_rate"] += 12 * drift

        dose += dose_rate
        if day == SOLAR_EVENT_DAY:
            dose += SOLAR_EVENT_EXTRA_MSV
        r["cumulative_dose"] = dose

        r["mood"] = min(5.0, max(1.0, r["mood"]))
        days.append({"day": day, "readings": {k: round(v, 1) for k, v in r.items()}})

    return {
        "version": 1,
        "astronaut": "Demo astronaut A",
        "environmentId": ENVIRONMENT_ID,
        "synthetic": True,
        "baseline": BASELINE,
        "days": days,
        "events": [
            {"day": POOR_SLEEP.start, "type": "sleep_loss", "note": "One week of disrupted sleep after a schedule shift"},
            {"day": SOLAR_EVENT_DAY, "type": "solar_particle_event", "note": f"Solar particle event, +{SOLAR_EVENT_EXTRA_MSV:.0f} mSv"},
            {"day": FEVER_DAYS.start, "type": "fever", "note": "Short febrile episode"},
            {"day": BP_DRIFT_START, "type": "bp_drift", "note": "Gradual rise in blood pressure and resting heart rate"},
        ],
    }


def main() -> None:
    OUT.write_text(json.dumps(build(), ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print(f"wrote {OUT.relative_to(ASSETS.parents[3])}")


if __name__ == "__main__":
    main()
