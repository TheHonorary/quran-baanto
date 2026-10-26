# Google Play Store Data Safety Questionnaire Guide

Use this reference to complete the **Data safety** section in the **Google Play Console** (`Policy and programs -> App content -> Data safety`).

---

## 1. Overview Summary

| Question | Answer | Details / Explanation |
| :--- | :--- | :--- |
| **Does your app collect or share any of the required user data types?** | **Yes** | App collects display names for online rooms and integrates with Google AdMob & Firebase. |
| **Is all of the user data collected by your app encrypted in transit?** | **Yes** | All network traffic uses HTTPS / TLS 1.3 enforced via Android Network Security Config. |
| **Do you provide a way for users to request that their data be deleted?** | **Yes** | Users can delete local rooms or delete online room claims directly in the app. |

---

## 2. Data Types Collected & Shared

### A. Location
- **Approximate Location (Coarse):**
  - **Collected:** Yes (by Google AdMob SDK for ad serving / fraud prevention).
  - **Shared:** Yes (with Google AdMob).
  - **Processed Ephemerally:** Yes.
  - **Required / Optional:** Optional (used only if user grants location or accepts ad).
  - **Purposes:** Advertising or marketing, Fraud prevention & security.

### B. Personal Info
- **Name / Display Name:**
  - **Collected:** Yes (Participant display names entered when creating/joining online rooms).
  - **Shared:** No (only visible to other members possessing the room passcode).
  - **Processed Ephemerally:** No (stored in Firestore until room is completed/deleted).
  - **Required / Optional:** Optional (users can use offline rooms without cloud).
  - **Purposes:** App functionality.
- **User IDs:**
  - **Collected:** Yes (Firebase Anonymous / Google Auth UID for room permissions).
  - **Shared:** No.
  - **Purposes:** App functionality, Account management.

### C. Financial Info
- **Not Collected / Not Shared** (App is free to download with zero in-app purchases).

### D. Health & Fitness
- **Not Collected / Not Shared**.

### E. Messages / Photos / Media / Audio
- **Not Collected / Not Shared**.

### F. App Activity
- **Not Collected / Not Shared** (Mushaf reading position and surah bookmarks are stored purely on-device via Room/DataStore and never transmitted).

### G. App Info and Performance
- **Crash Logs / Diagnostics:**
  - **Collected:** Yes (by Google Play Services / Firebase for app stability).
  - **Shared:** Yes (with Google for crash analysis).
  - **Purposes:** Analytics, Diagnostics, Fraud prevention.

### H. Device or Other IDs
- **Device or other IDs (Advertising ID):**
  - **Collected:** Yes (Google Advertising ID by Google AdMob).
  - **Shared:** Yes (with Google AdMob).
  - **Purposes:** Advertising or marketing, Fraud prevention.

---

## 3. Security Practices

- **Encryption in Transit:** Checked (Yes).
- **Data Deletion Mechanism:** Checked (Yes).
  - URL for data deletion request: `https://github.com/<your-username>/quran-baanto/blob/main/PRIVACY_POLICY.md` (or your support email).
- **Family Policy / Target Age:**
  - Target audience: 13 and older (or All Ages if certified under Families policy with AdMob neutral age screen).

---

## 4. Ads Declaration in Google Play Console

In **App Content -> Ads**:
- Select: **"Yes, my app contains ads"**
  - Explanation: The app contains voluntary, permissionary rewarded ads to support cloud server costs.
