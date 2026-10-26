# Quran Baanto

Quran Baanto is a calm, offline-first Android application for organizing Qur'an Khatm and Amal distributions among families and groups, with an integrated Mushaf reader, optional real-time online rooms via Google Cloud Firestore, and a permissionary, non-intrusive server support ad model.

---

## Overview

Quran Baanto simplifies the coordination of collective Qur'an recitations. Whether arranging a monthly 30-Para family Khatm rotation or organizing shared Amal (such as Surah Yasin or collective Duas), the app computes fair, consecutive, non-overlapping assignments and provides ready-to-share summaries with dual Gregorian and Hijri calendar dates.

---

## Core Features

- **Offline-First Architecture**: All Qur'an text, distribution plans, participant details, and reading bookmarks are stored and processed locally on device in an encrypted-at-rest SQLite database.
- **Fair Rotation Engine**: Automatically shifts monthly Khatm assignments so every participant cycles through different Paras without overlapping or circular splitting.
- **Online Rooms (Optional Cloud Sync)**: Create or join live shared rooms with a 6-character passcode or QR code, syncing claims across devices in real time with Google Cloud Firestore.
- **Permissionary Server-Support Ads**:
  - **Zero Ads on Qur'an Reader**
  - **Zero Ads on Home Screen**
  - **Zero Banner Ads on Distribution Screens**
  - A voluntary 30-second sponsored message is suggested only when creating or joining an online room to help cover server bandwidth. Users can decline with zero penalty.
- **Play Store & Security Hardened**: Full TLS 1.3 network security config, cleartext HTTP blocked, Android extraction rules, ProGuard/R8 optimization, and complete Data Safety documentation.
- **Instant Sharing**: Generate clean, formatted summaries organized by assigned Paras with Gregorian and Islamic (Hijri) dates for WhatsApp, Telegram, and SMS.
- **Adaptive Visuals**: Modern Material 3 interface with Dark/Light theme support, edge-to-edge layout, and responsive typography.

---

## Project Structure & Documentation Guides

- [Google AdMob Setup Guide](ADMOB_SETUP_GUIDE.md): Step-by-step setup guide for creating AdMob IDs, rewarded video ad units, and configuring `.env` and GitHub secrets.
- [Google Play Data Safety Guide](PLAY_STORE_DATA_SAFETY.md): Questionnaire answers for the Google Play Console Data Safety section.
- [Privacy Policy](PRIVACY_POLICY.md): Full privacy policy complying with Google Play Developer Program policies.
- [Firebase Firestore Security Rules](firestore.rules): Production-hardened Firestore security rules with salted SHA-256 capability hashes.

---

## GitHub Actions CI/CD Workflows

The repository includes pre-configured, production-ready GitHub Actions workflows:

1. **`.github/workflows/build-apk.yml`**:
   - Builds debug APK on push/PR.
   - Decodes `GOOGLE_SERVICES_JSON_BASE64` or uses the included `app/google-services.json.example`.
   - Injects `.env` secrets.
   - Uploads `quran-baanto-debug-apk` artifact.

2. **`.github/workflows/build-release.yml`**:
   - Production Release workflow triggered manually or by git tags (`v*`).
   - Builds signed Google Play Store **Android App Bundle (`app-release.aab`)** and **Release APK (`app-release.apk`)**.
   - Handles release keystore signing via secrets (or generates self-signed CI fallback).
   - Automatically publishes GitHub Releases with attached artifacts.

3. **`.github/workflows/ci.yml`**:
   - Runs `./gradlew testDebugUnitTest` and verifies compilation on every commit and PR.

---

## Build Instructions

### Prerequisites
- JDK 21 (or JDK 17+)
- Android SDK Platform 36 and Build-Tools 36.0.0

### Local Build
```bash
# 1. Copy environment variables template
cp .env.example .env

# 2. Build Debug APK
./gradlew assembleDebug

# 3. Build Release Bundle (AAB for Google Play)
./gradlew bundleRelease

# 4. Run Unit Tests
./gradlew testDebugUnitTest
```
