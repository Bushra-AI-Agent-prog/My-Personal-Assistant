# 📦 BUSHRA AI — Release & Deployment Guide

## 1. Local / CI APK Build Procedure

To build the APK locally or inside CI:

```bash
# Clean & run tests
gradle testDebugUnitTest

# Assemble Release APK
gradle assembleRelease

# Alternatively, assemble Debug APK if release signing keys are unset
gradle assembleDebug
```

The APK will be located in:
- `app/build/outputs/apk/release/app-release.apk`
- or `app/build/outputs/apk/debug/app-debug.apk`

---

## 2. GitHub Actions Automated Pipeline

The `.github/workflows/build.yml` workflow triggers on:
- Pushes to `main` / `master`
- Version tags (e.g., `git tag v1.0.0 && git push origin v1.0.0`)
- Manual trigger via **Run Workflow** in GitHub Actions

The workflow automatically:
1. Compiles the project using JDK 17.
2. Executes unit tests.
3. Packages the APK (`bushra-ai-release.apk`).
4. Uploads it to the GitHub Actions Artifact storage.
5. Publishes a GitHub Release with the APK attached.

---

## 3. Direct Installation on Android

1. Download `bushra-ai-release.apk` to your Android device.
2. Tap the downloaded file in your Files / Downloads app.
3. Allow "Install unknown apps" if prompted.
4. Open **BUSHRA AI** and configure your preferred provider in **Settings**!
