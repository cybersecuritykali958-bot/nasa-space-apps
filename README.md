# NASA Space Apps Challenge

Командный проект для хакатона NASA Space Apps Challenge (5 человек).

- **Челлендж:** _заполнить_
- **Идея:** _одно предложение_

## Структура

| Папка | Что внутри | Роль |
|-------|-----------|------|
| `web/` | Next.js + TypeScript + Tailwind | Фронтенд |
| `api/` | FastAPI (Python) | Бэкенд |
| `data/` | Скрипты и готовые наборы данных | Данные |
| `docs/` | Питч, роли, материалы | Дизайн и презентация |
| `.claude/`, `.cursor/` | Агенты, скиллы, правила для ИИ | Все |

## Быстрый старт

```bash
cp .env.example .env        # заполнить ключи

# фронтенд
cd web && npm install && npm run dev      # http://localhost:3000

# бэкенд
cd api && python -m venv .venv && source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt
uvicorn main:app --reload                 # http://localhost:8000/docs

# данные
cd data && pip install -r requirements.txt && python scripts/build_data.py
```

## Правила работы

- Своя ветка на человека: `feat/<имя>-<задача>`, в `main` напрямую не коммитим.
- Мерж в `main` каждые 1–2 часа, принимает тимлид.
- Перед крупной правкой агента делаем коммит.
- Подробнее: `CLAUDE.md`, `docs/TEAM_ROLES.md`.
