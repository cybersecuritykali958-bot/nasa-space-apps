---
name: release-checklist
description: Steps to build, publish and verify the final APK and submission links. Use before the release and before submitting.
---

# Release checklist
- [ ] `main` is green in CI; `./gradlew testDebugUnitTest assembleRelease` passes locally.
- [ ] Demo flow passes on a real phone: launch → body figure → indicator card → trends → PVT.
- [ ] App works in airplane mode.
- [ ] Tag `v1.0.0` pushed → CI attaches the APK to a GitHub Release.
- [ ] APK link opens without login (incognito). If the repo is private, mirror the APK and the video to a public Google Drive folder.
- [ ] 30-second demo video recorded from the phone screen.
- [ ] README, `docs/SUBMISSION.md` and screenshots updated.
