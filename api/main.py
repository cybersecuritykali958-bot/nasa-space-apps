"""API проекта NASA Space Apps. Отдаёт готовые данные из data/processed/."""
import json
import os
from pathlib import Path

from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

load_dotenv(Path(__file__).resolve().parent.parent / ".env")

DATA_DIR = Path(__file__).resolve().parent.parent / "data" / "processed"

app = FastAPI(title="NASA Space Apps API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=os.getenv("CORS_ORIGINS", "http://localhost:3000").split(","),
    allow_methods=["GET"],
    allow_headers=["*"],
)


@app.get("/health")
def health() -> dict:
    """Проверка, что сервер работает."""
    return {"status": "ok"}


@app.get("/api/datasets")
def list_datasets() -> list[str]:
    """Список готовых наборов данных (имена файлов без расширения)."""
    return sorted(p.stem for p in DATA_DIR.glob("*.json"))


@app.get("/api/datasets/{name}")
def get_dataset(name: str) -> dict:
    """Содержимое набора данных из data/processed/<name>.json."""
    path = (DATA_DIR / f"{name}.json").resolve()
    if DATA_DIR.resolve() not in path.parents or not path.is_file():
        raise HTTPException(status_code=404, detail="Dataset not found")
    return json.loads(path.read_text(encoding="utf-8"))
