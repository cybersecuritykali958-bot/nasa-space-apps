"""Service health endpoint."""

from fastapi import APIRouter

from app.core import db
from app.core.config import get_settings

router = APIRouter(tags=["system"])


@router.get("/health")
def health() -> dict:
    """Liveness check.

    `status` stays "ok" while the process is up; `database` reports SQLite reachability.
    """
    settings = get_settings()
    return {
        "status": "ok",
        "version": settings.version,
        "database": "ok" if db.ping() else "unavailable",
    }
