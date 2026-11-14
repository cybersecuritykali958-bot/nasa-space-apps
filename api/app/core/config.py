"""Application settings, read once from environment variables (and the repo-root .env)."""

import os
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path

from dotenv import load_dotenv

API_DIR = Path(__file__).resolve().parents[2]
REPO_DIR = API_DIR.parent

load_dotenv(REPO_DIR / ".env")


@dataclass(frozen=True)
class Settings:
    app_name: str
    version: str
    cors_origins: list[str]
    database_url: str
    data_dir: Path
    nasa_api_key: str
    anthropic_api_key: str


@lru_cache
def get_settings() -> Settings:
    default_db = f"sqlite:///{API_DIR / 'var' / 'app.db'}"
    return Settings(
        app_name="Astronaut Health Monitor API",
        version="0.1.0",
        cors_origins=[
            o.strip()
            for o in os.getenv("CORS_ORIGINS", "http://localhost:3000").split(",")
            if o.strip()
        ],
        database_url=os.getenv("DATABASE_URL", default_db),
        data_dir=Path(os.getenv("DATA_DIR", REPO_DIR / "data" / "processed")),
        nasa_api_key=os.getenv("NASA_API_KEY", ""),
        anthropic_api_key=os.getenv("ANTHROPIC_API_KEY", ""),
    )
