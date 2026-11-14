# Joining the CrewPulse project

## 1. Access
Accept the invitation to `cybersecuritykali958-bot/nasa-space-apps` (GitHub email or https://github.com/notifications).

## 2. Install
- **Android Studio** (latest stable): https://developer.android.com/studio. It ships its own JDK.
- **Git** and **GitHub CLI**: https://git-scm.com, https://cli.github.com
- **Python 3.12**, science and data roles only.
- **Claude Code** or **Cursor**, whichever you prefer.
- Design only needs Figma.

## 3. Get the code
```bash
gh auth login
gh repo clone cybersecuritykali958-bot/nasa-space-apps
cd nasa-space-apps
git checkout -b feat/<nick>-<task>
```

## 4. Open and run
1. Android Studio → **Open** → the `android/` folder (not the repo root).
2. Wait for **Gradle Sync** (5–10 minutes the first time).
3. **Device Manager** → create a Pixel with Android 14, or connect a phone (Settings → About phone → tap "Build number" 7 times → Developer options → USB debugging).
4. Press ▶ **Run**. The CrewPulse home screen with body systems should open.

Optional: add `NASA_API_KEY=...` to `android/local.properties` for live NASA DONKI data. Never commit this file.

## 5. AI agents
- **Claude Code:** run `claude` in the repo root. It reads `CLAUDE.md`, agents in `.claude/agents/` and skills in `.claude/skills/`.
- **Cursor:** File → Open Folder → repo root. Rules are in `.cursor/rules/`, agents in `.cursor/agents/`.
- Backend usage for screens and how to extend the engine: `docs/BACKEND.md` (skill `health-engine`).

## 6. Daily routine
```bash
git pull origin main
# work
cd android && ./gradlew testDebugUnitTest assembleDebug   # before pushing
git add <your files>
git commit -m "What changed"
git push -u origin feat/<nick>-<task>
```
Then open a **Pull request** into `main` on GitHub. CI builds the APK and runs the tests.

## 7. Rules
- Edit only your zone (`docs/TEAM_ROLES.md`).
- Only the team lead changes the data contract.
- Do not commit `local.properties`, keys or APKs.
- Everything in the repo is in English.
- Merge conflict: do not delete other people's work, call the team lead.
