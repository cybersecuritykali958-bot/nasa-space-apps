# API

FastAPI backend for the astronaut health self-assessment tool.

## Run

```bash
cd api
python -m venv .venv && source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements-dev.txt
uvicorn main:app --reload                           # http://localhost:8000/docs
```

## Test and lint

```bash
pytest
ruff check . && ruff format --check .
```

## Layout

```
main.py           entry point (uvicorn main:app)
app/main.py       application factory
app/core/         settings (env / repo-root .env) and SQLite database
app/routers/      HTTP endpoints
tests/            pytest suite
var/              local SQLite file (git-ignored)
```

## Environment

| Variable | Default | Purpose |
|---|---|---|
| `CORS_ORIGINS` | `http://localhost:3000` | comma-separated allowed origins |
| `DATABASE_URL` | `sqlite:///api/var/app.db` | database location |
| `DATA_DIR` | `data/processed` | prepared datasets folder |
| `NASA_API_KEY` | empty | api.nasa.gov key (optional) |
| `ANTHROPIC_API_KEY` | empty | explanation feature (optional) |
