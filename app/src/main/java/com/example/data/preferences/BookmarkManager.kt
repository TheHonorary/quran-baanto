package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.data.quran.ArabicFontStyle
import com.example.data.quran.LastReadBookmark
import com.example.data.quran.ReadMode
import org.json.JSONArray
import org.json.JSONObject

class BookmarkManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("quran_baanto_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ALL_BOOKMARKS = "all_bookmarks_json"
        private const val KEY_ARABIC_FONT_STYLE = "arabic_font_style"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
    }

    @Synchronized
    fun getAllBookmarks(): List<LastReadBookmark> {
        val jsonStr = prefs.getString(KEY_ALL_BOOKMARKS, null)
        if (jsonStr.isNullOrBlank()) {
            val legacy = getLegacyLastRead()
            if (legacy != null) {
                saveAllBookmarks(listOf(legacy))
                return listOf(legacy)
            }
            return emptyList()
        }

        val list = mutableListOf<LastReadBookmark>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val modeStr = obj.optString("mode", ReadMode.PARA.name)
                val mode = try {
                    ReadMode.valueOf(modeStr)
                } catch (e: Exception) {
                    ReadMode.PARA
                }
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

    @Synchronized
    fun getLatestBookmark(): LastReadBookmark? {
        return getAllBookmarks().firstOrNull()
    }

    @Synchronized
    fun addOrUpdateBookmark(bookmark: LastReadBookmark) {
        val currentList = getAllBookmarks().toMutableList()
        // Remove existing bookmark matching the exact same ayah if present
        currentList.removeAll {
            if (bookmark.mode == ReadMode.SURAH) {
                it.mode == ReadMode.SURAH && it.id == bookmark.id && it.ayahNumber == bookmark.ayahNumber
            } else {
                it.mode == ReadMode.PARA && it.id == bookmark.id && it.surahNumber == bookmark.surahNumber && it.ayahNumber == bookmark.ayahNumber
            }
        }
        // Insert new bookmark at the beginning (latest)
        currentList.add(0, bookmark)
        saveAllBookmarks(currentList)
    }

    @Synchronized
    fun removeBookmark(mode: ReadMode, id: Int, surahNumber: Int, ayahNumber: Int) {
        val currentList = getAllBookmarks().toMutableList()
        currentList.removeAll {
            if (mode == ReadMode.SURAH) {
                it.mode == ReadMode.SURAH && it.id == id && it.ayahNumber == ayahNumber
            } else {
                it.mode == ReadMode.PARA && it.id == id && it.surahNumber == surahNumber && it.ayahNumber == ayahNumber
            }
        }
        saveAllBookmarks(currentList)
    }

    @Synchronized
    fun clearAllBookmarks() {
        prefs.edit().remove(KEY_ALL_BOOKMARKS).apply()
        clearLegacyLastRead()
    }

    private fun saveAllBookmarks(list: List<LastReadBookmark>) {
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
        prefs.edit().putString(KEY_ALL_BOOKMARKS, array.toString()).apply()
    }

    // Legacy migration support
    fun saveLastRead(bookmark: LastReadBookmark) {
        addOrUpdateBookmark(bookmark)
    }

    fun clearLastRead() {
        val latest = getLatestBookmark()
        if (latest != null) {
            removeBookmark(latest.mode, latest.id, latest.surahNumber, latest.ayahNumber)
        }
    }

    fun getLastRead(): LastReadBookmark? {
        return getLatestBookmark()
    }

    private fun getLegacyLastRead(): LastReadBookmark? {
        val modeStr = prefs.getString("last_read_mode", null) ?: return null
        val mode = try {
            ReadMode.valueOf(modeStr)
        } catch (e: Exception) {
            ReadMode.PARA
        }
        val id = prefs.getInt("last_read_id", 1)
        val title = prefs.getString("last_read_title", "Para 1") ?: "Para 1"
        val arabicTitle = prefs.getString("last_read_arabic_title", "آلم") ?: "آلم"
        val subtitle = prefs.getString("last_read_subtitle", "") ?: ""
        val surahNumber = prefs.getInt("last_read_surah_num", 1)
        val ayahNumber = prefs.getInt("last_read_ayah_num", 1)
        val ayahText = prefs.getString("last_read_ayah_text", "") ?: ""
        val timestamp = prefs.getLong("last_read_timestamp", System.currentTimeMillis())

        return LastReadBookmark(
            mode = mode,
            id = id,
            title = title,
            arabicTitle = arabicTitle,
            subtitle = subtitle,
            surahNumber = surahNumber,
            ayahNumber = ayahNumber,
            ayahText = ayahText,
            timestamp = timestamp
        )
    }

    private fun clearLegacyLastRead() {
        prefs.edit().apply {
            remove("last_read_mode")
            remove("last_read_id")
            remove("last_read_title")
            remove("last_read_arabic_title")
            remove("last_read_subtitle")
            remove("last_read_surah_num")
            remove("last_read_ayah_num")
            remove("last_read_ayah_text")
            remove("last_read_timestamp")
            apply()
        }
    }

    fun saveFontStyle(style: ArabicFontStyle) {
        prefs.edit().putString("arabic_font_style", style.name).apply()
    }

    fun getFontStyle(): ArabicFontStyle {
        val styleStr = prefs.getString("arabic_font_style", ArabicFontStyle.UTHMANI.name)
        return try {
            ArabicFontStyle.valueOf(styleStr ?: ArabicFontStyle.UTHMANI.name)
        } catch (e: Exception) {
            ArabicFontStyle.UTHMANI
        }
    }

    fun saveKeepScreenOn(enabled: Boolean) {
        prefs.edit().putBoolean("keep_screen_on", enabled).apply()
    }

    fun getKeepScreenOn(): Boolean {
        return prefs.getBoolean("keep_screen_on", true)
    }
}
