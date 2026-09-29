package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.quran.ArabicFontStyle
import com.example.data.quran.LastReadBookmark
import com.example.data.quran.ReadMode
import com.example.ui.viewmodel.ReadingTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings_and_session")

data class UserSettings(
    val isDarkTheme: Boolean = false,
    val arabicFontStyle: ArabicFontStyle = ArabicFontStyle.UTHMANI,
    val fontSizeSp: Int = 24,
    val keepScreenOn: Boolean = true,
    val readingTheme: ReadingTheme = ReadingTheme.DEFAULT
)

data class SessionState(
    val lastReadBookmark: LastReadBookmark? = null
)

class UserPreferencesDataStore(private val context: Context) {

    companion object {
        val KEY_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val KEY_ARABIC_FONT_STYLE = stringPreferencesKey("arabic_font_style")
        val KEY_FONT_SIZE_SP = intPreferencesKey("font_size_sp")
        val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val KEY_READING_THEME = stringPreferencesKey("reading_theme")
        val KEY_ALL_BOOKMARKS_JSON = stringPreferencesKey("all_bookmarks_json")

        // Session state keys
        val KEY_SESSION_MODE = stringPreferencesKey("session_mode")
        val KEY_SESSION_ID = intPreferencesKey("session_id")
        val KEY_SESSION_SURAH_NUM = intPreferencesKey("session_surah_num")
        val KEY_SESSION_AYAH_NUM = intPreferencesKey("session_ayah_num")
        val KEY_SESSION_AYAH_TEXT = stringPreferencesKey("session_ayah_text")
        val KEY_SESSION_TITLE = stringPreferencesKey("session_title")
        val KEY_SESSION_ARABIC_TITLE = stringPreferencesKey("session_arabic_title")
        val KEY_SESSION_SUBTITLE = stringPreferencesKey("session_subtitle")
        val KEY_SESSION_TIMESTAMP = longPreferencesKey("session_timestamp")

        @Volatile
        private var INSTANCE: UserPreferencesDataStore? = null

        fun getInstance(context: Context): UserPreferencesDataStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferencesDataStore(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    init {
        // Migrate from legacy SharedPreferences on first run if DataStore is empty
        CoroutineScope(Dispatchers.IO).launch {
            migrateFromLegacyPreferencesIfNeeded()
        }
    }

    private suspend fun migrateFromLegacyPreferencesIfNeeded() {
        try {
            val prefs = context.dataStore.data.first()
            if (!prefs.contains(KEY_ALL_BOOKMARKS_JSON)) {
                val sharedPrefs = context.getSharedPreferences("quran_baanto_prefs", Context.MODE_PRIVATE)
                val legacyBookmarksJson = sharedPrefs.getString("all_bookmarks_json", null)
                val legacyFontStyle = sharedPrefs.getString("arabic_font_style", null)
                val legacyKeepScreenOn = if (sharedPrefs.contains("keep_screen_on")) {
                    sharedPrefs.getBoolean("keep_screen_on", true)
                } else null

                context.dataStore.edit { preferences ->
                    if (legacyBookmarksJson != null) {
                        preferences[KEY_ALL_BOOKMARKS_JSON] = legacyBookmarksJson
                    }
                    if (legacyFontStyle != null) {
                        preferences[KEY_ARABIC_FONT_STYLE] = legacyFontStyle
                    }
                    if (legacyKeepScreenOn != null) {
                        preferences[KEY_KEEP_SCREEN_ON] = legacyKeepScreenOn
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val isDark = preferences[KEY_DARK_THEME] ?: false
            val fontStyleStr = preferences[KEY_ARABIC_FONT_STYLE] ?: ArabicFontStyle.UTHMANI.name
            val fontStyle = try {
                ArabicFontStyle.valueOf(fontStyleStr)
            } catch (e: Exception) {
                ArabicFontStyle.UTHMANI
            }
            val fontSize = preferences[KEY_FONT_SIZE_SP] ?: 24
            val keepAwake = preferences[KEY_KEEP_SCREEN_ON] ?: true
            val readingThemeStr = preferences[KEY_READING_THEME] ?: ReadingTheme.DEFAULT.name
            val rTheme = try {
                ReadingTheme.valueOf(readingThemeStr)
            } catch (e: Exception) {
                ReadingTheme.DEFAULT
            }
            UserSettings(
                isDarkTheme = isDark,
                arabicFontStyle = fontStyle,
                fontSizeSp = fontSize,
                keepScreenOn = keepAwake,
                readingTheme = rTheme
            )
        }

    val sessionStateFlow: Flow<SessionState> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val modeStr = preferences[KEY_SESSION_MODE]
            if (modeStr == null) {
                SessionState(lastReadBookmark = null)
            } else {
                val mode = try { ReadMode.valueOf(modeStr) } catch (e: Exception) { ReadMode.PARA }
                val bookmark = LastReadBookmark(
                    mode = mode,
                    id = preferences[KEY_SESSION_ID] ?: 1,
                    title = preferences[KEY_SESSION_TITLE] ?: "",
                    arabicTitle = preferences[KEY_SESSION_ARABIC_TITLE] ?: "",
                    subtitle = preferences[KEY_SESSION_SUBTITLE] ?: "",
                    surahNumber = preferences[KEY_SESSION_SURAH_NUM] ?: 1,
                    ayahNumber = preferences[KEY_SESSION_AYAH_NUM] ?: 1,
                    ayahText = preferences[KEY_SESSION_AYAH_TEXT] ?: "",
                    timestamp = preferences[KEY_SESSION_TIMESTAMP] ?: System.currentTimeMillis()
                )
                SessionState(lastReadBookmark = bookmark)
            }
        }

    val bookmarksFlow: Flow<List<LastReadBookmark>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val jsonStr = preferences[KEY_ALL_BOOKMARKS_JSON] ?: ""
            parseBookmarksJson(jsonStr)
        }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DARK_THEME] = isDark
        }
    }

    suspend fun setArabicFontStyle(style: ArabicFontStyle) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ARABIC_FONT_STYLE] = style.name
        }
    }

    suspend fun setFontSize(fontSizeSp: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_FONT_SIZE_SP] = fontSizeSp
        }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_KEEP_SCREEN_ON] = enabled
        }
    }

    suspend fun setReadingTheme(theme: ReadingTheme) {
        context.dataStore.edit { preferences ->
            preferences[KEY_READING_THEME] = theme.name
        }
    }

    suspend fun saveSessionState(bookmark: LastReadBookmark) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SESSION_MODE] = bookmark.mode.name
            preferences[KEY_SESSION_ID] = bookmark.id
            preferences[KEY_SESSION_SURAH_NUM] = bookmark.surahNumber
            preferences[KEY_SESSION_AYAH_NUM] = bookmark.ayahNumber
            preferences[KEY_SESSION_AYAH_TEXT] = bookmark.ayahText
            preferences[KEY_SESSION_TITLE] = bookmark.title
            preferences[KEY_SESSION_ARABIC_TITLE] = bookmark.arabicTitle
            preferences[KEY_SESSION_SUBTITLE] = bookmark.subtitle
            preferences[KEY_SESSION_TIMESTAMP] = bookmark.timestamp
        }
    }

    suspend fun addOrUpdateBookmark(bookmark: LastReadBookmark) {
        context.dataStore.edit { preferences ->
            val currentList = parseBookmarksJson(preferences[KEY_ALL_BOOKMARKS_JSON] ?: "").toMutableList()
            currentList.removeAll {
                if (bookmark.mode == ReadMode.SURAH) {
                    it.mode == ReadMode.SURAH && it.id == bookmark.id && it.ayahNumber == bookmark.ayahNumber
                } else {
                    it.mode == ReadMode.PARA && it.id == bookmark.id && it.surahNumber == bookmark.surahNumber && it.ayahNumber == bookmark.ayahNumber
                }
            }
            currentList.add(0, bookmark)
            preferences[KEY_ALL_BOOKMARKS_JSON] = serializeBookmarksJson(currentList)
            // Also update session state to the latest bookmarked Ayah
            preferences[KEY_SESSION_MODE] = bookmark.mode.name
            preferences[KEY_SESSION_ID] = bookmark.id
            preferences[KEY_SESSION_SURAH_NUM] = bookmark.surahNumber
            preferences[KEY_SESSION_AYAH_NUM] = bookmark.ayahNumber
            preferences[KEY_SESSION_AYAH_TEXT] = bookmark.ayahText
            preferences[KEY_SESSION_TITLE] = bookmark.title
            preferences[KEY_SESSION_ARABIC_TITLE] = bookmark.arabicTitle
            preferences[KEY_SESSION_SUBTITLE] = bookmark.subtitle
            preferences[KEY_SESSION_TIMESTAMP] = bookmark.timestamp
        }
    }

    suspend fun removeBookmark(mode: ReadMode, id: Int, surahNumber: Int, ayahNumber: Int) {
        context.dataStore.edit { preferences ->
            val currentList = parseBookmarksJson(preferences[KEY_ALL_BOOKMARKS_JSON] ?: "").toMutableList()
            currentList.removeAll {
                if (mode == ReadMode.SURAH) {
                    it.mode == ReadMode.SURAH && it.id == id && it.ayahNumber == ayahNumber
                } else {
                    it.mode == ReadMode.PARA && it.id == id && it.surahNumber == surahNumber && it.ayahNumber == ayahNumber
                }
            }
            preferences[KEY_ALL_BOOKMARKS_JSON] = serializeBookmarksJson(currentList)
        }
    }

    suspend fun clearAllBookmarks() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_ALL_BOOKMARKS_JSON)
        }
    }

    // Synchronous helpers for callers that require immediate snapshot
    fun getInitialSettingsBlocking(): UserSettings {
        return runBlocking(Dispatchers.IO) {
            try {
                userSettingsFlow.first()
            } catch (e: Exception) {
                UserSettings()
            }
        }
    }

    fun getInitialBookmarksBlocking(): List<LastReadBookmark> {
        return runBlocking(Dispatchers.IO) {
            try {
                bookmarksFlow.first()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private fun parseBookmarksJson(jsonStr: String): List<LastReadBookmark> {
        if (jsonStr.isBlank()) return emptyList()
        val list = mutableListOf<LastReadBookmark>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val modeStr = obj.optString("mode", ReadMode.PARA.name)
                val mode = try { ReadMode.valueOf(modeStr) } catch (e: Exception) { ReadMode.PARA }
                list.add(
                    LastReadBookmark(
                        mode = mode,
                        id = obj.optInt("id", 1),
                        title = obj.optString("title", ""),
                        arabicTitle = obj.optString("arabicTitle", ""),
                        subtitle = obj.optString("subtitle", ""),
                        surahNumber = obj.optInt("surahNumber", 1),
                        ayahNumber = obj.optInt("ayahNumber", 1),
                        ayahText = obj.optString("ayahText", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list.sortedByDescending { it.timestamp }
    }

    private fun serializeBookmarksJson(list: List<LastReadBookmark>): String {
        val array = JSONArray()
        for (bm in list) {
            val obj = JSONObject()
            obj.put("mode", bm.mode.name)
            obj.put("id", bm.id)
            obj.put("title", bm.title)
            obj.put("arabicTitle", bm.arabicTitle)
            obj.put("subtitle", bm.subtitle)
            obj.put("surahNumber", bm.surahNumber)
            obj.put("ayahNumber", bm.ayahNumber)
            obj.put("ayahText", bm.ayahText)
            obj.put("timestamp", bm.timestamp)
            array.put(obj)
        }
        return array.toString()
    }
}
