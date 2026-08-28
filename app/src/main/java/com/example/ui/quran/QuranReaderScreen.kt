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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
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
    val lastReadBookmark by quranViewModel.lastReadBookmark.collectAsState()

    val fontSizeSp by quranViewModel.fontSizeSp.collectAsState()
    val readingTheme by quranViewModel.readingTheme.collectAsState()
    val arabicFontStyle by quranViewModel.arabicFontStyle.collectAsState()
    val keepScreenOn by quranViewModel.keepScreenOn.collectAsState()

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
            Color(0xFF121615), // Serene night obsidian
            Color(0xFFDCE2DF), // Soft sage ink
            Color(0xFF1C2220), // Subtle dark header
            Color(0xFF74C2A3)  // Soft luminous emerald
        )
    }

    val isParaMode = activeMode == ReadMode.PARA

    val isCurrentBookmarked = if (isParaMode) {
        lastReadBookmark?.mode == ReadMode.PARA && lastReadBookmark?.id == selectedParaNumber
    } else {
        lastReadBookmark?.mode == ReadMode.SURAH && lastReadBookmark?.id == selectedSurahNumber
    }

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
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = textColor
                            )
                        }
                    },
                    actions = {
                        // Bookmark button
                        IconButton(
                            onClick = {
                                if (isParaMode && paraDetail != null) {
                                    val detail = paraDetail!!
                                    val bookmark = com.example.data.quran.LastReadBookmark(
                                        mode = ReadMode.PARA,
                                        id = detail.info.number,
                                        title = "Para ${detail.info.number} — ${detail.info.transliteration}",
                                        arabicTitle = detail.info.arabicTitle,
                                        subtitle = detail.info.surahRange
                                    )
                                    quranViewModel.saveBookmark(bookmark)
                                    Toast.makeText(context, "Bookmarked Para ${detail.info.number}", Toast.LENGTH_SHORT).show()
                                } else if (!isParaMode && surahDetail != null) {
                                    val detail = surahDetail!!
                                    val bookmark = com.example.data.quran.LastReadBookmark(
                                        mode = ReadMode.SURAH,
                                        id = detail.info.number,
                                        title = "Surah ${detail.info.number} — ${detail.info.transliteration}",
                                        arabicTitle = detail.info.arabicName,
                                        subtitle = "${detail.info.englishMeaning} • ${detail.info.versesCount} Verses"
                                    )
                                    quranViewModel.saveBookmark(bookmark)
                                    Toast.makeText(context, "Bookmarked Surah ${detail.info.transliteration}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isCurrentBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark this reading position",
                                tint = if (isCurrentBookmarked) markerColor else textColor.copy(alpha = 0.65f)
                            )
                        }

                        // Keep Screen On Toggle
                        IconButton(
                            onClick = { quranViewModel.toggleKeepScreenOn() }
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
                                    enabled = currentNum > 1
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
                                    enabled = currentNum > 1
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
                                    modifier = Modifier.size(36.dp)
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
                                    modifier = Modifier.size(36.dp)
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
                                    enabled = currentNum < 30
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
                                    enabled = currentNum < 114
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
            if (isParaMode) {
                val detail = paraDetail
                if (detail == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Select a Para to read.", color = textColor)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            top = if (showControls) innerPadding.calculateTopPadding() + 8.dp else 24.dp,
                            bottom = if (showControls) innerPadding.calculateBottomPadding() + 24.dp else 40.dp,
                            start = 20.dp,
                            end = 20.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(32.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = detail.surahSections,
                            key = { "para_${detail.info.number}_sec_${it.surahNumber}" }
                        ) { section ->
                            SurahSectionReaderFlow(
                                section = section,
                                fontSizeSp = fontSizeSp,
                                fontStyle = arabicFontStyle,
                                textColor = textColor,
                                markerColor = markerColor
                            )
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
                        contentPadding = PaddingValues(
                            top = if (showControls) innerPadding.calculateTopPadding() + 8.dp else 24.dp,
                            bottom = if (showControls) innerPadding.calculateBottomPadding() + 24.dp else 40.dp,
                            start = 20.dp,
                            end = 20.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(28.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item(key = "surah_reader_${detail.info.number}") {
                            SingleSurahReaderFlow(
                                detail = detail,
                                fontSizeSp = fontSizeSp,
                                fontStyle = arabicFontStyle,
                                textColor = textColor,
                                markerColor = markerColor
                            )
                        }
                    }
                }
            }
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
private fun SurahSectionReaderFlow(
    section: SurahSection,
    fontSizeSp: Int,
    fontStyle: ArabicFontStyle,
    textColor: Color,
    markerColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Chapter Header Banner
        ChapterHeaderBanner(
            surahNumber = section.surahNumber,
            surahNameEnglish = section.surahNameEnglish,
            surahNameArabic = section.surahNameArabic,
            subtitle = "Surah ${section.surahNumber}",
            markerColor = markerColor,
            textColor = textColor
        )

        // Bismillah
        val bismillahText = section.bismillah
        if (!bismillahText.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = bismillahText,
                style = TextStyle(
                    fontSize = (fontSizeSp + 3).sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = if (fontStyle == ArabicFontStyle.UTHMANI) FontFamily.Serif else FontFamily.Default,
                    textAlign = TextAlign.Center,
                    textDirection = TextDirection.Rtl
                ),
                color = textColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Continuous Flowing Verses with Integrated Ayah Markers
        MushafVersesFlow(
            rawVerses = section.verses,
            fontSizeSp = fontSizeSp,
            fontStyle = fontStyle,
            textColor = textColor,
            markerColor = markerColor
        )
    }
}

@Composable
private fun SingleSurahReaderFlow(
    detail: SurahDetail,
    fontSizeSp: Int,
    fontStyle: ArabicFontStyle,
    textColor: Color,
    markerColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Chapter Header Banner
        val subtitle = "${detail.info.englishMeaning} • ${if (detail.info.revelationType == RevelationType.MAKKI) "Makki" else "Madani"} • ${detail.info.versesCount} Verses • Para ${detail.info.startingPara}"
        ChapterHeaderBanner(
            surahNumber = detail.info.number,
            surahNameEnglish = detail.info.transliteration,
            surahNameArabic = detail.info.arabicName,
            subtitle = subtitle,
            markerColor = markerColor,
            textColor = textColor
        )

        // Bismillah
        val bismillahText = detail.bismillah
        if (!bismillahText.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = bismillahText,
                style = TextStyle(
                    fontSize = (fontSizeSp + 3).sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = if (fontStyle == ArabicFontStyle.UTHMANI) FontFamily.Serif else FontFamily.Default,
                    textAlign = TextAlign.Center,
                    textDirection = TextDirection.Rtl
                ),
                color = textColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Continuous Flowing Verses with Integrated Ayah Markers
        MushafVersesFlow(
            rawVerses = detail.verses,
            fontSizeSp = fontSizeSp,
            fontStyle = fontStyle,
            textColor = textColor,
            markerColor = markerColor
        )
    }
}

/**
 * Renders an array of verse strings into a continuous, flowing Mushaf page
 * where each Ayah ends seamlessly with a custom inline Ayah End Marker with Arabic-Indic digits.
 */
@Composable
private fun MushafVersesFlow(
    rawVerses: List<String>,
    fontSizeSp: Int,
    fontStyle: ArabicFontStyle,
    textColor: Color,
    markerColor: Color,
    modifier: Modifier = Modifier
) {
    val parsedAyahs = remember(rawVerses) {
        rawVerses.mapIndexed { index, verseStr ->
            parseAyah(verseStr, fallbackNumber = index + 1)
        }
    }

    val lineHeightVal = if (fontStyle == ArabicFontStyle.INDO_PAK) (fontSizeSp * 2.2).sp else (fontSizeSp * 1.95).sp
    val letterSpacingVal = if (fontStyle == ArabicFontStyle.INDO_PAK) 0.5.sp else 0.sp

    val inlineContentMap = remember(parsedAyahs, fontSizeSp, markerColor, textColor) {
        val map = mutableMapOf<String, InlineTextContent>()
        parsedAyahs.forEachIndexed { index, ayah ->
            val inlineId = "ayah_marker_${ayah.ayahNumber}_$index"
            map[inlineId] = InlineTextContent(
                Placeholder(
                    width = (fontSizeSp * 1.4f).sp,
                    height = (fontSizeSp * 1.4f).sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                )
            ) {
                AyahEndMarker(
                    ayahNumber = ayah.ayahNumber,
                    fontSizeSp = fontSizeSp,
                    markerColor = markerColor,
                    textColor = textColor
                )
            }
        }
        map
    }

    val annotatedText = remember(parsedAyahs) {
        buildAnnotatedString {
            parsedAyahs.forEachIndexed { index, ayah ->
                append(ayah.arabicText)
                append(" ")
                val inlineId = "ayah_marker_${ayah.ayahNumber}_$index"
                appendInlineContent(id = inlineId, alternateText = " ۝${ayah.ayahNumber} ")
                if (index < parsedAyahs.size - 1) {
                    append("  ")
                }
            }
        }
    }

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
        modifier = modifier.fillMaxWidth()
    )
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
