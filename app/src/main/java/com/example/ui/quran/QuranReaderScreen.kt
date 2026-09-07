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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import com.example.ui.components.toArabicIndicDigits
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.quran.ArabicFontStyle
import com.example.data.quran.ReadMode
import com.example.data.quran.RevelationType
import com.example.data.quran.SurahDetail
import com.example.data.quran.SurahSection
import com.example.ui.components.AyahEndMarker
import com.example.ui.components.parseAyah
import com.example.ui.viewmodel.QuranViewModel
import com.example.ui.viewmodel.ReadingTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranReaderScreen(
    quranViewModel: QuranViewModel,
    onBack: () -> Unit,
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

    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Immersive toggle: tap canvas to show/hide controls
    var showControls by remember { mutableStateOf(true) }

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

    // Auto-scroll directly to the exact bookmarked Ayah
    LaunchedEffect(targetScrollAyah, paraDetail, surahDetail) {
        val target = targetScrollAyah ?: return@LaunchedEffect
        val targetIndex = if (isParaMode && paraDetail != null) {
            val targetSurah = lastReadBookmark?.surahNumber ?: paraDetail!!.surahSections.firstOrNull()?.surahNumber ?: 1
            findAyahItemIndexInPara(paraDetail!!, targetSurah, target)
        } else if (!isParaMode && surahDetail != null) {
            findAyahItemIndexInSurah(surahDetail!!, target)
        } else {
            0
        }

        if (targetIndex > 0) {
            listState.scrollToItem(targetIndex)
        }
        quranViewModel.clearTargetScrollAyah()
    }

    // Peaceful, high-legibility palette for each reading environment
    val (backgroundColor, textColor, headerBgColor, markerColor) = when (readingTheme) {
        ReadingTheme.DEFAULT -> Quadruple(
            Color(0xFFFAF7F2), // Warm natural ivory paper
            Color(0xFF1E2321), // Deep charcoal ink
            Color(0xFFF1EBE1), // Subtle header tint
            Color(0xFF2D5C4C)  // Subtle botanical emerald
        )
        ReadingTheme.SEPIA -> Quadruple(
            Color(0xFFF4ECDC), // Aged parchment
            Color(0xFF382A1B), // Warm walnut ink
            Color(0xFFE8DCC6), // Warm sand header
            Color(0xFF7A562D)  // Warm bronze marker
        )
        ReadingTheme.DARK -> Quadruple(
            Color(0xFF111315), // Velvety deep night obsidian
            Color(0xFFEDEFEF), // Warm soft ivory text
            Color(0xFF191C20), // Subtle elevated card header
            Color(0xFFE5C378)  // Luminous noble antique gold marker
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

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it }
            ) {
                TopAppBar(
                    title = {
                        Column {
                            if (isParaMode && paraDetail != null) {
                                val detail = paraDetail!!
                                Text(
                                    text = "Para ${detail.info.number} · ${detail.info.transliteration}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = (-0.1).sp
                                    ),
                                    color = textColor
                                )
                                Text(
                                    text = detail.info.surahRange,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textColor.copy(alpha = 0.65f)
                                )
                            } else if (!isParaMode && surahDetail != null) {
                                val detail = surahDetail!!
                                Text(
                                    text = "Surah ${detail.info.number} · ${detail.info.transliteration}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = (-0.1).sp
                                    ),
                                    color = textColor
                                )
                                Text(
                                    text = "${detail.info.englishMeaning} · ${if (detail.info.revelationType == RevelationType.MAKKI) "Makki" else "Madani"} · ${detail.info.versesCount} Verses",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textColor.copy(alpha = 0.65f)
                                )
                            } else {
                                Text(text = "The Holy Qur'an", color = textColor)
                            }
                        }
                    },
                    navigationIcon = {
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
                    },
                    actions = {
                        // Transport to bookmark button
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (currentChapterBookmarks.isEmpty()) {
                                    Toast.makeText(
                                        context,
                                        "No bookmarks in this ${if (isParaMode) "Para" else "Surah"} yet.\nLong-press any Ayah to bookmark it.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else if (currentChapterBookmarks.size == 1) {
                                    val bm = currentChapterBookmarks[0]
                                    val targetIndex = if (isParaMode && paraDetail != null) {
                                        findAyahItemIndexInPara(paraDetail!!, bm.surahNumber, bm.ayahNumber)
                                    } else if (!isParaMode && surahDetail != null) {
                                        findAyahItemIndexInSurah(surahDetail!!, bm.ayahNumber)
                                    } else 0

                                    coroutineScope.launch {
                                        if (targetIndex >= 0) {
                                            listState.animateScrollToItem(targetIndex)
                                        }
                                    }
                                    Toast.makeText(context, "Transported to Ayah ${bm.ayahNumber}", Toast.LENGTH_SHORT).show()
                                } else {
                                    val nextIndex = (transportBookmarkIndex + 1) % currentChapterBookmarks.size
                                    transportBookmarkIndex = nextIndex
                                    val bm = currentChapterBookmarks[nextIndex]
                                    val targetIndex = if (isParaMode && paraDetail != null) {
                                        findAyahItemIndexInPara(paraDetail!!, bm.surahNumber, bm.ayahNumber)
                                    } else if (!isParaMode && surahDetail != null) {
                                        findAyahItemIndexInSurah(surahDetail!!, bm.ayahNumber)
                                    } else 0

                                    coroutineScope.launch {
                                        if (targetIndex >= 0) {
                                            listState.animateScrollToItem(targetIndex)
                                        }
                                    }
                                    Toast.makeText(
                                        context,
                                        "Transported to Bookmark ${nextIndex + 1} of ${currentChapterBookmarks.size} (Ayah ${bm.ayahNumber})",
                                        Toast.LENGTH_SHORT
                                    ).show()
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
                                tint = if (keepScreenOn) markerColor else textColor.copy(alpha = 0.5f)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = backgroundColor.copy(alpha = 0.96f)
                    )
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                // Sleek Floating Control Bar
                Surface(
                    color = headerBgColor,
                    shadowElevation = 10.dp,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp)
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
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = textColor.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
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

                            // Theme palette dots
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ThemeDot(
                                    color = Color(0xFFFAF7F2),
                                    borderColor = Color(0xFF2D5C4C),
                                    isSelected = readingTheme == ReadingTheme.DEFAULT,
                                    onClick = { quranViewModel.setReadingTheme(ReadingTheme.DEFAULT) }
                                )
                                ThemeDot(
                                    color = Color(0xFFF4ECDC),
                                    borderColor = Color(0xFF7A562D),
                                    isSelected = readingTheme == ReadingTheme.SEPIA,
                                    onClick = { quranViewModel.setReadingTheme(ReadingTheme.SEPIA) }
                                )
                                ThemeDot(
                                    color = Color(0xFF121615),
                                    borderColor = Color(0xFF74C2A3),
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
                            // Previous
                            if (isParaMode) {
                                val currentNum = selectedParaNumber ?: 1
                                IconButton(
                                    onClick = {
                                        if (currentNum > 1) {
                                            quranViewModel.selectPara(currentNum - 1)
                                        }
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
                                        if (currentNum > 1) {
                                            quranViewModel.selectSurah(currentNum - 1)
                                        }
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
                                    text = "${fontSizeSp} pt",
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

                            // Next
                            if (isParaMode) {
                                val currentNum = selectedParaNumber ?: 1
                                IconButton(
                                    onClick = {
                                        if (currentNum < 30) {
                                            quranViewModel.selectPara(currentNum + 1)
                                        }
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
                                        if (currentNum < 114) {
                                            quranViewModel.selectSurah(currentNum + 1)
                                        }
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
        containerColor = backgroundColor,
        modifier = modifier
    ) { innerPadding ->
        val interactionSource = remember { MutableInteractionSource() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    showControls = !showControls
                }
        ) {
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
            } else if (isParaMode) {
                val detail = paraDetail
                if (detail == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Select a Para to read.", color = textColor)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(
                            top = if (showControls) innerPadding.calculateTopPadding() + 8.dp else 24.dp,
                            bottom = if (showControls) innerPadding.calculateBottomPadding() + 24.dp else 40.dp,
                            start = 18.dp,
                            end = 18.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        detail.surahSections.forEach { section ->
                            item(key = "para_${detail.info.number}_sec_${section.surahNumber}_hdr") {
                                ChapterHeaderBanner(
                                    surahNumber = section.surahNumber,
                                    surahNameEnglish = section.surahNameEnglish,
                                    surahNameArabic = section.surahNameArabic,
                                    subtitle = "Surah ${section.surahNumber}",
                                    markerColor = markerColor,
                                    textColor = textColor
                                )

                                val bismillahText = section.bismillah
                                if (!bismillahText.isNullOrEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = bismillahText,
                                        style = TextStyle(
                                            fontSize = (fontSizeSp + 3).sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = if (arabicFontStyle == ArabicFontStyle.UTHMANI) FontFamily.Serif else FontFamily.Default,
                                            textAlign = TextAlign.Center,
                                            textDirection = TextDirection.Rtl
                                        ),
                                        color = textColor,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                } else {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }

                            itemsIndexed(
                                items = section.verses,
                                key = { vIdx, _ -> "para_${detail.info.number}_s_${section.surahNumber}_v_$vIdx" }
                            ) { vIdx, verseStr ->
                                val parsed = remember(verseStr) { parseAyah(verseStr, fallbackNumber = vIdx + 1) }
                                val isBookmarked = "para_${detail.info.number}_s_${section.surahNumber}_a_${parsed.ayahNumber}" in bookmarkedAyahKeys

                                AyahReaderItem(
                                    surahNumber = section.surahNumber,
                                    ayahNumber = parsed.ayahNumber,
                                    arabicText = parsed.arabicText,
                                    fontSizeSp = fontSizeSp,
                                    fontStyle = arabicFontStyle,
                                    textColor = textColor,
                                    markerColor = markerColor,
                                    isBookmarked = isBookmarked,
                                    onLongClick = remember(isBookmarked, section.surahNumber, parsed.ayahNumber, parsed.arabicText) {
                                        {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            if (isBookmarked) {
                                                quranViewModel.removeBookmark(
                                                    mode = ReadMode.PARA,
                                                    id = detail.info.number,
                                                    surahNumber = section.surahNumber,
                                                    ayahNumber = parsed.ayahNumber
                                                )
                                                Toast.makeText(context, "Bookmark removed for Ayah ${parsed.ayahNumber}", Toast.LENGTH_SHORT).show()
                                            } else {
                                                quranViewModel.bookmarkAyah(
                                                    mode = ReadMode.PARA,
                                                    id = detail.info.number,
                                                    surahNumber = section.surahNumber,
                                                    surahNameEn = section.surahNameEnglish,
                                                    surahNameAr = section.surahNameArabic,
                                                    ayahNumber = parsed.ayahNumber,
                                                    ayahText = parsed.arabicText
                                                )
                                                Toast.makeText(context, "Ayah ${parsed.ayahNumber} Bookmarked", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    onBookmarkClick = remember(section.surahNumber, parsed.ayahNumber) {
                                        {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            quranViewModel.removeBookmark(
                                                mode = ReadMode.PARA,
                                                id = detail.info.number,
                                                surahNumber = section.surahNumber,
                                                ayahNumber = parsed.ayahNumber
                                            )
                                            Toast.makeText(context, "Bookmark removed for Ayah ${parsed.ayahNumber}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onToggleControls = onToggleControls
                                )
                            }
                        }
                    }
                }
            } else {
                val detail = surahDetail
                if (detail == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Select a Surah to read.", color = textColor)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(
                            top = if (showControls) innerPadding.calculateTopPadding() + 8.dp else 24.dp,
                            bottom = if (showControls) innerPadding.calculateBottomPadding() + 24.dp else 40.dp,
                            start = 18.dp,
                            end = 18.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item(key = "surah_header_${detail.info.number}") {
                            val subtitle = "${detail.info.englishMeaning} • ${if (detail.info.revelationType == RevelationType.MAKKI) "Makki" else "Madani"} • ${detail.info.versesCount} Verses • Para ${detail.info.startingPara}"
                            ChapterHeaderBanner(
                                surahNumber = detail.info.number,
                                surahNameEnglish = detail.info.transliteration,
                                surahNameArabic = detail.info.arabicName,
                                subtitle = subtitle,
                                markerColor = markerColor,
                                textColor = textColor
                            )

                            val bismillahText = detail.bismillah
                            if (!bismillahText.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = bismillahText,
                                    style = TextStyle(
                                        fontSize = (fontSizeSp + 3).sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = if (arabicFontStyle == ArabicFontStyle.UTHMANI) FontFamily.Serif else FontFamily.Default,
                                        textAlign = TextAlign.Center,
                                        textDirection = TextDirection.Rtl
                                    ),
                                    color = textColor,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            } else {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        itemsIndexed(
                            items = detail.verses,
                            key = { vIdx, _ -> "surah_${detail.info.number}_v_$vIdx" }
                        ) { vIdx, verseStr ->
                            val parsed = remember(verseStr) { parseAyah(verseStr, fallbackNumber = vIdx + 1) }
                            val isBookmarked = "surah_${detail.info.number}_a_${parsed.ayahNumber}" in bookmarkedAyahKeys

                            AyahReaderItem(
                                surahNumber = detail.info.number,
                                ayahNumber = parsed.ayahNumber,
                                arabicText = parsed.arabicText,
                                fontSizeSp = fontSizeSp,
                                fontStyle = arabicFontStyle,
                                textColor = textColor,
                                markerColor = markerColor,
                                isBookmarked = isBookmarked,
                                onLongClick = remember(isBookmarked, detail.info.number, parsed.ayahNumber, parsed.arabicText) {
                                    {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (isBookmarked) {
                                            quranViewModel.removeBookmark(
                                                mode = ReadMode.SURAH,
                                                id = detail.info.number,
                                                surahNumber = detail.info.number,
                                                ayahNumber = parsed.ayahNumber
                                            )
                                            Toast.makeText(context, "Bookmark removed for Ayah ${parsed.ayahNumber}", Toast.LENGTH_SHORT).show()
                                        } else {
                                            quranViewModel.bookmarkAyah(
                                                mode = ReadMode.SURAH,
                                                id = detail.info.number,
                                                surahNumber = detail.info.number,
                                                surahNameEn = detail.info.transliteration,
                                                surahNameAr = detail.info.arabicName,
                                                ayahNumber = parsed.ayahNumber,
                                                ayahText = parsed.arabicText
                                            )
                                            Toast.makeText(context, "Ayah ${parsed.ayahNumber} Bookmarked", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onBookmarkClick = remember(detail.info.number, parsed.ayahNumber) {
                                    {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        quranViewModel.removeBookmark(
                                            mode = ReadMode.SURAH,
                                            id = detail.info.number,
                                            surahNumber = detail.info.number,
                                            ayahNumber = parsed.ayahNumber
                                        )
                                        Toast.makeText(context, "Bookmark removed for Ayah ${parsed.ayahNumber}", Toast.LENGTH_SHORT).show()
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

private fun findAyahItemIndexInPara(paraDetail: com.example.data.quran.ParaDetail, targetSurah: Int, targetAyah: Int): Int {
    var currentIndex = 0
    for (section in paraDetail.surahSections) {
        currentIndex++ // for section header banner item
        for ((idx, verseStr) in section.verses.withIndex()) {
            val parsed = parseAyah(verseStr, fallbackNumber = idx + 1)
            if (section.surahNumber == targetSurah && parsed.ayahNumber == targetAyah) {
                return currentIndex
            }
            currentIndex++
        }
    }
    return 0
}

private fun findAyahItemIndexInSurah(surahDetail: SurahDetail, targetAyah: Int): Int {
    var currentIndex = 1 // index 0 is surah header banner
    for ((idx, verseStr) in surahDetail.verses.withIndex()) {
        val parsed = parseAyah(verseStr, fallbackNumber = idx + 1)
        if (parsed.ayahNumber == targetAyah) {
            return currentIndex
        }
        currentIndex++
    }
    return 0
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AyahReaderItem(
    surahNumber: Int,
    ayahNumber: Int,
    arabicText: String,
    fontSizeSp: Int,
    fontStyle: ArabicFontStyle,
    textColor: Color,
    markerColor: Color,
    isBookmarked: Boolean,
    onLongClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onToggleControls: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lineHeightVal = if (fontStyle == ArabicFontStyle.INDO_PAK) (fontSizeSp * 2.1).sp else (fontSizeSp * 1.95).sp
    val letterSpacingVal = if (fontStyle == ArabicFontStyle.INDO_PAK) 0.5.sp else 0.sp

    val annotatedText = remember(arabicText, ayahNumber, markerColor, fontSizeSp) {
        val digits = toArabicIndicDigits(ayahNumber)
        buildAnnotatedString {
            append(arabicText.trim())
            append("  ")
            withStyle(
                SpanStyle(
                    color = markerColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = (fontSizeSp * 0.95f).sp
                )
            ) {
                append("۝")
                append(digits)
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isBookmarked) markerColor.copy(alpha = 0.12f) else Color.Transparent,
        border = if (isBookmarked) BorderStroke(1.2.dp, markerColor.copy(alpha = 0.65f)) else null,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onLongClick = onLongClick,
                onClick = {
                    if (isBookmarked) {
                        onBookmarkClick()
                    } else {
                        onToggleControls()
                    }
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (isBookmarked) 14.dp else 6.dp,
                    vertical = if (isBookmarked) 10.dp else 6.dp
                )
        ) {
            if (isBookmarked) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Bookmarked",
                            tint = markerColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Bookmarked · Tap to remove",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = markerColor
                        )
                    }
                    Text(
                        text = "Ayah $ayahNumber",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        ),
                        color = textColor.copy(alpha = 0.6f)
                    )
                }
            }

            Text(
                text = annotatedText,
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
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Noble Mushaf chapter header: Dignified typographic rule with Arabic calligraphy name,
 * transliteration, and revelation details without bulky cards.
 */
@Composable
private fun ChapterHeaderBanner(
    surahNumber: Int?,
    surahNameEnglish: String,
    surahNameArabic: String,
    subtitle: String,
    markerColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Decorative top hairline with subtle central diamond
        OrnamentalHeaderDivider(color = markerColor.copy(alpha = 0.4f))

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (surahNumber != null) "$surahNumber. $surahNameEnglish" else surahNameEnglish,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp
                    ),
                    color = textColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.65f)
                )
            }

            Text(
                text = surahNameArabic,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    fontFamily = FontFamily.Serif
                ),
                color = markerColor
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        OrnamentalHeaderDivider(color = markerColor.copy(alpha = 0.4f))
    }
}

@Composable
private fun OrnamentalHeaderDivider(
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp),
        contentAlignment = Alignment.Center
    ) {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 0.8.dp,
            color = color
        )
        Canvas(modifier = Modifier.size(10.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val halfSize = size.width / 2f * 0.7f
            // Draw a subtle ornamental diamond pip in the center of the divider
            drawCircle(
                color = color,
                radius = halfSize * 0.7f,
                center = center
            )
        }
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
            .size(24.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 2.dp else 0.8.dp,
                color = if (isSelected) borderColor else Color.Gray.copy(alpha = 0.35f),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
