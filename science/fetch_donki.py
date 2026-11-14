"""Fetch recent NASA DONKI space weather events into the bundled offline cache.

The app fetches DONKI live when it has network, and falls back to this file otherwise,
so the demo always shows real NASA data.

Output: android/app/src/main/assets/donki_cache.json (format: docs/DATA_CONTRACT.md).
Key: NASA_API_KEY env var, or NASA_API_KEY in android/local.properties. Never commit the key.

Usage: python science/fetch_donki.py [--days 365]
"""

from __future__ import annotations

import argparse
import json
import os
import urllib.error
import urllib.request
from datetime import date, datetime, timedelta, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "android" / "app" / "src" / "main" / "assets" / "donki_cache.json"
API = "https://api.nasa.gov/DONKI"
MAX_RANGE_DAYS = 60  # DONKI rejects longer ranges
EVENT_TYPES = ("SEP", "CME", "FLR")
FAST_CME_KMS = 1000  # matches spaceWeather.cmeFastSpeedKms in engine_rules.json


def api_key() -> str:
    if key := os.environ.get("NASA_API_KEY"):
        return key
    props = ROOT / "android" / "local.properties"
    if props.exists():
        for line in props.read_text(encoding="utf-8").splitlines():
            if line.startswith("NASA_API_KEY="):
                return line.split("=", 1)[1].strip()
    return "DEMO_KEY"


def iso(t: str) -> str:
    """DONKI uses 2026-10-16T06:09Z; normalize to seconds precision."""
    return datetime.strptime(t, "%Y-%m-%dT%H:%MZ").strftime("%Y-%m-%dT%H:%M:00Z")


def get(kind: str, start: date, end: date, key: str) -> list[dict]:
    url = f"{API}/{kind}?startDate={start}&endDate={end}&api_key={key}"
    try:
        with urllib.request.urlopen(url, timeout=30) as resp:
            body = resp.read().decode("utf-8")
    except urllib.error.HTTPError as e:
        print(f"  {kind} {start}..{end}: HTTP {e.code}, skipped")
        return []
    return json.loads(body) if body.strip() else []


def normalize(kind: str, raw: dict) -> dict | None:
    if kind == "SEP":
        instruments = ", ".join(i.get("displayName", "") for i in raw.get("instruments") or [])
        return {"type": "SEP", "id": raw["sepID"], "time": iso(raw["eventTime"]),
                "detail": f"Solar energetic particles ({instruments})" if instruments else "Solar energetic particles",
                "link": raw.get("link", "")}
    if kind == "CME":
        speeds = [a["speed"] for a in raw.get("cmeAnalyses") or [] if a.get("speed")]
        speed = max(speeds) if speeds else None
        return {"type": "CME", "id": raw["activityID"], "time": iso(raw["startTime"]),
                "detail": f"Coronal mass ejection, {speed:.0f} km/s" if speed else "Coronal mass ejection",
                "speedKms": speed, "link": raw.get("link", "")}
    if kind == "FLR":
        cls = raw.get("classType")
        return {"type": "FLR", "id": raw["flrID"], "time": iso(raw["peakTime"] or raw["beginTime"]),
                "detail": f"Solar flare {cls}" if cls else "Solar flare", "flareClass": cls,
                "link": raw.get("link", "")}
    return None


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--days", type=int, default=365)
    args = parser.parse_args()

    key = api_key()
    end = datetime.now(timezone.utc).date()
    events: dict[str, dict] = {}
    chunk_end = end
    while chunk_end > end - timedelta(days=args.days):
        chunk_start = max(chunk_end - timedelta(days=MAX_RANGE_DAYS - 1), end - timedelta(days=args.days))
        for kind in EVENT_TYPES:
            for raw in get(kind, chunk_start, chunk_end, key):
                ev = normalize(kind, raw)
                if ev:
                    events[ev["id"]] = ev
        chunk_end = chunk_start - timedelta(days=1)

    # Keep the app light: every SEP and flare, recent CMEs, and only fast CMEs further back.
    recent = (end - timedelta(days=MAX_RANGE_DAYS)).isoformat()
    kept = [
        e for e in events.values()
        if e["type"] != "CME" or e["time"] >= recent or (e.get("speedKms") or 0) >= FAST_CME_KMS
    ]

    out = {
        "version": 1,
        "fetchedAt": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "source": "NASA DONKI",
        "events": sorted(kept, key=lambda e: e["time"]),
    }
    OUT.write_text(json.dumps(out, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    counts = {k: sum(e["type"] == k for e in out["events"]) for k in EVENT_TYPES}
    print(f"wrote {OUT.relative_to(ROOT)}: {counts}")


if __name__ == "__main__":
    main()
