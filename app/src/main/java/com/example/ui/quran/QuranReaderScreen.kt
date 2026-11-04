package com.example.ui.quran

import android.app.Activity
import android.content.ContextWrapper
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.quran.ArabicFontStyle
import com.example.data.quran.ParaDetail
import com.example.data.quran.ReadMode
import com.example.data.quran.RevelationType
import com.example.data.quran.SurahDetail
import com.example.ui.components.AyahEndMedallionSymbol
import com.example.ui.components.ManuscriptOrnamentDivider
import com.example.ui.components.OctagonalNumberMedallion
import com.example.ui.components.PrimarySanctuaryButton
import com.example.ui.components.SecondaryEditorialButton
import com.example.ui.components.parseAyah
import com.example.ui.viewmodel.QuranViewModel
import com.example.ui.viewmodel.ReadingTheme
import kotlinx.coroutines.launch

/**
 * Precomputed, flattened reader list item to guarantee 120 FPS zero-lag scrolling
 * without any regex, string trimming, or offscreen Surface clipping during scroll frames.
 */
private sealed interface FlattenedReaderItem {
    val itemKey: String
    val contentType: String

    data class HeaderItem(
        override val itemKey: String,
        val surahNumber: Int,
        val surahNameEnglish: String,
        val surahNameArabic: String,
        val subtitle: String,
        val bismillah: String?
    ) : FlattenedReaderItem {
        override val contentType: String = "header"
    }

    data class VerseItem(
        override val itemKey: String,
        val bookmarkKey: String,
        val surahNumber: Int,
        val surahNameEnglish: String,
        val surahNameArabic: String,
        val ayahNumber: Int,
        val arabicText: String,
        val arabicDigits: String
    ) : FlattenedReaderItem {
        override val contentType: String = "verse"
    }
}

