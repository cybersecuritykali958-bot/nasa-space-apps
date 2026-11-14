# Контракт данных

Файлы лежат в `android/app/src/main/assets/`, Kotlin-модели в `data/model/Contract.kt`.
**Менять формат только вместе с моделями и только с согласия тимлида.** Добавлять записи (новые показатели, источники) можно свободно.

Статусы: `"green" | "yellow" | "red"` (`"unknown"` только в коде).

## indicators.json
```json
{
  "version": 1,
  "indicators": [{
    "id": "heart_rate",               // уникальный, snake_case; ключ в demo_mission.readings
    "name": "Resting heart rate",
    "unit": "bpm",
    "system": "cardiovascular",       // см. список систем ниже
    "bands": [                        // min включительно, max не включительно, null = без границы
      {"status": "red", "max": 40},
      {"status": "green", "min": 50, "max": 100}
    ],
    "baselineDeviationPct": {"yellow": 15, "red": 30},   // необязательно: % отклонения от личной базы
    "howToMeasure": "...",
    "whySpace": "...",
    "actions": {"green": "...", "yellow": "...", "red": "..."},
    "riskIds": ["cardiovascular"],    // id из risks.json
    "sources": [{"title": "...", "url": "..."}],
    "verified": false                 // true только после проверки наукой
  }]
}
```
Итоговый статус берётся как **худший** из двух: по диапазону `bands` и по отклонению от личной базы.

**Системы тела:** `cardiovascular`, `metabolic`, `immune`, `behavioral`, `musculoskeletal`, `vision`, `radiation`.

## environments.json
```json
{"version": 1, "environments": [{
  "id": "mars_transit", "name": "Earth-Mars transit",
  "gravityG": 0.0, "radiationMsvPerDay": 1.84, "commDelayMinOneWay": 12.0,
  "sources": [{"title": "...", "url": "..."}]
}]}
```
`commDelayMinOneWay` — типичная задержка в одну сторону в минутах (для Марса реальный диапазон ~3–22).

## risks.json
```json
{"version": 1, "risks": [{
  "id": "sans", "name": "...", "hazard": "Altered gravity",
  "system": "vision", "summary": "...", "url": "..."
}]}
```
`hazard` — один из 5 hazards NASA HRP: Space radiation, Isolation and confinement, Distance from Earth, Altered gravity, Hostile closed environment.

## demo_mission.json
Генерируется скриптом `science/generate_demo_mission.py`, руками не правится.
```json
{
  "version": 1, "astronaut": "Demo astronaut A", "environmentId": "mars_transit",
  "synthetic": true,
  "baseline": {"heart_rate": 62.0},
  "days": [{"day": 1, "readings": {"heart_rate": 61.8, "cumulative_dose": 1.8}}],
  "events": [{"day": 92, "type": "solar_particle_event", "note": "..."}]
}
```

## Room (данные в телефоне)
Таблица `readings`: `indicatorId`, `value`, `timestampMillis`, `source` (`manual` | `camera` | `pvt` | `demo`).
