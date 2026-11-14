"""Reference Digital Twin model for CrewPulse: bone, muscle, radiation dose.

This is the source of truth for the formulas. The Kotlin port in domain/ must reproduce
the test vectors printed by this script. Every constant has a source; see docs/SCIENCE.md,
section "Digital Twin".

Model, per mission day, for a phase in an environment with gravity g (in Earth g):
    unloading u = 1 - g                       (Earth: no loss; ISS / transit: full loss)
    bone  BMD_hip[%]   -= rate_bone(a) * u / 30.44
    muscle strength[%] *= exp(-k_muscle(a) * u)
    dose  D[mSv]       += radiationMsvPerDay (+ solar particle events)
where a in [0, 1] is resistive-exercise adherence (1 = full ARED protocol).
After landing, bone recovers as L(t) = L0 * 0.5 ** (t / HL)   (Sibonga et al. 2007).

Usage: python science/digital_twin.py      (prints test vectors, writes nothing)
"""

from __future__ import annotations

import json
import math
from dataclasses import dataclass
from pathlib import Path

ASSETS = Path(__file__).resolve().parent.parent / "android" / "app" / "src" / "main" / "assets"

DAYS_PER_MONTH = 30.44

# --- Bone: hip areal BMD, % of pre-flight -------------------------------------------------
# NASA HRP evidence report: 1-1.5 %/month at weight-bearing sites without adequate countermeasures.
BONE_RATE_NO_CM = 1.25  # %/month, midpoint of 1-1.5
# NASA/SP-20250000273 (2025): ARED era, average 0.5-1.0 %/month; Smith et al. 2012: ARED + nutrition
# kept hip BMD near pre-flight. We use the lower bound as the full-adherence case.
BONE_RATE_ARED = 0.5  # %/month
# Sibonga et al. 2007: 50 % recovery time 255 days at the trochanter (slowest site, conservative).
BONE_RECOVERY_HALF_LIFE_DAYS = 255.0
# NASA-STD-3001 Vol. 1 Rev B: hip and spine >= 95 %, femoral neck >= 90 % of pre-mission BMD.
BONE_GREEN_MIN = 95.0
BONE_RED_BELOW = 90.0

# --- Muscle: lower-body strength, % of pre-flight -----------------------------------------
# NASA/SP-20250000273: ARED era, lower-body strength -9 % after a ~6-month ISS mission.
MUSCLE_LOSS_ARED_6MO = 0.09
# Trappe et al. 2009: calf peak power -32 % after 6 months with pre-ARED exercise (worst-case proxy).
MUSCLE_LOSS_LOW_6MO = 0.32
MISSION_6MO_DAYS = 180.0
K_MUSCLE_ARED = -math.log(1 - MUSCLE_LOSS_ARED_6MO) / MISSION_6MO_DAYS  # per day
K_MUSCLE_LOW = -math.log(1 - MUSCLE_LOSS_LOW_6MO) / MISSION_6MO_DAYS  # per day
# NASA-STD-3001 Vol. 1 Rev B: in-mission strength >= 80 % of baseline. Yellow at half the margin.
MUSCLE_GREEN_MIN = 90.0
MUSCLE_RED_BELOW = 80.0

# --- Radiation -----------------------------------------------------------------------------
# NASA-STD-3001 Vol. 1: career effective dose < 600 mSv (set at 3 % REID, 35-year-old female).
DOSE_LIMIT_MSV = 600.0
REID_AT_LIMIT_PCT = 3.0
DOSE_YELLOW_FRACTION = 0.5  # ALARA, team rule (see docs/SCIENCE.md)
DOSE_RED_FRACTION = 0.9

# LeBlanc et al. 2000: human data cover flights of 4-14.4 months. Beyond that we extrapolate.
DATA_LIMIT_DAYS = 438


@dataclass(frozen=True)
class Phase:
    environment_id: str
    days: int


@dataclass(frozen=True)
class TwinState:
    day: int
    environment_id: str
    bone_pct: float
    muscle_pct: float
    dose_msv: float
    bone_status: str
    muscle_status: str
    dose_status: str
    reid_pct: float
    days_to_dose_limit: float | None
    extrapolated: bool


def _lerp(low: float, high: float, adherence: float) -> float:
    a = min(max(adherence, 0.0), 1.0)
    return low + (high - low) * a


def bone_rate_pct_per_month(adherence: float) -> float:
    return _lerp(BONE_RATE_NO_CM, BONE_RATE_ARED, adherence)


def muscle_k_per_day(adherence: float) -> float:
    return _lerp(K_MUSCLE_LOW, K_MUSCLE_ARED, adherence)


