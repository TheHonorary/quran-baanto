# Privacy Policy for Quran Baanto

**Effective Date:** October 2026  
**Last Updated:** October 2026  
**Developer:** Hambasy Studios  
**Application:** Quran Baanto (Android)

---

## 1. Introduction
Welcome to **Quran Baanto**. We hold the privacy, peace of mind, and security of our users in the highest regard. This Privacy Policy details how Quran Baanto collects, uses, stores, and protects your information when you use our mobile application.

Our guiding principle is **Sanctuary and Simplicity**:
- We do **not** sell, rent, or trade your personal information.
- We do **not** track or log your private Qur'an recitation habits.
- Local khatm groups and personal readings remain **100% offline** on your device by default.

---

## 2. Information We Collect

### A. Local Information (Stored Exclusively on Your Device)
- **Local Khatm & Amal Distributions:** Participant names, para allocations, completion statuses, and targets for local groups are stored exclusively in an encrypted-at-rest SQLite database on your device via Android Room.
- **Mushaf & Reading State:** Your last read ayah, bookmarks, Arabic font preferences, font sizes, and dark/light display settings are stored locally on your device via Android Jetpack DataStore.
- **This data never leaves your device unless you explicitly create an Online Room.**

### B. Online Rooms & Shared Khatms (Cloud-Synchronized)
When you optionally choose to create or join an **Online Room** via a room passcode or QR code:
- **Room Identity:** Room name, total target (e.g., 30 Paras or Amal counts), and active round number.
- **Participant Claims:** The display name you choose (e.g., "Uncle Tariq") and your selected Para/Amal number.
- **Authentication:** To maintain server integrity and prevent spam, anonymous or Google-authenticated session tokens are securely handled via Google Firebase Authentication.
- **Cloud Security:** Online rooms are hosted in **Google Cloud Firestore** using end-to-end encrypted TLS 1.3 connections and hardened server-side security rules.

### C. Voluntary Ad Experience & Advertising Identifiers
- Quran Baanto has **ZERO banner ads** and **ZERO intrusive popups**.
- **Quran reading screens and the Home screen are 100% ad-free**.
- When creating or joining an online room, the app may display a polite, optional prompt asking if you would like to view a voluntary 30-second sponsored message to help cover cloud server costs.
- If you accept, Google Mobile Ads (AdMob) SDK serves a sponsored video. Google AdMob may collect standard device identifiers (such as Google Advertising ID), coarse location, or diagnostic information in compliance with Google Play Developer Program policies.
- If you decline, you are immediately returned to your room with **zero penalty, zero feature restrictions, and zero lockouts**.

---

## 3. How We Use Information
We use information strictly to:
1. Enable offline tracking and monthly rotation of Qur'an reading among family members.
2. Synchronize claimed Paras among participants in real time when using Online Rooms.
3. Schedule local on-device reminder notifications for your assigned Paras (scheduled locally via Android AlarmManager without cloud telemetry).
4. Maintain server stability and offset cloud database costs through voluntary, user-approved sponsorships.

---

## 4. Data Sharing and Third-Party Disclosures
We **never** sell user data to data brokers, marketers, or unauthorized third parties. We only integrate with trusted, industry-standard Google service providers:
- **Google Firebase (Firestore, Auth):** For real-time online room synchronization and database hosting. [Firebase Privacy Policy](https://firebase.google.com/support/privacy)
- **Google AdMob:** For voluntary, opt-in rewarded server-support advertisements. [Google Privacy Policy](https://policies.google.com/privacy)
- **Google Play Services:** For core app integrity and delivery.

---

## 5. Security & Protection of Information
- **Encrypted in Transit:** All communication between Quran Baanto and Google cloud servers is encrypted using HTTPS and TLS 1.3 protocols.
- **Network Security Configuration:** Cleartext (HTTP) traffic is strictly blocked at the Android operating system level.
- **Backup Protections:** Local databases and credentials are explicitly excluded from unencrypted cloud backups via Android Data Extraction Rules.
- **Access Control:** Firebase Firestore security rules enforce strict write validation and prevent unauthorized access or room enumeration.

---

## 6. User Rights & Data Deletion
- **Delete Local Data:** You can clear any local distribution or reset app data directly within the app or Android system settings at any time.
- **Leave / Delete Online Rooms:** Room hosts can close or delete rooms. Participants can release their claimed Paras at any time.
- **Opt-out of Personalized Ads:** You can manage or reset your Android Advertising ID at any time via `Android Settings -> Google -> Ads`.

---

## 7. Children's Privacy
Quran Baanto is designed for general audiences, families, and community members. We do not knowingly collect personally identifiable information from children under the age of 13.

---

## 8. Changes to This Privacy Policy
We may update this Privacy Policy from time to time. Any changes will be published in this repository and accessible within the app's Settings menu.

---

## 9. Contact Us
If you have any questions, feedback, or requests regarding this Privacy Policy or your data, please reach out to:
- **Developer:** Hambasy Studios
- **Email:** support@hambasystudios.com (or via the project repository)
