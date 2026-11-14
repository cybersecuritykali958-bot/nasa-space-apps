# Роли команды KHNUIA

| # | Роль | Кто | Зона | Инструмент |
|---|---|---|---|---|
| 1 | Тимлид / архитектор | Claude Code (ведёт @lazzzzz1) | корень, Gradle, CI, `docs/PLAN.md` | Claude Code |
| 2 | Фронтенд (экраны) | @harvio1 | `ui/` | Cursor / Android Studio |
| 3 | Бэкенд (логика в телефоне) | @jarororoor | `data/`, `domain/` | Claude Code / Cursor |
| 4 | Наука | @LoranSent | `assets/indicators.json`, `risks.json`, `environments.json`, `docs/SCIENCE.md` | Claude Code |
| 5 | Инженер данных | @interpolfuckyou | `science/`, `assets/demo_mission.json` | Claude Code |
| 6 | Дизайн и презентация | @gutsonithemvp | `docs/DESIGN.md`, `docs/PITCH.md`, `docs/SUBMISSION.md`, слайды, видео | Figma / Canva |

Пути `ui/`, `data/`, `domain/` указаны относительно `android/app/src/main/java/space/crewpulse/`.

## Кто что делает

**Тимлид.** Каркас, контракт, мержи каждые 1–2 часа, сборка APK, подача, выступление.

**@harvio1, фронтенд.** Фигура тела (главный экран), форма чек-ина, карточка «Почему?», графики трендов, экраны PVT и Digital Twin. Сначала на демо-данных, потом на данных из Room.

**@jarororoor, бэкенд.** Сохранение чек-ина в Room, личный базовый уровень, `StatusEngine`, логика PVT (тайминг в мс), NASA DONKI с кэшем, пульс с камеры (CameraX + ML Kit), если успеем.

**@LoranSent, наука.** Проверить каждый порог и источник в `indicators.json` и поставить `verified: true`. Формулы Digital Twin: кости, мышцы, доза. Таблица источников в `docs/SCIENCE.md`. Ответы учёным на защите.

**@interpolfuckyou, данные.** Генератор демо-миссии (`science/generate_demo_mission.py`), проверка алгоритма пульса rPPG на Python до переноса в Kotlin, кэш DONKI для офлайна.

**@gutsonithemvp, дизайн.** Макеты 4 экранов, SVG фигуры тела с зонами систем, иконка, 7 слайдов, видео на 30 с с англ. субтитрами, тексты Project Page.

## Пары
- @gutsonithemvp → @harvio1: макеты и SVG → экраны.
- @LoranSent → @jarororoor: пороги и формулы → движок.
- @interpolfuckyou → @jarororoor: демо-данные и алгоритм пульса.
- @LoranSent + @gutsonithemvp + тимлид: наука → слайды и питч.
