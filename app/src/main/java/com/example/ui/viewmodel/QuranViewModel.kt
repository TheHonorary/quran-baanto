package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.BookmarkManager
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ReadingTheme {
    DEFAULT, SEPIA, DARK
}

class QuranViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val bookmarkManager = BookmarkManager(context)

    init {
        // Initialize offline Qur'an repository asynchronously on background thread
        viewModelScope.launch(Dispatchers.IO) {
            QuranRepository.initialize(context)
        }
    }

    val paraList: List<ParaInfo> = QuranRepository.ALL_PARAS
    val surahList: List<SurahInfo> = QuranRepository.ALL_SURAHS

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

    private val _lastReadBookmark = MutableStateFlow<LastReadBookmark?>(bookmarkManager.getLastRead())
    val lastReadBookmark: StateFlow<LastReadBookmark?> = _lastReadBookmark.asStateFlow()

    fun selectPara(paraNumber: Int?) {
        _activeReadMode.value = ReadMode.PARA
        _selectedParaNumber.value = paraNumber
        _selectedSurahNumber.value = null
        _surahDetail.value = null

        if (paraNumber != null) {
            _isLoading.value = true
            viewModelScope.launch(Dispatchers.IO) {
                val detail = QuranRepository.getParaDetail(context, paraNumber)
                val bookmark = LastReadBookmark(
                    mode = ReadMode.PARA,
                    id = paraNumber,
                    title = "Para $paraNumber — ${detail.info.transliteration}",
                    arabicTitle = detail.info.arabicTitle,
                    subtitle = detail.info.surahRange
                )
                withContext(Dispatchers.Main) {
                    _paraDetail.value = detail
                    _lastReadBookmark.value = bookmark
                    _isLoading.value = false
                    bookmarkManager.saveLastRead(bookmark)
                }
            }
        } else {
            _paraDetail.value = null
            _isLoading.value = false
        }
    }

    fun selectSurah(surahNumber: Int?) {
        _activeReadMode.value = ReadMode.SURAH
        _selectedSurahNumber.value = surahNumber
        _selectedParaNumber.value = null
        _paraDetail.value = null

        if (surahNumber != null) {
            _isLoading.value = true
            viewModelScope.launch(Dispatchers.IO) {
                val detail = QuranRepository.getSurahDetail(context, surahNumber)
                val bookmark = LastReadBookmark(
                    mode = ReadMode.SURAH,
                    id = surahNumber,
                    title = "Surah ${detail.info.number} — ${detail.info.transliteration}",
                    arabicTitle = detail.info.arabicName,
                    subtitle = "${detail.info.englishMeaning} • Para ${detail.info.startingPara}"
                )
                withContext(Dispatchers.Main) {
                    _surahDetail.value = detail
                    _lastReadBookmark.value = bookmark
                    _isLoading.value = false
                    bookmarkManager.saveLastRead(bookmark)
                }
            }
        } else {
            _surahDetail.value = null
            _isLoading.value = false
        }
    }

    fun adjustFontSize(delta: Int) {
        val newSize = (_fontSizeSp.value + delta).coerceIn(18, 42)
        _fontSizeSp.value = newSize
    }

    fun setReadingTheme(theme: ReadingTheme) {
        _readingTheme.value = theme
    }

    fun setArabicFontStyle(style: ArabicFontStyle) {
        _arabicFontStyle.value = style
        bookmarkManager.saveFontStyle(style)
    }

    fun toggleKeepScreenOn() {
        val newState = !_keepScreenOn.value
        _keepScreenOn.value = newState
        bookmarkManager.saveKeepScreenOn(newState)
    }

    fun saveBookmark(bookmark: LastReadBookmark) {
        _lastReadBookmark.value = bookmark
        bookmarkManager.saveLastRead(bookmark)
    }
}
