# Научная база

Владелец: @LoranSent. Жюри — учёные, поэтому **каждая цифра в приложении должна иметь источник**, а в `indicators.json` поле `verified: true` ставится только после проверки.

## Статус проверки
| Показатель / параметр | Текущее значение | Источник | Проверено |
|---|---|---|---|
| Систолическое / диастолическое давление | 90–130 / 60–80 норма | AHA | ☐ |
| Пульс в покое | 50–100 норма, отклонение от базы 15/30 % | HRP + клиника | ☐ |
| Глюкоза натощак | 70–100 норма, ≥126 красный | ADA | ☐ |
| Температура | 36.0–37.3 норма, ≥38 красный | клиника | ☐ |
| Сон | ≥7 ч норма, <6 красный | Watson 2015 (AASM/SRS) | ☐ |
| Настроение | шкала 1–5 | HRP 5 Hazards | ☐ |
| PVT | <350 мс норма, отклонение 15/30 % | Basner 2015 (Cognition) | ☐ пороги уточнить |
| Пожизненная доза | лимит 600 мЗв, жёлтый с 300, красный с 540 | NASA-STD-3001 Vol. 1 | ☐ |
| МКС, доза | ~0.5 мЗв/сут (реально ~0.3–0.8) | HRP | ☐ |
| Луна, доза | 1.37 мЗв/сут | Zhang 2020, Chang'e-4 LND | ☐ |
| Перелёт к Марсу | 1.84 мЗв/сут | Zeitlin 2013, MSL RAD | ☐ |
| Поверхность Марса | 0.64 мЗв/сут | Hassler 2014, Curiosity RAD | ☐ |
| Потеря костной массы | ~1–1.5 % в месяц | NASA HRP | ☐ для Digital Twin |
| CSA radiation | Monitoring radiation for astronaut health (2026) | CSA | ☐ |

## Методы движка
| Метод | Параметры | Источник | Проверено |
|---|---|---|---|
| CUSUM | k = 0.5σ, h = 5σ | Page 1954, doi:10.1093/biomet/41.1-2.100 | ☐ |
| EWMA | λ = 0.2, L = 3.0 | Roberts 1959, doi:10.1080/00401706.1959.10489860; Lucas & Saccucci 1990, doi:10.1080/00401706.1990.10484583 | ☐ |
| Окно реакции триажа | красный 30 мин, жёлтый 24 ч, обработка на Земле 10 мин | допущение команды | ☐ нужен источник или обоснование |
| Космическая погода | SEP 48 ч, быстрый CME ≥1000 км/с за 72 ч | NASA DONKI | ☐ |
| Паттерны и GO/NO-GO | `engine_rules.json` | см. `sources` у каждого правила | ☐ |

**Самопроверка на демо-миссии:** поймано 10 из 14 событий, 5 до порога (среднее опережение 16.6 дня), 6 ложных тревог за 180 дней (`docs/BACKEND.md`, раздел 5).

## Ресурсы из описания челленджа
- NASA: [5 Hazards](https://www.nasa.gov/hrp/hazards/), [Risks of Human Spaceflight](https://www.nasa.gov/hrp/risks/), [Ongoing HRP Research](https://www.nasa.gov/hrp/ongoing-hrp-research/).
- CSA: [Monitoring radiation for astronaut health](https://www.asc-csa.gc.ca/eng/news/articles/2026/2026-02-26-monitoring-radiation-astronaut-health.asp), [How does radiation affect the human body in space?](https://www.asc-csa.gc.ca/eng/astronauts/space-medicine/radiation.asp); Why Do We Conduct Science Experiments in Space?

## Что ещё нужно от науки
1. Пройтись по таблице: проверить цифры и URL, поставить `verified: true`.
2. Формулы Digital Twin (кости, мышцы, доза) с источниками, отдать @jarororoor.
3. Абзац «Limitations» для подачи: синтетические данные, популяционные пороги, не медицинское изделие.
4. Список вероятных вопросов учёных и ответы на них (в `docs/PITCH.md`).
