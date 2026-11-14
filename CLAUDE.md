# CLAUDE.md: CrewPulse (NASA Space Apps 2026)

## Проект
- **Челлендж:** Create Health Monitoring Software for Astronauts on Space Missions.
- **Идея:** офлайн Android-приложение для самоосмотра астронавта: измерить → увидеть статус систем тела → понять, что делать, даже когда Земля отвечает через 20+ минут.
- **Жюри:** учёные. Главное — научная обоснованность: у каждого порога и цифры есть источник.
- **Приоритет:** рабочий демо-сценарий важнее количества функций.
- **Подача:** 15.11.2026 23:55 (Киев). Защита 17–18.11.

## Стек
- Kotlin, Jetpack Compose, Material 3, Navigation Compose.
- Room (локальная БД), kotlinx.serialization (JSON из `assets/`).
- Ручной DI через `AppContainer`. Hilt не добавлять.
- Позже по плану: CameraX + ML Kit (пульс с камеры), Retrofit (NASA DONKI), Vico (графики), WorkManager (напоминания).
- Python 3.12 в `science/` для данных и проверки алгоритмов.

## Зоны (правь только свою)
| Путь | Владелец |
|---|---|
| `android/app/src/main/java/space/crewpulse/ui/` | @harvio1 |
| `android/app/src/main/java/space/crewpulse/data/`, `domain/` | @jarororoor |
| `android/app/src/main/assets/`, `science/` | @LoranSent, @interpolfuckyou |
| `docs/` (дизайн, слайды, тексты) | @gutsonithemvp |
| корень, `android/*.kts`, `gradle/`, `.github/`, `.claude/`, `.cursor/`, `docs/PLAN.md` | тимлид |

## Контракт данных
- Формат JSON описан в `docs/DATA_CONTRACT.md`, Kotlin-модели в `data/model/Contract.kt`.
- Менять оба только вместе и только с согласия тимлида.
- Демо не зависит от живых API: всё нужное лежит в `assets/`, сеть только как бонус с кэшем.

## Правила кода
- Код, имена и комментарии на английском, тексты интерфейса на английском.
- Маленькие composable-функции, состояние во ViewModel (`StateFlow`).
- Новую зависимость добавлять в `gradle/libs.versions.toml` только по согласованию с тимлидом.
- Секреты только в `android/local.properties` (не коммитится).
- Логику (`domain/`) покрывать unit-тестами в `app/src/test/`.

## Git
- Ветка `feat/<ник>-<задача>`, PR в `main`, мержит тимлид каждые 1–2 часа.
- Перед большой правкой агента сделай коммит.
- CI собирает APK и гоняет тесты на каждый PR. Красный CI не мержим.

## Команды
```bash
cd android && ./gradlew testDebugUnitTest assembleDebug
python science/generate_demo_mission.py
```

## Как работать агенту
- Сначала прочитай нужные файлы и `docs/PLAN.md`.
- Делай только то, что просят, без лишнего рефакторинга.
- Не трогай чужие зоны и контракт без явной просьбы.
- После изменений запусти тесты и сборку, кратко опиши результат.
- Не придумывай медицинские цифры: нет источника — пометь `TODO(science)`.
