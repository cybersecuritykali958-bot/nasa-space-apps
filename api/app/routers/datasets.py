"""Prepared datasets from data/processed/, served as-is (shared data contract with web/)."""

import json

from fastapi import APIRouter, HTTPException

from app.core.config import get_settings

router = APIRouter(prefix="/api/datasets", tags=["datasets"])


@router.get("")
def list_datasets() -> list[str]:
    """Names of available datasets (file names without the .json extension)."""
    return sorted(p.stem for p in get_settings().data_dir.glob("*.json"))


@router.get("/{name}")
def get_dataset(name: str) -> dict:
    """Contents of data/processed/<name>.json."""
    data_dir = get_settings().data_dir.resolve()
    path = (data_dir / f"{name}.json").resolve()
    if data_dir not in path.parents or not path.is_file():
        raise HTTPException(status_code=404, detail="Dataset not found")
    return json.loads(path.read_text(encoding="utf-8"))
