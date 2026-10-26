# Quran Baanto

Quran Baanto is a calm, offline-first Android application for organizing Qur'an Khatm and Amal distributions among families and groups, with an integrated Mushaf reader, optional real-time online rooms via Google Cloud Firestore, and a permissionary, non-intrusive server support ad model.

---

## Core Features

- **Offline-First Architecture**: All Qur'an text, distribution plans, participant details, and reading bookmarks are stored and processed locally on device in an encrypted-at-rest SQLite database.
- **Fair Rotation Engine**: Automatically shifts monthly Khatm assignments so every participant cycles through different Paras without overlapping or circular splitting.
- **Online Rooms (Cloud Sync)**: Create or join live shared rooms with a 6-character passcode or QR code, syncing claims across devices in real time with Google Cloud Firestore.
- **Permissionary Server-Support Ads**:
  - Zero Ads on Qur'an Reader
  - Zero Ads on Home Screen
  - Zero Banner Ads on Distribution Screens
  - A voluntary 30-second sponsored message is suggested only when creating or joining an online room to help cover server bandwidth. Users can decline with zero penalty.
- **Play Store & Security Hardened**: Full TLS 1.3 network security config, cleartext HTTP blocked, Android extraction rules, and ProGuard/R8 optimization.
- **Instant Sharing**: Generate clean, formatted summaries organized by assigned Paras with Gregorian and Islamic (Hijri) dates for WhatsApp, Telegram, and SMS.
- **Adaptive Visuals**: Modern Material 3 interface with Dark/Light theme support, edge-to-edge layout, and responsive typography.

---

## GitHub Actions CI/CD Workflows

The repository includes pre-configured, production-ready GitHub Actions workflows:

1. **`.github/workflows/build-apk.yml`**: Builds debug APK and uploads `quran-baanto-debug-apk` artifact.
2. **`.github/workflows/build-release.yml`**: Production Release workflow triggered manually or by git tags (`v*`). Builds Google Play Store **Android App Bundle (`app-release.aab`)** and **Release APK (`app-release.apk`)**.
3. **`.github/workflows/ci.yml`**: Runs unit tests and verifies compilation on every commit and pull request.

---

## Build Instructions

### Prerequisites
- JDK 21 (or JDK 17+)
- Android SDK Platform 36 and Build-Tools 36.0.0

### Local Build
```bash
# 1. Build Debug APK
./gradlew assembleDebug

# 2. Build Release Bundle (AAB for Google Play)
./gradlew bundleRelease

# 3. Run Unit Tests
./gradlew testDebugUnitTest
```
