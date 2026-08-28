package com.example.data.quran

import android.content.Context
import org.json.JSONArray
import java.util.concurrent.ConcurrentHashMap

object QuranRepository {

    val ALL_PARAS: List<ParaInfo> = listOf(
        ParaInfo(1, "آلم", "Alif Lam Meem", "Al-Fatihah 1:1 – Al-Baqarah 2:141", "148 Verses"),
        ParaInfo(2, "سَيَقُولُ", "Sayaqool", "Al-Baqarah 2:142 – Al-Baqarah 2:252", "111 Verses"),
        ParaInfo(3, "تِلْكَ الرُّسُلُ", "Tilka-r-Rusul", "Al-Baqarah 2:253 – Ali 'Imran 3:92", "126 Verses"),
        ParaInfo(4, "لَنْ تَنَالُوا", "Lan Tanalu", "Ali 'Imran 3:93 – An-Nisa 4:23", "131 Verses"),
        ParaInfo(5, "وَالْمُحْصَنَاتُ", "Wal-Muhsanat", "An-Nisa 4:24 – An-Nisa 4:147", "124 Verses"),
        ParaInfo(6, "لَا يُحِبُّ اللَّهُ", "La Yuhibbu-llah", "An-Nisa 4:148 – Al-Ma'idah 5:81", "110 Verses"),
        ParaInfo(7, "وَإِذَا سَمِعُوا", "Wa Iza Sami'u", "Al-Ma'idah 5:82 – Al-An'am 6:110", "149 Verses"),
        ParaInfo(8, "وَلَوْ أَنَّنَا", "Wa Lau Annana", "Al-An'am 6:111 – Al-A'raf 7:87", "142 Verses"),
        ParaInfo(9, "قَالَ الْمَلَأُ", "Qalal-Mala'u", "Al-A'raf 7:88 – Al-Anfal 8:40", "159 Verses"),
        ParaInfo(10, "وَاعْلَمُوا", "Wa'lamu", "Al-Anfal 8:41 – At-Tawbah 9:92", "127 Verses"),
        ParaInfo(11, "يَعْتَذِرُونَ", "Ya'tazirun", "At-Tawbah 9:93 – Hud 11:5", "151 Verses"),
        ParaInfo(12, "وَمَا مِنْ دَابَّةٍ", "Wa Ma Min Dabbah", "Hud 11:6 – Yusuf 12:52", "170 Verses"),
        ParaInfo(13, "وَمَا أُبَرِّئُ", "Wa Ma Ubarri'u", "Yusuf 12:53 – Ibrahim 14:52", "154 Verses"),
        ParaInfo(14, "رُبَمَا", "Rubama", "Al-Hijr 15:1 – An-Nahl 16:128", "227 Verses"),
        ParaInfo(15, "سُبْحَانَ الَّذِي", "Subhana-llazi", "Al-Isra 17:1 – Al-Kahf 18:74", "185 Verses"),
        ParaInfo(16, "قَالَ أَلَمْ", "Qala Alam", "Al-Kahf 18:75 – Ta-Ha 20:135", "269 Verses"),
        ParaInfo(17, "اقْتَرَبَ لِلنَّاسِ", "Iqtaraba Lin-Nasi", "Al-Anbiya 21:1 – Al-Hajj 22:78", "190 Verses"),
        ParaInfo(18, "قَدْ أَفْلَحَ", "Qad Aflaha", "Al-Mu'minun 23:1 – Al-Furqan 25:20", "202 Verses"),
        ParaInfo(19, "وَقَالَ الَّذِينَ", "Wa Qalal-Ladhina", "Al-Furqan 25:21 – An-Naml 27:55", "339 Verses"),
        ParaInfo(20, "أَمَّنْ خَلَقَ", "Amman Khalaqa", "An-Naml 27:56 – Al-'Ankabut 29:45", "171 Verses"),
        ParaInfo(21, "اتْلُ مَا أُوحِيَ", "Utlu Ma Oohiya", "Al-'Ankabut 29:46 – Al-Ahzab 33:30", "178 Verses"),
        ParaInfo(22, "وَمَنْ يَقْنُتْ", "Wa Man Yaqnut", "Al-Ahzab 33:31 – Ya-Sin 36:27", "169 Verses"),
        ParaInfo(23, "وَمَا لِيَ", "Wa Maliya", "Ya-Sin 36:28 – Az-Zumar 39:31", "357 Verses"),
        ParaInfo(24, "فَمَنْ أَظْلَمُ", "Faman Azlamu", "Az-Zumar 39:32 – Fussilat 41:46", "175 Verses"),
        ParaInfo(25, "إِلَيْهِ يُرَدُّ", "Ilayhi Yuraddu", "Fussilat 41:47 – Al-Jathiyah 45:37", "246 Verses"),
        ParaInfo(26, "حم", "Ha-Meem", "Al-Ahqaf 46:1 – Adh-Dhariyat 51:30", "195 Verses"),
        ParaInfo(27, "قَالَ فَمَا خَطْبُكُمْ", "Qala Fama Khatbukum", "Adh-Dhariyat 51:31 – Al-Hadid 57:29", "399 Verses"),
        ParaInfo(28, "قَدْ سَمِعَ اللَّهُ", "Qad Sami'a-llahu", "Al-Mujadila 58:1 – At-Tahrim 66:12", "137 Verses"),
        ParaInfo(29, "تَبَارَكَ الَّذِي", "Tabaraka-llazi", "Al-Mulk 67:1 – Al-Mursalat 77:50", "431 Verses"),
        ParaInfo(30, "عَمَّ يَتَسَاءَلُونَ", "Amma Yatasa'alun", "An-Naba 78:1 – An-Nas 114:6", "564 Verses")
    )

