# Бэкенд CrewPulse: Autonomous Health Reasoning Engine

Владелец: @jarororoor. Код: `android/app/src/main/java/space/crewpulse/data/` и `domain/`.

## 1. Концепция

Движок в телефоне, который **замечает проблему за дни до порога, объясняет почему и говорит, что делать, когда Земля не успеет ответить**. Работает без интернета, у каждого порога и правила есть источник.

| Вопрос астронавта | Что отвечает движок | Где в коде |
|---|---|---|
| Что происходит? | Цвет каждого показателя: норма для всех **и** личная норма. Раннее предупреждение по трендам. Уверенность в данных | `StatusEngine`, `EarlyWarning`, `ConfidenceEstimator` |
| Что это значит? | Связки показателей с цепочкой доказательств, включая детектор расхождений «говорю, что всё хорошо, а данные против» | `PatternMatcher` |
| Что делать сейчас? | Кто действует (сам / медик экипажа / Земля / бортовой протокол) с учётом задержки связи. GO / NO-GO перед выходом в открытый космос и посадкой | `Triage`, `Readiness` |
| Когда мерить? | Адаптивное расписание: проблемное часто, стабильное редко. Считает сэкономленное время экипажа | `MeasurementPlanner` |
| Зачем мерить каждый день? | Серии, очки, уровни, значки, «миссия дня» | `Gamification` |
| Что с космической погодой? | Реальные события NASA DONKI → «в укрытие» + радиационный бюджет до лимита карьеры 600 мЗв | `SpaceWeather`, `SpaceWeatherRepository` |
| Можно ли движку верить? | Самопроверка: прогон 180 дней миссии, время опережения, ложные тревоги | `EngineBenchmark` |

## 2. Как работает приложение (поток данных)

```
 Чек-ин / PVT / камера ──► ReadingRepository ──► Room (readings)
 Демо-миссия (assets) ──► DemoSeeder ─────────────┘     │
 Личная норма ──► BaselineRepository ──► Room (baselines)│
 NASA DONKI ──► SpaceWeatherRepository (live → кэш → assets)
                                                          ▼
 engine_rules.json + indicators.json ──►  HealthEngine.analyze(EngineInput)
                                                          │
                                                          ▼
                                       EngineReport  ──► HomeViewModel.state ──► экраны
```

1. Пользователь сохраняет чек-ин → значения пишутся в Room.
2. Room сам уведомляет `HomeViewModel` (Flow), тот собирает `EngineInput` и вызывает `HealthEngine.analyze`.
3. Движок возвращает один объект `EngineReport` со всем: статусы, тревоги, паттерны, триаж, готовность, план, игра, космическая погода.
4. Экраны просто рисуют `state.report`. Логики в экранах нет.

Переключатели (`setProfile`, `setEnvironment`) сохраняются между запусками и сразу пересчитывают отчёт.

## 3. Как фронтенд пользуется бэкендом

Всё доступно через `HomeViewModel` (один на всё приложение, создаётся в `AppNavHost`).

### Действия
```kotlin
home.saveCheckIn(mapOf("systolic_bp" to 121.0, "diastolic_bp" to 78.0, "heart_rate" to 64.0,
                       "glucose" to 90.0, "body_temp" to 36.6, "sleep_hours" to 7.2, "mood" to 4.0))
home.setProfile(Profile.ME)           // или Profile.DEMO
home.setEnvironment("mars_surface")   // id из environments.json
home.refreshSpaceWeather()            // свежие данные NASA, офлайн безопасно
home.runBenchmark()                   // самопроверка движка → state.benchmark
```

### Чтение (`val state by home.state.collectAsStateWithLifecycle()`)
| Поле | Что это | Пример UI |
|---|---|---|
| `state.systems` | Системы тела и их цвет (`SystemStatus`) | Фигура тела |
| `state.evaluation("heart_rate")` | Значение, цвет, отклонение от личной нормы | Карточка «Почему?» |
| `state.report.alerts` | Ранние предупреждения (`Alert.message`, `daysToThreshold`) | Баннер «давление растёт, порог через ~7 дней» |
| `state.report.confidence["heart_rate"]` | `HIGH/MEDIUM/LOW` + причины | Значок «перемерь» |
| `state.report.patterns` | Найденные связки + `evidence` + совет | Карточка «Cardiovascular strain» |
| `state.report.triage` | `responder`, `headline`, `reason`, `earthResponseMin` | Главный блок «что делать» |
| `state.report.readiness` | `GO / CAUTION / NO_GO` для выхода и посадки | Экран «Готовность» |
| `state.report.plan` | Что мерить в ближайшие 24 ч, сэкономленные минуты | Список задач |
| `state.report.game` | Очки, уровень, серия, значки, миссия дня | Шапка профиля |
| `state.report.spaceWeather` | `QUIET/WATCH/WARNING` + источник данных | Плашка космической погоды |
| `state.report.radiation` | Доза, остаток до 600 мЗв, дней осталось | Радиационный бюджет |
| `state.environment`, `state.environments` | Текущая и все среды | Переключатель МКС / Луна / Марс |
| `state.benchmark` | Отчёт самопроверки (`summary()`) | Экран «Validation» |

### Пример: блок триажа и баннер раннего предупреждения
```kotlin
@Composable
fun TriageCard(state: HomeUiState) {
    val t = state.report.triage
    Card {
        Text(t.headline, style = MaterialTheme.typography.titleLarge)
        Text(t.reason)
        t.earthResponseMin?.let { Text("Earth can answer in ~${it.roundToInt()} min") }
    }
    state.report.alerts.firstOrNull()?.let { alert ->
        Text("⚠ ${alert.message}")
    }
}
```

