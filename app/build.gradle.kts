import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  // alias(libs.plugins.roborazzi)
  alias(libs.plugins.google.services)
}

val envProps = Properties().apply {
  val envFile = rootProject.file(".env")
  if (envFile.exists()) {
    envFile.inputStream().use { load(it) }
  }
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "quran.baanto"
    minSdk = 24
    targetSdk = 36
    versionCode = 3
    versionName = "3.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    val admobAppId = envProps.getProperty("ADMOB_APP_ID", "ca-app-pub-3940256099942544~3347511713")
    val defaultRewardedId = envProps.getProperty("ADMOB_REWARDED_AD_UNIT_ID", "ca-app-pub-3940256099942544/5224354917")
    val admobRoomRewardedId = envProps.getProperty("ADMOB_ROOM_REWARDED_AD_UNIT_ID", defaultRewardedId)
    val admobFreezeRewardedId = envProps.getProperty("ADMOB_FREEZE_REWARDED_AD_UNIT_ID", defaultRewardedId)
    val firebaseProjectId = envProps.getProperty("FIREBASE_PROJECT_ID", "quran-baanto")
    val firebaseProjectNumber = envProps.getProperty("FIREBASE_PROJECT_NUMBER", "895060640103")
    val firebaseAppId = envProps.getProperty("FIREBASE_APP_ID", "1:895060640103:android:c4576140c2b29ffb662fc2")
    val firebaseStorageBucket = envProps.getProperty("FIREBASE_STORAGE_BUCKET", "quran-baanto.firebasestorage.app")
    val firebaseApiKey = envProps.getProperty("FIREBASE_API_KEY", "AIzaSyC6z77kNhAhRU8uw0RkNopdt5KWcH0smf8")

    manifestPlaceholders["ADMOB_APP_ID"] = admobAppId

    buildConfigField("String", "ADMOB_APP_ID", "\"$admobAppId\"")
    buildConfigField("String", "ADMOB_REWARDED_AD_UNIT_ID", "\"$defaultRewardedId\"")
    buildConfigField("String", "ADMOB_ROOM_REWARDED_AD_UNIT_ID", "\"$admobRoomRewardedId\"")
    buildConfigField("String", "ADMOB_FREEZE_REWARDED_AD_UNIT_ID", "\"$admobFreezeRewardedId\"")
    buildConfigField("String", "FIREBASE_PROJECT_ID", "\"$firebaseProjectId\"")
    buildConfigField("String", "FIREBASE_PROJECT_NUMBER", "\"$firebaseProjectNumber\"")
    buildConfigField("String", "FIREBASE_APP_ID", "\"$firebaseAppId\"")
    buildConfigField("String", "FIREBASE_STORAGE_BUCKET", "\"$firebaseStorageBucket\"")
    buildConfigField("String", "FIREBASE_API_KEY", "\"$firebaseApiKey\"")
  }

  signingConfigs {
    create("release") {
      val envPath = System.getenv("KEYSTORE_PATH")
      val candidateFile = when {
        !envPath.isNullOrBlank() && file(envPath).isAbsolute -> file(envPath)
        !envPath.isNullOrBlank() -> rootProject.file(envPath)
        rootProject.file("my-upload-key.jks").exists() -> rootProject.file("my-upload-key.jks")
        file("my-upload-key.jks").exists() -> file("my-upload-key.jks")
        rootProject.file("debug.keystore").exists() -> rootProject.file("debug.keystore")
        else -> null
      }

      if (candidateFile != null && candidateFile.exists()) {
        storeFile = candidateFile
        storePassword = System.getenv("STORE_PASSWORD") ?: "android"
        keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
        keyPassword = System.getenv("KEY_PASSWORD") ?: "android"
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      val releaseSigning = signingConfigs.findByName("release")
      if (releaseSigning?.storeFile != null && releaseSigning.storeFile!!.exists()) {
        signingConfig = releaseSigning
      } else {
        signingConfig = signingConfigs.getByName("debug")
      }
    }
    debug { }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.core.splashscreen)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  // implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  implementation(libs.firebase.auth)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services)
  implementation(libs.googleid)
  // implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.zxing.core)
  implementation(libs.play.services.ads)
  // implementation(libs.logging.interceptor)
  // implementation(libs.moshi.kotlin)
  // implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  // implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  // testImplementation(libs.roborazzi)
  // testImplementation(libs.roborazzi.compose)
  // testImplementation(libs.roborazzi.junit.rule)
  // androidTestImplementation(platform(libs.androidx.compose.bom))
  // androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  // androidTestImplementation(libs.androidx.espresso.core)
  // androidTestImplementation(libs.androidx.junit)
  // androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  // "ksp"(libs.moshi.kotlin.codegen)
}
