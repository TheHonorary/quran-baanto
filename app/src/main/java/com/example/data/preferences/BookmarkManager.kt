package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.data.quran.ArabicFontStyle
import com.example.data.quran.LastReadBookmark
import com.example.data.quran.ReadMode

class BookmarkManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("quran_baanto_prefs", Context.MODE_PRIVATE)

    fun saveLastRead(bookmark: LastReadBookmark) {
        prefs.edit().apply {
            putString("last_read_mode", bookmark.mode.name)
            putInt("last_read_id", bookmark.id)
            putString("last_read_title", bookmark.title)
            putString("last_read_arabic_title", bookmark.arabicTitle)
            putString("last_read_subtitle", bookmark.subtitle)
            putLong("last_read_timestamp", bookmark.timestamp)
            apply()
        }
    }

    fun getLastRead(): LastReadBookmark? {
        val modeStr = prefs.getString("last_read_mode", null) ?: return null
        val mode = try {
            ReadMode.valueOf(modeStr)
        } catch (e: Exception) {
            ReadMode.PARA
        }
        val id = prefs.getInt("last_read_id", 1)
        val title = prefs.getString("last_read_title", "Para 1") ?: "Para 1"
        val arabicTitle = prefs.getString("last_read_arabic_title", "آلم") ?: "آلم"
        val subtitle = prefs.getString("last_read_subtitle", "Al-Fatihah 1:1 – Al-Baqarah 2:141") ?: ""
        val timestamp = prefs.getLong("last_read_timestamp", System.currentTimeMillis())

        return LastReadBookmark(
            mode = mode,
            id = id,
            title = title,
            arabicTitle = arabicTitle,
            subtitle = subtitle,
            timestamp = timestamp
        )
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