    val ALL_SURAHS: List<SurahInfo> = SurahCatalog.ALL_SURAHS

    private val parasCache = ConcurrentHashMap<Int, ParaDetail>()
    private val surahsCache = ConcurrentHashMap<Int, SurahDetail>()

    @Volatile
    private var isInitialized = false

    fun initialize(context: Context) {
        if (!isInitialized) {
            synchronized(this) {
                if (!isInitialized) {
                    loadParasFromAssets(context)
                    loadSurahsFromAssets(context)
                    isInitialized = true
                }
            }
        }
    }

    fun getParaInfo(number: Int): ParaInfo {
        return ALL_PARAS.firstOrNull { it.number == number } ?: ALL_PARAS[0]
    }

    fun getSurahInfo(number: Int): SurahInfo {
        return SurahCatalog.getSurahInfo(number)
    }

    fun getParaDetail(context: Context, number: Int): ParaDetail {
        initialize(context)
        return parasCache[number] ?: fallbackParaDetail(number)
    }

    fun getSurahDetail(context: Context, number: Int): SurahDetail {
        initialize(context)
        return surahsCache[number] ?: fallbackSurahDetail(number)
    }

    private fun loadParasFromAssets(context: Context) {
        try {
            val jsonString = context.assets.open("quran/paras.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                val juzObj = jsonArray.getJSONObject(i)
                val juzNum = juzObj.getInt("juz")
                val sectionsArray = juzObj.getJSONArray("sections")
                val sectionsList = ArrayList<SurahSection>(sectionsArray.length())

                for (j in 0 until sectionsArray.length()) {
                    val secObj = sectionsArray.getJSONObject(j)
                    val sId = secObj.getInt("surah_id")
                    val sNameAr = secObj.getString("surah_name_ar")
                    val sNameEn = secObj.getString("surah_name_en")
                    val fromAyah = secObj.optInt("from_ayah", 1)
                    val toAyah = secObj.optInt("to_ayah", 1)
                    val bismillah = if (secObj.isNull("bismillah")) null else secObj.getString("bismillah")

                    val versesArray = secObj.getJSONArray("verses")
                    val versesList = ArrayList<String>(versesArray.length())
                    for (k in 0 until versesArray.length()) {
                        versesList.add(versesArray.getString(k))
                    }

                    sectionsList.add(
                        SurahSection(
                            surahNumber = sId,
                            surahNameArabic = sNameAr,
                            surahNameEnglish = sNameEn,
                            fromAyah = fromAyah,
                            toAyah = toAyah,
                            bismillah = bismillah,
                            verses = versesList
                        )
                    )
                }

                val info = getParaInfo(juzNum)
                parasCache[juzNum] = ParaDetail(info = info, surahSections = sectionsList)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadSurahsFromAssets(context: Context) {
        try {
            val jsonString = context.assets.open("quran/surahs.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                val surahObj = jsonArray.getJSONObject(i)
                val surahId = surahObj.getInt("id")
                val versesArray = surahObj.getJSONArray("verses")
                val versesList = ArrayList<String>(versesArray.length())

                for (k in 0 until versesArray.length()) {
                    val verseText = versesArray.getString(k)
                    versesList.add("$verseText ۝${k + 1}")
                }

                val info = getSurahInfo(surahId)
                val bismillah = if (surahId == 9 || surahId == 1) null else "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
                surahsCache[surahId] = SurahDetail(info = info, bismillah = bismillah, verses = versesList)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun fallbackParaDetail(paraNumber: Int): ParaDetail {
        val info = getParaInfo(paraNumber)
        return ParaDetail(
            info = info,
            surahSections = listOf(
                SurahSection(
                    surahNumber = 1,
                    surahNameArabic = "الفَاتِحَة",
                    surahNameEnglish = "Al-Fatihah",
                    fromAyah = 1,
                    toAyah = 7,
                    bismillah = null,
                    verses = listOf(
                        "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ ۝1",
                        "ٱلۡحَمۡدُ لِلَّهِ رَبِّ ٱلۡعَٰلَمِينَ ۝2",
                        "ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ ۝3",
                        "مَٰلِكِ يَوۡمِ ٱلدِّينِ ۝4",
                        "إِيَّاكَ نَعۡبُدُ وَإِيَّاكَ نَسۡتَعِينُ ۝5",
                        "ٱهۡدِنَا ٱلصِّرَٰطَ ٱلۡمُسۡتَقِيمَ ۝6",
                        "صِرَٰطَ ٱلَّذِينَ أَنۡعَمۡتَ عَلَيۡهِمۡ غَيۡرِ ٱلۡمَغۡضُوبِ عَلَيۡهِمۡ وَلَا ٱلضَّآلِّينَ ۝7"
                    )
                )
            )
        )
    }

    private fun fallbackSurahDetail(surahNumber: Int): SurahDetail {
        val info = getSurahInfo(surahNumber)
        return SurahDetail(
            info = info,
            bismillah = if (surahNumber == 9) null else "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
            verses = listOf(
                "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ ۝1",
                "ٱلۡحَمۡدُ لِلَّهِ رَبِّ ٱلۡعَٰلَمِينَ ۝2",
                "ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ ۝3",
                "مَٰلِكِ يَوۡمِ ٱلدِّينِ ۝4",
                "إِيَّاكَ نَعۡبُدُ وَإِيَّاكَ نَسۡتَعِينُ ۝5",
                "ٱهۡدِنَا ٱلصِّرَٰطَ ٱلۡمُسۡتَقِيمَ ۝6",
                "صِرَٰطَ ٱلَّذِينَ أَنۡعَمۡتَ عَلَيۡهِمۡ غَيۡرِ ٱلۡمَغۡضُوبِ عَلَيۡهِمۡ وَلَا ٱلضَّآلِّينَ ۝7"
            )
        )
    }
}