## 4. Что выдаёт движок на демо-астронавте (день 180, перелёт к Марсу)

Из `DemoScenarioTest`, это ровно то, что увидит экран:

- **Системы:** сердце и сосуды — жёлтый, радиация — жёлтый, остальные зелёные.
- **Ранние предупреждения:** систолическое давление ещё **в норме** (129.5 < 130), но сдвинуто на +4.3 SD от личной вариабельности и дрейфует 18 дней. Аналогично диастолическое давление и пульс.
- **Паттерн:** «Cardiovascular strain» с доказательствами (давление +10 %, пульс +15 % к личной норме).
- **Триаж:** «Talk to the crew medical officer»: 2 системы требуют внимания плюс найден паттерн.
- **Готовность:** выход в открытый космос — CAUTION, посадка — CAUTION.
- **Радиация:** 361 из 600 мЗв, при 1.84 мЗв/сут хватит на ~130 дней.
- **План:** 6 замеров сегодня (~7 мин), сэкономлено ~8 мин против «всё каждый день».
- **Космическая погода:** реальные данные DONKI на сегодня.

Сценарий для защиты: переключить среду с Марса на МКС при красном показателе. Решение триажа меняется с «бортовой протокол» на «связаться с врачом на Земле».

## 5. Самопроверка движка (валидация)

`EngineBenchmark` прогоняет 180 дней демо-миссии так, будто движок запускался каждое утро:

> Replayed 180 days: **caught 10 of 14 events, 5 before the threshold (mean lead 16.6 days), 6 false alarm(s).**

Как читать честно:
- Медленные изменения (давление, пульс, доза) пойманы **за 7–30 дней** до порога.
- Внезапные события (неделя без сна, 3 дня температуры) предсказать нельзя, они ловятся в день события. Так и должно быть.
- 6 ложных тревог на 9 показателей за 180 дней.
- Данные синтетические. На реальных данных метрики нужно перепроверить.

Методика описана в KDoc `EngineBenchmark`, запуск: `./gradlew testDebugUnitTest --tests '*EngineBenchmarkTest*' -i`.

## 6. Алгоритмы и параметры

Все числа лежат в `assets/engine_rules.json` и меняются без кода.

| Часть | Метод | Параметры | Источник |
|---|---|---|---|
| Статус | Диапазоны `bands` + % отклонения от личной нормы, берётся худший | `indicators.json` | AHA, ADA, AASM, HRP |
| Личная норма | Среднее первых 3 замеров или заданная вручную | `BaselineCalculator.FIRST_READINGS` | — |
| EWMA | z = λx + (1−λ)z, тревога за пределами L·σ·√(λ/(2−λ)) | λ = 0.2, L = 3.0 | Roberts 1959; Lucas & Saccucci 1990 |
| CUSUM | Табличный CUSUM, перезапуск после окончания эпизода | k = 0.5σ, h = 5σ | Page 1954 |
| Тренд | МНК за 14 дней, |t| ≥ 3, прогноз пересечения в пределах 30 дней | `trend*` | — |
| σ | Разброс первых 14 замеров (личный, не популяционный) | `baselineWindow` | — |
| Триаж | Ответ Земли = 2 × задержка + 10 мин обработки, сравнивается с окном реакции (красный 30 мин) | `triage` | **допущение команды, ждёт проверки науки** |
| Космическая погода | SEP за 48 ч → WARNING; быстрый CME (≥1000 км/с) за 72 ч или X-вспышка → WATCH | `spaceWeather` | NASA DONKI, NASA-STD-3001 |

## 7. Как развивать с помощью ИИ

Перед любой задачей агент читает `CLAUDE.md`, этот файл и `docs/DATA_CONTRACT.md`. Для Claude Code есть скилл `health-engine`.

**Новое правило-паттерн (без кода).** Добавить объект в `patterns` в `engine_rules.json`. Условия: `minStatus`, `statusIn`, `minDeviationPct`, `maxDeviationPct`, `hasAlert`.
> Промпт: «Добавь в engine_rules.json паттерн "dehydration": пульс ≥ +10 % к норме и давление ниже нормы. Источник — HRP. Запусти тесты.»

**Новый показатель.** Добавить в `indicators.json` (+ при желании в `schedule.intervalDays`). Остальное подхватится само.
> Промпт: «Добавь показатель SpO2 (%, система cardiovascular) с порогами и источником, интервалом замера 1 день. Проверь AssetContractTest.»

**Новый экран на данных движка.**
> Промпт для @harvio1: «Сделай экран Readiness: для каждого `state.report.readiness` карточка с вердиктом GO/CAUTION/NO_GO и списком причин. Данные бери только из HomeUiState, логику не дублируй.»

**Новая логика в движке.** Новый объект в `domain/` → результат в `EngineReport` → вызов в `HealthEngine.analyze` → unit-тест в `app/src/test/`.
> Промпт: «Добавь в domain/ PVT-логику: класс, который по списку времён реакции считает среднее, промахи > 500 мс и фальстарты. Покрой тестами.»

**Правила:** не менять формат JSON без тимлида, не придумывать медицинские числа (`verified: false` + `TODO(science)`), запускать `./gradlew testDebugUnitTest assembleDebug`.

## 8. Ограничения (честно для жюри)
- Демо-данные синтетические. Пороги и правила помечены `verified: false`, пока их не проверила наука.
- Окно реакции в триаже — допущение команды.
- Это поддержка решений и обучение, не медицинское изделие.
- Канал вспышек DONKI (FLR) сейчас отвечает 404, используются SEP и CME. Код готов к FLR, если канал заработает.
