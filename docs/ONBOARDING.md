# Как подключиться к проекту CrewPulse

## 1. Доступ
Прими приглашение в репозиторий `cybersecuritykali958-bot/nasa-space-apps` (письмо от GitHub или https://github.com/notifications).

## 2. Установить
- **Android Studio** (последняя стабильная): https://developer.android.com/studio. JDK уже внутри.
- **Git** и **GitHub CLI**: https://git-scm.com, https://cli.github.com
- **Python 3.12**, только науке и данным.
- **Claude Code** или **Cursor**, кому что удобно.
- Дизайнеру достаточно Figma.

## 3. Скачать проект
```bash
gh auth login
gh repo clone cybersecuritykali958-bot/nasa-space-apps
cd nasa-space-apps
git checkout -b feat/<ник>-<задача>
```

## 4. Открыть и запустить
1. Android Studio → **Open** → папка `android/` (именно она, не корень).
2. Дождись **Gradle Sync** (в первый раз 5–10 минут).
3. **Device Manager** → создать Pixel с Android 14 или подключить телефон (Настройки → О телефоне → 7 раз тапнуть «Номер сборки» → Для разработчиков → Отладка по USB).
4. Нажми ▶ **Run**. Должен открыться экран CrewPulse со списком систем тела.

## 5. Агенты
- **Claude Code:** запускай `claude` в корне репозитория. Он прочитает `CLAUDE.md`, агентов из `.claude/agents/` и скиллы из `.claude/skills/`.
- **Cursor:** File → Open Folder → корень репозитория. Правила лежат в `.cursor/rules/`, агенты в `.cursor/agents/`.

## 6. Каждый день
```bash
git pull origin main
# работа
cd android && ./gradlew testDebugUnitTest assembleDebug   # перед пушем
git add <свои файлы>
git commit -m "Что сделано"
git push -u origin feat/<ник>-<задача>
```
Потом на GitHub открой **Pull request** в `main`. CI сам соберёт APK и прогонит тесты.

## 7. Правила
- Правь только свою зону (`docs/TEAM_ROLES.md`).
- Контракт данных меняет только тимлид.
- Не коммить `local.properties`, ключи и APK.
- Конфликт при мерже: не удаляй чужое, позови тимлида.
