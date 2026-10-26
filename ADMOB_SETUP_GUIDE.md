# Google AdMob Setup Guide for Quran Baanto

This step-by-step guide explains how to configure Google AdMob for **Quran Baanto**, which files to edit, what values to supply, and where to find them in the Google AdMob console.

---

## 1. Overview of the Quran Baanto Ad Model
- **ZERO Ads on Qur'an Reader:** The Mushaf reading experience is completely clean and holy.
- **ZERO Ads on Home Screen:** Uncluttered sanctuary design.
- **ZERO Banner Ads:** No banners taking up screen real estate on distribution screens.
- **Voluntary Permissionary Ads Only:** When a user creates or joins an online room, the app presents a polite, psychological prompt asking if they would like to watch a brief 30-second sponsored message to help cover cloud server costs ("Help Keep Our Servers Free"). If they accept, the ad is displayed; if they decline, the room continues with **zero restrictions**.

---

## 2. Where to Create AdMob Accounts & IDs

### Step 1: Create a Google AdMob Account
1. Visit [https://admob.google.com/](https://admob.google.com/) and sign in with your Google Account.
2. Complete your payment and tax profile.

### Step 2: Register the Android App in AdMob
1. In the left navigation menu, click **Apps** -> **Add App**.
2. Select **Android**.
3. If the app is already published on Google Play, select "Yes" and search for it. If not yet published, select "No" and enter the app name: `Quran Baanto`.
4. Once created, note your **AdMob App ID**.
   - Format: `ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY` (Notice the tilde `~`).

### Step 3: Create a Rewarded Video Ad Unit
1. In AdMob, go to **Apps** -> Select **Quran Baanto** -> **Ad units** -> **Add ad unit**.
2. Choose **Rewarded** (Rewarded video ad).
3. Set the Ad unit name to: `Online Room Server Support`.
4. Set reward settings (e.g. Reward amount: `1`, Reward item: `Server Support`).
5. Click **Create ad unit**.
6. Note your **Rewarded Ad Unit ID**.
   - Format: `ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ` (Notice the slash `/`).

---

## 3. Which Files to Edit & What to Enter

### File 1: `.env` (Local Development) & `.env.example`
Located at: `/.env` in the project root.
Replace the Google test IDs with your production IDs:
```env
# Production AdMob App ID
ADMOB_APP_ID=ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY

# Production Rewarded Ad Unit ID
ADMOB_REWARDED_AD_UNIT_ID=ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ
```
> **Note:** During local development and testing, you can keep the default Google Test IDs (`ca-app-pub-3940256099942544~3347511713` and `ca-app-pub-3940256099942544/5224354917`). Never click your own live ads!

### File 2: GitHub Actions Repository Secrets (for CI/CD & Automated Builds)
When using the automated GitHub Actions (`.github/workflows/build-release.yml` and `build-apk.yml`), configure these in:
**GitHub Repository -> Settings -> Secrets and variables -> Actions -> Repository secrets**:

| Secret Name | Value Description | Example / Format |
| :--- | :--- | :--- |
| `ADMOB_APP_ID` | Your live AdMob Application ID | `ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY` |
| `ADMOB_REWARDED_AD_UNIT_ID` | Your Rewarded Video Ad Unit ID | `ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ` |
| `GOOGLE_SERVICES_JSON_BASE64` | Base64-encoded `google-services.json` | `cat app/google-services.json \| base64 -w 0` |
| `KEYSTORE_BASE64` | Base64-encoded release keystore file | `cat my-upload-key.jks \| base64 -w 0` |
| `STORE_PASSWORD` | Password for your release keystore | Your secure password |
| `KEY_PASSWORD` | Password for your private key | Your secure password |
| `KEY_ALIAS` | Alias name for your release key | `upload` |

### File 3: `AndroidManifest.xml` (Already Pre-configured!)
Located at: `app/src/main/AndroidManifest.xml`
The app is already pre-configured to read `ADMOB_APP_ID` dynamically:
```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="${ADMOB_APP_ID}" />
```
Gradle will automatically inject your `ADMOB_APP_ID` from `.env` or GitHub Secrets during build!

---

## 4. Enabling the Official Google Mobile Ads SDK (Optional)
Currently, `ServerSupportAdManager.kt` provides the UI flow, 30-second sponsored countdown, and psychological prompt out of the box with zero external crashes.

To connect live AdMob Rewarded Video Ads:
1. In `gradle/libs.versions.toml`:
   ```toml
   [versions]
   playServicesAds = "23.6.0"

   [libraries]
   play-services-ads = { group = "com.google.android.gms", name = "play-services-ads", version.ref = "playServicesAds" }
   ```
2. In `app/build.gradle.kts`:
   ```kotlin
   dependencies {
       implementation(libs.play.services.ads)
   }
   ```
3. In `QuranBaantoApp.kt`:
   ```kotlin
   MobileAds.initialize(this) {}
   ```
4. In `ServerSupportAdManager.kt`:
   Load and show `RewardedAd`:
   ```kotlin
   RewardedAd.load(
       context,
       BuildConfig.ADMOB_REWARDED_AD_UNIT_ID,
       AdRequest.Builder().build(),
       object : RewardedAdLoadCallback() {
           override fun onAdLoaded(ad: RewardedAd) {
               ad.show(activity) { rewardItem ->
                   // Reward earned
               }
           }
       }
   )
   ```

---

## 5. Testing with Google Test Ad Units Before Going Live
Google strictly bans clicking or loading live production ads during development. Always verify that test ads load using Google's official test ad unit:
- **Test App ID:** `ca-app-pub-3940256099942544~3347511713`
- **Test Rewarded Video Ad Unit:** `ca-app-pub-3940256099942544/5224354917`
These are the pre-configured defaults in `.env.example`.
