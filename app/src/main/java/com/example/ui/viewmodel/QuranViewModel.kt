package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.BookmarkManager
import com.example.data.preferences.UserPreferencesDataStore
import com.example.data.quran.ArabicFontStyle
import com.example.data.quran.LastReadBookmark
import com.example.data.quran.ParaDetail
import com.example.data.quran.ParaInfo
import com.example.data.quran.QuranRepository
import com.example.data.quran.ReadMode
import com.example.data.quran.SurahDetail
import com.example.data.quran.SurahInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ReadingTheme {
    DEFAULT, SEPIA, DARK
}

class QuranViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val bookmarkManager = BookmarkManager(context)
    private val preferencesDataStore = UserPreferencesDataStore.getInstance(context)

    val paraList: List<ParaInfo> = QuranRepository.ALL_PARAS
    val surahList: List<SurahInfo> = QuranRepository.ALL_SURAHS

    // App Dark Theme preference backed by DataStore
    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // Active Reader Mode (Para vs Surah)
    private val _activeReadMode = MutableStateFlow(ReadMode.PARA)
    val activeReadMode: StateFlow<ReadMode> = _activeReadMode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedParaNumber = MutableStateFlow<Int?>(null)
    val selectedParaNumber: StateFlow<Int?> = _selectedParaNumber.asStateFlow()

    private val _paraDetail = MutableStateFlow<ParaDetail?>(null)
    val paraDetail: StateFlow<ParaDetail?> = _paraDetail.asStateFlow()

    private val _selectedSurahNumber = MutableStateFlow<Int?>(null)
    val selectedSurahNumber: StateFlow<Int?> = _selectedSurahNumber.asStateFlow()

    private val _surahDetail = MutableStateFlow<SurahDetail?>(null)
    val surahDetail: StateFlow<SurahDetail?> = _surahDetail.asStateFlow()

    private val _fontSizeSp = MutableStateFlow(24) // Default 24sp for Arabic text
    val fontSizeSp: StateFlow<Int> = _fontSizeSp.asStateFlow()

    private val _readingTheme = MutableStateFlow(ReadingTheme.DEFAULT)
    val readingTheme: StateFlow<ReadingTheme> = _readingTheme.asStateFlow()

    private val _arabicFontStyle = MutableStateFlow(bookmarkManager.getFontStyle())
    val arabicFontStyle: StateFlow<ArabicFontStyle> = _arabicFontStyle.asStateFlow()

    private val _keepScreenOn = MutableStateFlow(bookmarkManager.getKeepScreenOn())
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    private val _allBookmarks = MutableStateFlow<List<LastReadBookmark>>(bookmarkManager.getAllBookmarks())
    val allBookmarks: StateFlow<List<LastReadBookmark>> = _allBookmarks.asStateFlow()

    private val _lastReadBookmark = MutableStateFlow<LastReadBookmark?>(bookmarkManager.getLatestBookmark())
    val lastReadBookmark: StateFlow<LastReadBookmark?> = _lastReadBookmark.asStateFlow()

    private val _targetScrollAyah = MutableStateFlow<Int?>(null)
    val targetScrollAyah: StateFlow<Int?> = _targetScrollAyah.asStateFlow()

    private val _targetScrollSurah = MutableStateFlow<Int?>(null)
    val targetScrollSurah: StateFlow<Int?> = _targetScrollSurah.asStateFlow()

    init {
        // Initialize offline Qur'an repository asynchronously on background thread
        viewModelScope.launch(Dispatchers.IO) {
            QuranRepository.initialize(context)
        }

        // Collect persisted settings from DataStore
        viewModelScope.launch {
            preferencesDataStore.userSettingsFlow.collectLatest { settings ->
                _isDarkTheme.value = settings.isDarkTheme
                _arabicFontStyle.value = settings.arabicFontStyle
                _fontSizeSp.value = settings.fontSizeSp
                _keepScreenOn.value = settings.keepScreenOn
                _readingTheme.value = settings.readingTheme
            }
        }

        // Collect persisted bookmarks from DataStore while preserving existing SharedPreferences bookmarks
        viewModelScope.launch {
            val initialLocalBookmarks = bookmarkManager.getAllBookmarks()
            if (initialLocalBookmarks.isNotEmpty()) {
                _allBookmarks.value = initialLocalBookmarks
                _lastReadBookmark.value = initialLocalBookmarks.firstOrNull()
            }
            preferencesDataStore.bookmarksFlow.collectLatest { storeBookmarks ->
                val localBookmarks = bookmarkManager.getAllBookmarks()
                val merged = if (storeBookmarks.isNotEmpty()) {
                    storeBookmarks
                } else {
                    localBookmarks
                }
                _allBookmarks.value = merged
                _lastReadBookmark.value = merged.firstOrNull()
            }
        }
    }

    fun setDarkTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
        viewModelScope.launch {
            preferencesDataStore.setDarkTheme(isDark)
        }
    }

    fun clearTargetScrollAyah() {
        _targetScrollAyah.value = null
        _targetScrollSurah.value = null
    }

    fun selectPara(paraNumber: Int?, targetAyah: Int? = null, targetSurah: Int? = null) {
        _activeReadMode.value = ReadMode.PARA
        _selectedParaNumber.value = paraNumber
        _selectedSurahNumber.value = null
        _surahDetail.value = null

        val matchingBookmark = _allBookmarks.value.firstOrNull { it.mode == ReadMode.PARA && it.id == paraNumber }
        val effectiveAyah = targetAyah ?: matchingBookmark?.ayahNumber
        val effectiveSurah = targetSurah ?: matchingBookmark?.surahNumber
        _targetScrollAyah.value = effectiveAyah
        _targetScrollSurah.value = effectiveSurah

        if (paraNumber != null) {
            _isLoading.value = true
            viewModelScope.launch(Dispatchers.IO) {
                val detail = QuranRepository.getParaDetail(context, paraNumber)
                withContext(Dispatchers.Main) {
                    _paraDetail.value = detail
                    _isLoading.value = false
                }
            }
        } else {
            _paraDetail.value = null
            _isLoading.value = false
        }
    }

    fun selectSurah(surahNumber: Int?, targetAyah: Int? = null) {
        _activeReadMode.value = ReadMode.SURAH
        _selectedSurahNumber.value = surahNumber
        _selectedParaNumber.value = null
        _paraDetail.value = null

        val matchingBookmark = _allBookmarks.value.firstOrNull { it.mode == ReadMode.SURAH && it.id == surahNumber }
        val effectiveAyah = targetAyah ?: matchingBookmark?.ayahNumber
        _targetScrollAyah.value = effectiveAyah
        _targetScrollSurah.value = surahNumber

        if (surahNumber != null) {
            _isLoading.value = true
            viewModelScope.launch(Dispatchers.IO) {
                val detail = QuranRepository.getSurahDetail(context, surahNumber)
                withContext(Dispatchers.Main) {
                    _surahDetail.value = detail
                    _isLoading.value = false
                }
            }
        } else {
            _surahDetail.value = null
            _isLoading.value = false
        }
    }

    fun bookmarkAyah(
        mode: ReadMode,
        id: Int,
        surahNumber: Int,
        surahNameEn: String,
        surahNameAr: String,
        ayahNumber: Int,
        ayahText: String
    ) {
        val title = if (mode == ReadMode.SURAH) {
            "Surah $surahNumber: $surahNameEn · Ayah $ayahNumber"
        } else {
            "Para $id · $surahNameEn, Ayah $ayahNumber"
        }
        val subtitle = if (mode == ReadMode.SURAH) {
            "Ayah $ayahNumber"
        } else {
            "Surah $surahNameEn, Ayah $ayahNumber"
        }
        val bookmark = LastReadBookmark(
            mode = mode,
            id = id,
            title = title,
            arabicTitle = surahNameAr,
            subtitle = subtitle,
            surahNumber = surahNumber,
            ayahNumber = ayahNumber,
            ayahText = ayahText.take(70),
            timestamp = System.currentTimeMillis()
        )
        bookmarkManager.addOrUpdateBookmark(bookmark)
        val updated = bookmarkManager.getAllBookmarks()
        _allBookmarks.value = updated
        _lastReadBookmark.value = updated.firstOrNull()
        viewModelScope.launch {
            preferencesDataStore.addOrUpdateBookmark(bookmark)
        }
    }

    fun removeBookmark(mode: ReadMode, id: Int, surahNumber: Int, ayahNumber: Int) {
        bookmarkManager.removeBookmark(mode, id, surahNumber, ayahNumber)
        val updated = bookmarkManager.getAllBookmarks()
        _allBookmarks.value = updated
        _lastReadBookmark.value = updated.firstOrNull()
        viewModelScope.launch {
            preferencesDataStore.removeBookmark(mode, id, surahNumber, ayahNumber)
        }
    }

    fun removeBookmark(bookmark: LastReadBookmark) {
        removeBookmark(bookmark.mode, bookmark.id, bookmark.surahNumber, bookmark.ayahNumber)
    }

    fun isAyahBookmarked(mode: ReadMode, id: Int, surahNumber: Int, ayahNumber: Int): Boolean {
        return _allBookmarks.value.any { b ->
            if (mode == ReadMode.SURAH) {
                b.mode == ReadMode.SURAH && b.id == id && b.ayahNumber == ayahNumber
            } else {
                b.mode == ReadMode.PARA && b.id == id && b.surahNumber == surahNumber && b.ayahNumber == ayahNumber
            }
        }
    }

    fun adjustFontSize(delta: Int) {
        val newSize = (_fontSizeSp.value + delta).coerceIn(18, 42)
        setFontSize(newSize)
    }

    fun setFontSize(sizeSp: Int) {
        val clamped = sizeSp.coerceIn(18, 42)
        _fontSizeSp.value = clamped
        viewModelScope.launch {
            preferencesDataStore.setFontSize(clamped)
        }
    }

    fun setReadingTheme(theme: ReadingTheme) {
        _readingTheme.value = theme
        viewModelScope.launch {
            preferencesDataStore.setReadingTheme(theme)
        }
    }

    fun setArabicFontStyle(style: ArabicFontStyle) {
        _arabicFontStyle.value = style
        bookmarkManager.saveFontStyle(style)
        viewModelScope.launch {
            preferencesDataStore.setArabicFontStyle(style)
        }
    }

    fun toggleKeepScreenOn() {
        val newState = !_keepScreenOn.value
        _keepScreenOn.value = newState
        bookmarkManager.saveKeepScreenOn(newState)
        viewModelScope.launch {
            preferencesDataStore.setKeepScreenOn(newState)
        }
    }

    fun saveBookmark(bookmark: LastReadBookmark) {
        bookmarkManager.saveLastRead(bookmark)
        val updated = bookmarkManager.getAllBookmarks()
        _allBookmarks.value = updated
        _lastReadBookmark.value = updated.firstOrNull()
        viewModelScope.launch {
            preferencesDataStore.addOrUpdateBookmark(bookmark)
        }
    }
}