def status_higher_is_better(value: float, green_min: float, red_below: float) -> str:
    if value < red_below:
        return "red"
    return "green" if value >= green_min else "yellow"


def dose_status(dose_msv: float) -> str:
    if dose_msv >= DOSE_LIMIT_MSV * DOSE_RED_FRACTION:
        return "red"
    return "yellow" if dose_msv >= DOSE_LIMIT_MSV * DOSE_YELLOW_FRACTION else "green"


def reid_pct(dose_msv: float) -> float:
    """Linear (no-threshold) approximation anchored at the NASA limit: 600 mSv -> 3 % REID."""
    return REID_AT_LIMIT_PCT * dose_msv / DOSE_LIMIT_MSV


def bone_after_landing(bone_at_landing_pct: float, days_after_landing: float) -> float:
    loss = 100.0 - bone_at_landing_pct
    return 100.0 - loss * 0.5 ** (days_after_landing / BONE_RECOVERY_HALF_LIFE_DAYS)


def simulate(
    phases: list[Phase],
    environments: dict[str, dict],
    adherence: float = 1.0,
    start_dose_msv: float = 0.0,
    extra_dose_by_day: dict[int, float] | None = None,
) -> list[TwinState]:
    """Day-by-day state. Day 0 is pre-flight; day N is the end of mission day N."""
    extra = extra_dose_by_day or {}
    bone, muscle, dose = 100.0, 100.0, start_dose_msv
    rate_bone = bone_rate_pct_per_month(adherence)
    k_muscle = muscle_k_per_day(adherence)
    states: list[TwinState] = []
    day = 0
    for phase in phases:
        env = environments[phase.environment_id]
        unloading = 1.0 - env["gravityG"]
        for _ in range(phase.days):
            day += 1
            bone -= rate_bone * unloading / DAYS_PER_MONTH
            muscle *= math.exp(-k_muscle * unloading)
            dose += env["radiationMsvPerDay"] + extra.get(day, 0.0)
            rate = env["radiationMsvPerDay"]
            to_limit = max(DOSE_LIMIT_MSV - dose, 0.0) / rate if rate > 0 else None
            states.append(
                TwinState(
                    day=day,
                    environment_id=phase.environment_id,
                    bone_pct=bone,
                    muscle_pct=muscle,
                    dose_msv=dose,
                    bone_status=status_higher_is_better(bone, BONE_GREEN_MIN, BONE_RED_BELOW),
                    muscle_status=status_higher_is_better(muscle, MUSCLE_GREEN_MIN, MUSCLE_RED_BELOW),
                    dose_status=dose_status(dose),
                    reid_pct=reid_pct(dose),
                    days_to_dose_limit=to_limit,
                    extrapolated=day > DATA_LIMIT_DAYS,
                )
            )
    return states


def load_environments() -> dict[str, dict]:
    data = json.loads((ASSETS / "environments.json").read_text(encoding="utf-8"))
    return {e["id"]: e for e in data["environments"]}


SCENARIOS: dict[str, list[Phase]] = {
    "iss_6mo": [Phase("iss", 180)],
    "mars_transit_180": [Phase("mars_transit", 180)],
    "mars_mission": [Phase("mars_transit", 180), Phase("mars_surface", 500), Phase("mars_transit", 180)],
    "two_years_transit": [Phase("mars_transit", 730)],
}


def _row(name: str, adherence: float, s: TwinState) -> str:
    left = "-" if s.days_to_dose_limit is None else f"{s.days_to_dose_limit:.0f}"
    return (
        f"{name:18} a={adherence:<4} day={s.day:<4} bone={s.bone_pct:6.2f}% {s.bone_status:6} "
        f"muscle={s.muscle_pct:6.2f}% {s.muscle_status:6} dose={s.dose_msv:7.1f} mSv {s.dose_status:6} "
        f"REID={s.reid_pct:4.2f}% to_limit={left:>4} d extrapolated={s.extrapolated}"
    )


def main() -> None:
    envs = load_environments()
    print("Test vectors (end of each scenario):")
    for name, phases in SCENARIOS.items():
        for adherence in (1.0, 0.5, 0.0):
            print(_row(name, adherence, simulate(phases, envs, adherence)[-1]))
    end = simulate(SCENARIOS["iss_6mo"], envs, 1.0)[-1].bone_pct
    for days_after in (0, 255, 365, 1095):
        print(f"bone after ISS landing, a=1.0, day+{days_after:<4}: {bone_after_landing(end, days_after):6.2f}%")


if __name__ == "__main__":
    main()
