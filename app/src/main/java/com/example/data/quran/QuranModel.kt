package com.example.data.quran

enum class RevelationType {
    MAKKI, MADANI
}

enum class ReadMode {
    PARA, SURAH
}

enum class ArabicFontStyle {
    UTHMANI, INDO_PAK
}

data class ParaInfo(
    val number: Int,
    val arabicTitle: String,
    val transliteration: String,
    val surahRange: String,
    val totalVersesDescription: String
)

data class SurahInfo(
    val number: Int,
    val arabicName: String,
    val transliteration: String,
    val englishMeaning: String,
    val versesCount: Int,
    val revelationType: RevelationType,
    val startingPara: Int
)

data class QuranVerse(
    val surahNameArabic: String,
    val surahNameEnglish: String,
    val surahNumber: Int,
    val verseNumber: Int,
    val arabicText: String
)

data class ParaDetail(
    val info: ParaInfo,
    val surahSections: List<SurahSection>
)

data class SurahDetail(
    val info: SurahInfo,
    val bismillah: String? = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
    val verses: List<String>
)

data class SurahSection(
    val surahNameArabic: String,
    val surahNameEnglish: String,
    val surahNumber: Int,
    val fromAyah: Int = 1,
    val toAyah: Int = 1,
    val bismillah: String? = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
    val verses: List<String>
)

data class LastReadBookmark(
    val mode: ReadMode,
    val id: Int,
    val title: String,
    val arabicTitle: String,
    val subtitle: String,
    val surahNumber: Int = 1,
    val ayahNumber: Int = 1,
    val ayahText: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