@Composable
fun QuranReaderScreen(
    quranViewModel: QuranViewModel,
    onBack: () -> Unit,
    onAyahRead: ((surahNum: Int, ayahNum: Int, paraNum: Int, surahName: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeMode by quranViewModel.activeReadMode.collectAsState()
    val selectedParaNumber by quranViewModel.selectedParaNumber.collectAsState()
    val paraDetail by quranViewModel.paraDetail.collectAsState()
    val selectedSurahNumber by quranViewModel.selectedSurahNumber.collectAsState()
    val surahDetail by quranViewModel.surahDetail.collectAsState()
    val allBookmarks by quranViewModel.allBookmarks.collectAsState()
    val lastReadBookmark by quranViewModel.lastReadBookmark.collectAsState()
    val isLoading by quranViewModel.isLoading.collectAsState()

    val fontSizeSp by quranViewModel.fontSizeSp.collectAsState()
    val readingTheme by quranViewModel.readingTheme.collectAsState()
    val arabicFontStyle by quranViewModel.arabicFontStyle.collectAsState()
    val keepScreenOn by quranViewModel.keepScreenOn.collectAsState()
    val targetScrollAyah by quranViewModel.targetScrollAyah.collectAsState()
    val targetScrollSurah by quranViewModel.targetScrollSurah.collectAsState()

    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var showControls by remember { mutableStateOf(true) }
    var showJumpDialog by remember { mutableStateOf(false) }

    // Screen Awake handler
    DisposableEffect(keepScreenOn) {
        var currentContext = context
        var activity: Activity? = null
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) {
                activity = currentContext
                break
            }
            currentContext = currentContext.baseContext
        }

        val window = activity?.window
        if (keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val isParaMode = activeMode == ReadMode.PARA

    // Precompute flattened items once per Para/Surah so LazyColumn does zero parsing on scroll
    val flattenedItems: List<FlattenedReaderItem> = remember(isParaMode, paraDetail, surahDetail) {
        if (isParaMode) {
            val detail = paraDetail ?: return@remember emptyList()
            val list = ArrayList<FlattenedReaderItem>(650)
            for (section in detail.surahSections) {
                list.add(
                    FlattenedReaderItem.HeaderItem(
                        itemKey = "para_${detail.info.number}_sec_${section.surahNumber}_hdr",
                        surahNumber = section.surahNumber,
                        surahNameEnglish = section.surahNameEnglish,
                        surahNameArabic = section.surahNameArabic,
                        subtitle = "Surah ${section.surahNumber} • Ayahs ${section.fromAyah}–${section.toAyah}",
                        bismillah = section.bismillah
                    )
                )
                section.verses.forEachIndexed { vIdx, verseStr ->
                    val parsed = parseAyah(verseStr, fallbackNumber = section.fromAyah + vIdx)
                    list.add(
                        FlattenedReaderItem.VerseItem(
                            itemKey = "para_${detail.info.number}_s_${section.surahNumber}_v_$vIdx",
                            bookmarkKey = "para_${detail.info.number}_s_${section.surahNumber}_a_${parsed.ayahNumber}",
                            surahNumber = section.surahNumber,
                            surahNameEnglish = section.surahNameEnglish,
                            surahNameArabic = section.surahNameArabic,
                            ayahNumber = parsed.ayahNumber,
                            arabicText = parsed.arabicText,
                            arabicDigits = parsed.arabicDigits
                        )
                    )
                }
            }
            list
        } else {
            val detail = surahDetail ?: return@remember emptyList()
            val list = ArrayList<FlattenedReaderItem>(detail.verses.size + 1)
            val revLabel = if (detail.info.revelationType == RevelationType.MAKKI) "Makki" else "Madani"
            list.add(
                FlattenedReaderItem.HeaderItem(
                    itemKey = "surah_header_${detail.info.number}",
                    surahNumber = detail.info.number,
                    surahNameEnglish = detail.info.transliteration,
                    surahNameArabic = detail.info.arabicName,
                    subtitle = "${detail.info.englishMeaning} • $revLabel • ${detail.info.versesCount} Verses • Para ${detail.info.startingPara}",
                    bismillah = detail.bismillah
                )
            )
            detail.verses.forEachIndexed { vIdx, verseStr ->
                val parsed = parseAyah(verseStr, fallbackNumber = vIdx + 1)
                list.add(
                    FlattenedReaderItem.VerseItem(
                        itemKey = "surah_${detail.info.number}_v_$vIdx",
                        bookmarkKey = "surah_${detail.info.number}_a_${parsed.ayahNumber}",
                        surahNumber = detail.info.number,
                        surahNameEnglish = detail.info.transliteration,
                        surahNameArabic = detail.info.arabicName,
                        ayahNumber = parsed.ayahNumber,
                        arabicText = parsed.arabicText,
                        arabicDigits = parsed.arabicDigits
                    )
                )
            }
            list
        }
    }

    // Auto-scroll directly to the exact bookmarked Ayah
    LaunchedEffect(targetScrollAyah, targetScrollSurah, flattenedItems) {
        val target = targetScrollAyah ?: return@LaunchedEffect
        if (flattenedItems.isEmpty()) return@LaunchedEffect

        val targetSurah = if (isParaMode) {
            targetScrollSurah ?: lastReadBookmark?.surahNumber ?: paraDetail?.surahSections?.firstOrNull()?.surahNumber ?: 1
        } else {
            surahDetail?.info?.number ?: 1
        }

        val targetIdx = flattenedItems.indexOfFirst { item ->
            item is FlattenedReaderItem.VerseItem &&
                (if (isParaMode) item.surahNumber == targetSurah && item.ayahNumber == target else item.ayahNumber == target)
        }

        if (targetIdx >= 0) {
            kotlinx.coroutines.delay(80)
            listState.scrollToItem(targetIdx)
        }
        quranViewModel.clearTargetScrollAyah()
    }

    // Automatic Ayah scroll counter with reader progress tracking
    LaunchedEffect(listState.firstVisibleItemIndex, flattenedItems) {
        val idx = listState.firstVisibleItemIndex
        val item = flattenedItems.getOrNull(idx)
        if (item is FlattenedReaderItem.VerseItem) {
            val pNum = if (isParaMode) (selectedParaNumber ?: 1) else (surahDetail?.info?.startingPara ?: 1)
            onAyahRead?.invoke(item.surahNumber, item.ayahNumber, pNum, item.surahNameEnglish)
        }
    }


    // Peaceful, high-legibility palette for each reading environment
    val (backgroundColor, textColor, headerBgColor, markerColor) = when (readingTheme) {
        ReadingTheme.DEFAULT -> Quadruple(
            Color(0xFFFBF8F1), // Warm natural Mushaf ivory
            Color(0xFF19201D), // Deep charcoal ink
            Color(0xFFF1EAE0), // Subtle header surface
            Color(0xFF0D4A38)  // Mamluk emerald
        )
        ReadingTheme.SEPIA -> Quadruple(
            Color(0xFFF3EAD8), // Aged manuscript parchment
            Color(0xFF332516), // Warm walnut ink
            Color(0xFFE6D8C0), // Warm sand header
            Color(0xFF8A5E24)  // Burnished bronze marker
        )
        ReadingTheme.DARK -> Quadruple(
            Color(0xFF12151E), // Midnight slate background
            Color(0xFFF3F0E8), // Warm ivory text
            Color(0xFF1A1E2B), // Elevated slate header
            Color(0xFFE5C178)  // Warm champagne brass marker
        )
    }

    val currentChapterBookmarks = remember(allBookmarks, isParaMode, selectedParaNumber, selectedSurahNumber) {
        if (isParaMode && selectedParaNumber != null) {
            allBookmarks.filter { it.mode == ReadMode.PARA && it.id == selectedParaNumber }
                .sortedWith(compareBy({ it.surahNumber }, { it.ayahNumber }))
        } else if (!isParaMode && selectedSurahNumber != null) {
            allBookmarks.filter { it.mode == ReadMode.SURAH && it.id == selectedSurahNumber }
                .sortedBy { it.ayahNumber }
        } else {
            emptyList()
        }
    }

    val bookmarkedAyahKeys = remember(allBookmarks) {
        val set = HashSet<String>(allBookmarks.size * 2)
        for (b in allBookmarks) {
            if (b.mode == ReadMode.PARA) {
                set.add("para_${b.id}_s_${b.surahNumber}_a_${b.ayahNumber}")
            } else {
                set.add("surah_${b.id}_a_${b.ayahNumber}")
            }
        }
        set
    }

    val onToggleControls = remember { { showControls = !showControls } }
    var transportBookmarkIndex by remember(currentChapterBookmarks.size) { mutableIntStateOf(-1) }

    // Quick-Jump to Ayah Dialog
    if (showJumpDialog) {
        var jumpInput by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showJumpDialog = false }) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    Text(
                        text = "QUICK JUMP TO VERSE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            fontSize = 10.sp
                        ),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Jump to Ayah Number",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = jumpInput,
                        onValueChange = { jumpInput = it.filter { ch -> ch.isDigit() } },
                        placeholder = { Text("Enter Ayah number (e.g. 1, 255)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SecondaryEditorialButton(
                            text = "Cancel",
                            onClick = { showJumpDialog = false },
                            modifier = Modifier.weight(1f)
                        )
                        PrimarySanctuaryButton(
                            text = "Jump",
                            onClick = {
                                val targetNum = jumpInput.toIntOrNull()
                                if (targetNum != null) {
                                    val idx = flattenedItems.indexOfFirst {
                                        it is FlattenedReaderItem.VerseItem && it.ayahNumber == targetNum
                                    }
                                    if (idx >= 0) {
                                        coroutineScope.launch {
                                            listState.scrollToItem(idx)
                                        }
                                        showJumpDialog = false
                                    } else {
                                        Toast.makeText(context, "Ayah $targetNum not found in this view", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it }
            ) {
                Surface(
                    color = headerBgColor.copy(alpha = 0.97f),
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = textColor
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    if (isParaMode && paraDetail != null) {
                                        val detail = paraDetail!!
                                        Text(
                                            text = "Para ${detail.info.number} · ${detail.info.transliteration}",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = (-0.1).sp
                                            ),
                                            color = textColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = detail.info.surahRange,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = textColor.copy(alpha = 0.7f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else if (!isParaMode && surahDetail != null) {
                                        val detail = surahDetail!!
                                        Text(
                                            text = "Surah ${detail.info.number} · ${detail.info.transliteration}",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = (-0.1).sp
                                            ),
                                            color = textColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${detail.info.englishMeaning} · ${if (detail.info.revelationType == RevelationType.MAKKI) "Makki" else "Madani"} · ${detail.info.versesCount} Verses",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = textColor.copy(alpha = 0.7f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else {
                                        Text(
                                            text = "The Noble Qur'an",
                                            color = textColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Quick Jump to Ayah button
                                IconButton(
                                    onClick = { showJumpDialog = true },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FormatListNumbered,
                                        contentDescription = "Jump to Ayah",
                                        tint = textColor.copy(alpha = 0.8f)
                                    )
                                }

                                // Sequential Multi-Bookmark Transport button
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (currentChapterBookmarks.isEmpty()) {
                                            Toast.makeText(
                                                context,
                                                "No bookmarks in this ${if (isParaMode) "Para" else "Surah"} yet.\nTap the bookmark icon on any Ayah to save one.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            val nextIndex = (transportBookmarkIndex + 1) % currentChapterBookmarks.size
                                            transportBookmarkIndex = nextIndex
                                            val bm = currentChapterBookmarks[nextIndex]
                                            val targetIdx = flattenedItems.indexOfFirst { item ->
                                                item is FlattenedReaderItem.VerseItem &&
                                                    (if (isParaMode) item.surahNumber == bm.surahNumber && item.ayahNumber == bm.ayahNumber else item.ayahNumber == bm.ayahNumber)
                                            }
                                            coroutineScope.launch {
                                                if (targetIdx >= 0) {
                                                    listState.animateScrollToItem(targetIdx)
                                                }
                                            }
                                            val msg = if (currentChapterBookmarks.size == 1) {
                                                "Transported to Ayah ${bm.ayahNumber}"
                                            } else {
                                                "Transported to Bookmark ${nextIndex + 1} of ${currentChapterBookmarks.size} (Ayah ${bm.ayahNumber})"
                                            }
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    if (currentChapterBookmarks.size > 1) {
                                        BadgedBox(
                                            badge = {
                                                Badge(
                                                    containerColor = markerColor,
                                                    contentColor = backgroundColor
                                                ) {
                                                    Text(
                                                        text = "${currentChapterBookmarks.size}",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 10.sp
                                                        )
                                                    )
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Bookmark,
                                                contentDescription = "Transport through bookmarks (${currentChapterBookmarks.size})",
                                                tint = markerColor
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (currentChapterBookmarks.isNotEmpty()) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = if (currentChapterBookmarks.isNotEmpty()) "Transport to bookmark" else "No bookmarks in this chapter",
                                            tint = if (currentChapterBookmarks.isNotEmpty()) markerColor else textColor.copy(alpha = 0.65f)
                                        )
                                    }
                                }

                                // Keep Screen On Toggle
                                IconButton(
                                    onClick = { quranViewModel.toggleKeepScreenOn() },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = if (keepScreenOn) Icons.Default.WbSunny else Icons.Default.LockOpen,
                                        contentDescription = if (keepScreenOn) "Screen stay awake: ON" else "Screen stay awake: OFF",
                                        tint = if (keepScreenOn) markerColor else textColor.copy(alpha = 0.55f)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(thickness = 0.8.dp, color = markerColor.copy(alpha = 0.25f))
                    }
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                Surface(
                    color = headerBgColor,
                    shadowElevation = 12.dp,
                    border = BorderStroke(0.8.dp, markerColor.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        // Top row: Script filter chips & Theme selector dots
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Script",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = textColor.copy(alpha = 0.7f)
                                )
                                FilterChip(
                                    selected = arabicFontStyle == ArabicFontStyle.UTHMANI,
                                    onClick = { quranViewModel.setArabicFontStyle(ArabicFontStyle.UTHMANI) },
                                    label = { Text("Uthmani", style = MaterialTheme.typography.labelSmall) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = markerColor.copy(alpha = 0.18f),
                                        selectedLabelColor = markerColor
                                    )
                                )
                                FilterChip(
                                    selected = arabicFontStyle == ArabicFontStyle.INDO_PAK,
                                    onClick = { quranViewModel.setArabicFontStyle(ArabicFontStyle.INDO_PAK) },
                                    label = { Text("Indo-Pak", style = MaterialTheme.typography.labelSmall) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = markerColor.copy(alpha = 0.18f),
                                        selectedLabelColor = markerColor
                                    )
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ThemeDot(
                                    color = Color(0xFFFBF8F1),
                                    borderColor = Color(0xFF0D4A38),
                                    isSelected = readingTheme == ReadingTheme.DEFAULT,
                                    onClick = { quranViewModel.setReadingTheme(ReadingTheme.DEFAULT) }
                                )
                                ThemeDot(
                                    color = Color(0xFFF3EAD8),
                                    borderColor = Color(0xFF8A5E24),
                                    isSelected = readingTheme == ReadingTheme.SEPIA,
                                    onClick = { quranViewModel.setReadingTheme(ReadingTheme.SEPIA) }
                                )
                                ThemeDot(
                                    color = Color(0xFF0C1311),
                                    borderColor = Color(0xFFE5B966),
                                    isSelected = readingTheme == ReadingTheme.DARK,
                                    onClick = { quranViewModel.setReadingTheme(ReadingTheme.DARK) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Bottom row: Previous / Font Size Controller / Next
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isParaMode) {
                                val currentNum = selectedParaNumber ?: 1
                                IconButton(
                                    onClick = {
                                        if (currentNum > 1) quranViewModel.selectPara(currentNum - 1)
                                    },
                                    enabled = currentNum > 1,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Previous Para",
                                        tint = if (currentNum > 1) textColor else textColor.copy(alpha = 0.25f)
                                    )
                                }
                            } else {
                                val currentNum = selectedSurahNumber ?: 1
                                IconButton(
                                    onClick = {
                                        if (currentNum > 1) quranViewModel.selectSurah(currentNum - 1)
                                    },
                                    enabled = currentNum > 1,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Previous Surah",
                                        tint = if (currentNum > 1) textColor else textColor.copy(alpha = 0.25f)
                                    )
                                }
                            }

                            // Font size stepper
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { quranViewModel.adjustFontSize(-2) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Decrease font size",
                                        tint = textColor
                                    )
                                }

                                Text(
                                    text = "$fontSizeSp pt",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = textColor
                                )

                                IconButton(
                                    onClick = { quranViewModel.adjustFontSize(2) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Increase font size",
                                        tint = textColor
                                    )
                                }
                            }

                            if (isParaMode) {
                                val currentNum = selectedParaNumber ?: 1
                                IconButton(
                                    onClick = {
                                        if (currentNum < 30) quranViewModel.selectPara(currentNum + 1)
                                    },
                                    enabled = currentNum < 30,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Next Para",
                                        tint = if (currentNum < 30) textColor else textColor.copy(alpha = 0.25f)
                                    )
                                }
                            } else {
                                val currentNum = selectedSurahNumber ?: 1
                                IconButton(
                                    onClick = {
                                        if (currentNum < 114) quranViewModel.selectSurah(currentNum + 1)
                                    },
                                    enabled = currentNum < 114,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Next Surah",
                                        tint = if (currentNum < 114) textColor else textColor.copy(alpha = 0.25f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = backgroundColor,
        modifier = modifier
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = markerColor,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(40.dp)
                    )
                }
            } else if (flattenedItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isParaMode) "Select a Para to read." else "Select a Surah to read.",
                        color = textColor
                    )
                }
            } else {
                val activeChapterId = if (isParaMode) (selectedParaNumber ?: 1) else (selectedSurahNumber ?: 1)

                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        top = 96.dp,
                        bottom = 120.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = flattenedItems,
                        key = { _, item -> item.itemKey },
                        contentType = { _, item -> item.contentType }
                    ) { _, item ->
                        when (item) {
                            is FlattenedReaderItem.HeaderItem -> {
                                ChapterHeaderBanner(
                                    surahNumber = item.surahNumber,
                                    surahNameEnglish = item.surahNameEnglish,
                                    surahNameArabic = item.surahNameArabic,
                                    subtitle = item.subtitle,
                                    bismillah = item.bismillah,
                                    fontSizeSp = fontSizeSp,
                                    arabicFontStyle = arabicFontStyle,
                                    markerColor = markerColor,
                                    headerBgColor = headerBgColor,
                                    textColor = textColor
                                )
                            }
                            is FlattenedReaderItem.VerseItem -> {
                                val isBookmarked = item.bookmarkKey in bookmarkedAyahKeys
                                AyahReaderItem(
                                    surahNumber = item.surahNumber,
                                    ayahNumber = item.ayahNumber,
                                    arabicText = item.arabicText,
                                    arabicDigits = item.arabicDigits,
                                    fontSizeSp = fontSizeSp,
                                    fontStyle = arabicFontStyle,
                                    textColor = textColor,
                                    markerColor = markerColor,
                                    isBookmarked = isBookmarked,
                                    onToggleBookmark = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (isBookmarked) {
                                            quranViewModel.removeBookmark(
                                                mode = activeMode,
                                                id = activeChapterId,
                                                surahNumber = item.surahNumber,
                                                ayahNumber = item.ayahNumber
                                            )
                                            Toast.makeText(
                                                context,
                                                "Bookmark removed for Ayah ${item.ayahNumber}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            quranViewModel.bookmarkAyah(
                                                mode = activeMode,
                                                id = activeChapterId,
                                                surahNumber = item.surahNumber,
                                                surahNameEn = item.surahNameEnglish,
                                                surahNameAr = item.surahNameArabic,
                                                ayahNumber = item.ayahNumber,
                                                ayahText = item.arabicText
                                            )
                                            Toast.makeText(
                                                context,
                                                "Ayah ${item.ayahNumber} Bookmarked",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    onToggleControls = onToggleControls
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Lightweight, zero-offscreen-layer Ayah row composable using [drawBehind] for 120 FPS scrolling.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AyahReaderItem(
    surahNumber: Int,
    ayahNumber: Int,
    arabicText: String,
    arabicDigits: String,
    fontSizeSp: Int,
    fontStyle: ArabicFontStyle,
    textColor: Color,
    markerColor: Color,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onToggleControls: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lineHeightVal = if (fontStyle == ArabicFontStyle.INDO_PAK) (fontSizeSp * 2.05).sp else (fontSizeSp * 1.9).sp
    val letterSpacingVal = if (fontStyle == ArabicFontStyle.INDO_PAK) 0.4.sp else 0.sp

    val annotatedText: AnnotatedString = remember(arabicText, arabicDigits) {
        buildAnnotatedString {
            append(arabicText)
            append(" ")
            appendInlineContent(id = "ayah_end", alternateText = arabicDigits)
        }
    }

    val inlineContentMap = remember(markerColor, fontSizeSp) {
        val markerSizeSp = (fontSizeSp * 1.34f).sp
        mapOf(
            "ayah_end" to InlineTextContent(
                placeholder = Placeholder(
                    width = markerSizeSp,
                    height = markerSizeSp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                )
            ) { digits ->
                AyahEndMedallionSymbol(
                    arabicDigits = digits,
                    markerColor = markerColor,
                    fontSizeSp = fontSizeSp
                )
            }
        )
    }

    val highlightFill = markerColor.copy(alpha = 0.11f)
    val highlightBorder = markerColor.copy(alpha = 0.55f)
    val dividerHairline = markerColor.copy(alpha = 0.14f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                if (isBookmarked) {
                    val corner = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                    drawRoundRect(color = highlightFill, cornerRadius = corner)
                    drawRoundRect(
                        color = highlightBorder,
                        cornerRadius = corner,
                        style = Stroke(width = 1.1.dp.toPx())
                    )
                } else {
                    drawLine(
                        color = dividerHairline,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 0.7.dp.toPx()
                    )
                }
            }
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "Ayah $ayahNumber${if (isBookmarked) ", bookmarked" else ""}"
            }
            .combinedClickable(
                onLongClick = onToggleBookmark,
                onClick = onToggleControls
            )
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Left Medallion & Quick-Bookmark Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(top = 4.dp, end = 10.dp)
                .width(48.dp)
        ) {
            Text(
                text = "$surahNumber:$ayahNumber",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                ),
                color = if (isBookmarked) markerColor else textColor.copy(alpha = 0.55f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggleBookmark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark Ayah $ayahNumber",
                    tint = if (isBookmarked) markerColor else textColor.copy(alpha = 0.45f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Right Quranic Verse Text Column
        Text(
            text = annotatedText,
            inlineContent = inlineContentMap,
            style = TextStyle(
                fontSize = fontSizeSp.sp,
                lineHeight = lineHeightVal,
                letterSpacing = letterSpacingVal,
                fontFamily = if (fontStyle == ArabicFontStyle.UTHMANI) FontFamily.Serif else FontFamily.Default,
                fontWeight = if (fontStyle == ArabicFontStyle.INDO_PAK) FontWeight.Medium else FontWeight.Normal,
                textAlign = TextAlign.Right,
                textDirection = TextDirection.Rtl
            ),
            color = textColor,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Illuminated Mushaf Chapter Header Banner with geometric medallion, dual ornamental borders,
 * and framed Bismillah plaque.
 */
@Composable
private fun ChapterHeaderBanner(
    surahNumber: Int,
    surahNameEnglish: String,
    surahNameArabic: String,
    subtitle: String,
    bismillah: String?,
    fontSizeSp: Int,
    arabicFontStyle: ArabicFontStyle,
    markerColor: Color,
    headerBgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = headerBgColor,
            border = BorderStroke(1.2.dp, markerColor.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp)
                ) {
                    OctagonalNumberMedallion(
                        number = surahNumber,
                        size = 40.dp,
                        primaryColor = markerColor,
                        secondaryColor = markerColor,
                        containerColor = markerColor.copy(alpha = 0.14f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = surahNameEnglish,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = textColor.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    text = surahNameArabic,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 25.sp,
                        fontFamily = FontFamily.Serif
                    ),
                    color = markerColor
                )
            }
        }

        if (!bismillah.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = headerBgColor.copy(alpha = 0.55f),
                border = BorderStroke(0.8.dp, markerColor.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = bismillah,
                    style = TextStyle(
                        fontSize = (fontSizeSp + 2).sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = if (arabicFontStyle == ArabicFontStyle.UTHMANI) FontFamily.Serif else FontFamily.Default,
                        textAlign = TextAlign.Center,
                        textDirection = TextDirection.Rtl
                    ),
                    color = textColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        ManuscriptOrnamentDivider(color = markerColor.copy(alpha = 0.45f))
    }
}

@Composable
private fun ThemeDot(
    color: Color,
    borderColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 2.2.dp else 0.9.dp,
                color = if (isSelected) borderColor else Color.Gray.copy(alpha = 0.4f),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
