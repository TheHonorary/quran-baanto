package com.example.util

import java.time.LocalDate
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.format.TextStyle
import java.time.temporal.ChronoField
import java.util.Locale

data class MonthDateInfo(
    val gregorianYear: Int,
    val gregorianMonth: Int,
    val gregorianMonthName: String,
    val gregorianFullText: String,
    val hijriYear: Int,
    val hijriMonth: Int,
    val hijriMonthName: String,
    val hijriMonthArabic: String,
    val hijriFullText: String,
    val dualHeaderDisplay: String
)

object HijriDateUtils {

    private val HIJRI_MONTH_NAMES_EN = listOf(
        "Muharram",
        "Safar",
        "Rabi' al-Awwal",
        "Rabi' al-Thani",
        "Jumada al-Ula",
        "Jumada al-Akhirah",
        "Rajab",
        "Sha'ban",
        "Ramadan",
        "Shawwal",
        "Dhu al-Qi'dah",
        "Dhu al-Hijjah"
    )

    private val HIJRI_MONTH_NAMES_AR = listOf(
        "المحرّم",
        "صفر",
        "ربيع الأول",
        "ربيع الثاني",
        "جمادى الأولى",
        "جمادى الآخرة",
        "رجب",
        "شعبان",
        "رمضان",
        "شوال",
        "ذو القعدة",
        "ذو الحجة"
    )

    /**
     * Obtains comprehensive date information for a given Gregorian year and month,
     * including accurate real-time Hijri calendar conversion.
     */
    fun getMonthInfo(year: Int, month: Int): MonthDateInfo {
        val safeMonth = month.coerceIn(1, 12)
        val sampleGregorianDate = LocalDate.of(year, safeMonth, 15)
        val gregorianMonthName = sampleGregorianDate.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        val gregorianFullText = "$gregorianMonthName $year"

        val (hijriYear, hijriMonth) = try {
            val hijrahDate: HijrahDate = HijrahChronology.INSTANCE.date(sampleGregorianDate)
            val hYear = hijrahDate.get(ChronoField.YEAR)
            val hMonth = hijrahDate.get(ChronoField.MONTH_OF_YEAR)
            Pair(hYear, hMonth)
        } catch (e: Exception) {
            // Fallback estimation if Hijrah Chronology unavailable
            val estimatedHijriYear = ((year - 622) * 1.030684).toInt()
            Pair(estimatedHijriYear, safeMonth)
        }

        val safeHijriMonthIdx = (hijriMonth - 1).coerceIn(0, 11)
        val hijriMonthName = HIJRI_MONTH_NAMES_EN.getOrElse(safeHijriMonthIdx) { "Month $hijriMonth" }
        val hijriMonthArabic = HIJRI_MONTH_NAMES_AR.getOrElse(safeHijriMonthIdx) { "" }
        val hijriFullText = "$hijriMonthName $hijriYear AH"

        val dualHeaderDisplay = "$gregorianFullText • $hijriFullText"

        return MonthDateInfo(
            gregorianYear = year,
            gregorianMonth = safeMonth,
            gregorianMonthName = gregorianMonthName,
            gregorianFullText = gregorianFullText,
            hijriYear = hijriYear,
            hijriMonth = hijriMonth,
            hijriMonthName = hijriMonthName,
            hijriMonthArabic = hijriMonthArabic,
            hijriFullText = hijriFullText,
            dualHeaderDisplay = dualHeaderDisplay
        )
    }
}
