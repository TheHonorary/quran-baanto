# Quran Baanto

Quran Baanto is a calm, offline-first Android application for organizing Qur'an Khatm and Amal distributions among families and groups, with an integrated Qur'an reading experience.

---

## Overview

Quran Baanto simplifies the coordination of collective Qur'an recitations. Whether arranging a monthly 30-Para family Khatm rotation or organizing shared Amal (such as Surah Yasin or Dua recite sessions), the app computes fair, consecutive, non-overlapping assignments and provides ready-to-share summaries with dual Gregorian and Hijri calendar dates.

---

## Core Features

- **Offline-First Architecture**: All Qur'an text, distribution plans, participant details, and reading bookmarks are stored and processed locally on device.
- **Fair Rotation Engine**: Automatically shifts monthly Khatm assignments so every participant cycles through different Paras without overlapping or circular splitting.
- **Custom Amal Recitations**: Distribute arbitrary counts of any Surah or custom prayer (e.g. 40x Surah Yasin, 100x Ayat al-Kursi) across participants.
- **Instant Sharing**: Generate clean, formatted text summaries organized by assigned Paras with Gregorian and Islamic (Hijri) dates for messaging apps.
- **Adaptive Visuals**: Modern Material 3 interface with Dark/Light theme support, edge-to-edge layout, and smooth typography.

---

## Qur'an Reader

- Complete 114 Surahs and 30 Paras available completely offline.
- High-contrast Arabic typography with verse numbering and translation options.
- Instant search and filtering across Surah names (English & Arabic), Revelation place, and Para indexes.
- Reading progress tracking and bookmark management.

---

## Khatm Distribution

- Organizes the complete 30-Para Qur'an recitation across any number of participants.
- Supports unequal share allocations (e.g., participants reading 1, 2, or 3 Paras each) while ensuring total sum equals 30.
- Automatic month-by-month progression ensuring strictly consecutive assigned Paras for each participant.
- Quick single-Para display formatting (e.g., `Para 11`, `Para 30`) and multi-Para range formatting (e.g., `Paras 14–16`).

---

## Amal Distribution

- Configure targeted recitation goals for specific Surahs or custom Adhkar/Duas.
- Distribute target recitation counts evenly or with custom allocation per member.
- Track group completion status and progress over time.

---

## Sharing

- One-tap formatted message generation for WhatsApp, Telegram, SMS, and email.
- Structured output ordered in ascending sequence by Para numbers (1 to 30) for visual clarity.
- Includes distribution title, active Gregorian month, calculated Hijri month/year, and traditional closing prayers.

---

## Privacy & Local Architecture

- **Zero Cloud Dependence for Core Logic**: No external account creation or remote database connection is required to read or distribute recitations.
- **Local Persistence**: Powered by Room Database and Encrypted Preferences for reliable on-device storage.
- **No Background Telemetry**: Respects user privacy and battery life.

---

## Technology Stack

- **Platform**: Android (minSdk 24, targetSdk 36)
- **Language**: Kotlin 2.2.10
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: MVVM / Clean Architecture with Kotlin Coroutines & Flow
- **Local Database**: Android Jetpack Room with Kotlin Symbol Processing (KSP)
- **Build System**: Gradle 9.3.1 with Android Gradle Plugin 9.1.1 and Gradle Version Catalog

---

## Build Instructions

### Prerequisites

- JDK 17
- Android SDK with Platform 36 and Build-Tools

### Building from Command Line

Clone the repository and build the debug APK using the included Gradle Wrapper:

```bash
# On Linux / macOS
./gradlew assembleDebug

# On Windows
gradlew.bat assembleDebug
```

The compiled APK will be generated in:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Running Unit Tests

```bash
./gradlew testDebugUnitTest
```

---

## Project Status

Active development. Tested for offline compatibility, unit test coverage across multi-month rotation engines, and standard Android CI/CD build environments.
