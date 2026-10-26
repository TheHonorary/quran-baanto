# ==============================================================================
# Quran Baanto - ProGuard & R8 Optimization Rules
# ==============================================================================

# 1. Strip debug and verbose logs from release binaries to protect internal state
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# 2. Local Database & Room Entities
-keep class com.example.data.db.** { *; }
-dontwarn androidx.room.paging.**

# 3. Firebase & Firestore Data Transfer Objects (DTOs)
-keep class com.example.data.firebase.** { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# 4. DataStore & Preferences Serialization
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
    <methods>;
}

# 5. ZXing QR Code Generation
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# 6. Quran Models & JSON Assets
-keep class com.example.data.quran.** { *; }

# 7. AdMob & Google Play Services
-keep class com.google.android.gms.ads.** { *; }
-keep public class com.google.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# 8. Kotlin Coroutines & Flow
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
