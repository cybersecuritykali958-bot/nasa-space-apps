"""Entry point: `uvicorn main:app --reload` (run from api/)."""

from app.main import create_app

app = create_app()
