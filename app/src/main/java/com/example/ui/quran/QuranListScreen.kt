package com.example.ui.quran

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.data.quran.LastReadBookmark
import com.example.data.quran.ParaInfo
import com.example.data.quran.ReadMode
import com.example.data.quran.RevelationType
import com.example.data.quran.SurahInfo
import com.example.ui.components.ChiseledSanctuaryButton
import com.example.ui.components.ManuscriptOrnamentDivider
import com.example.ui.components.OctagonalNumberMedallion
import com.example.ui.components.PeacefulEmptyState
import com.example.ui.components.SanctuaryTopHeader
import com.example.ui.components.parseAyah
import com.example.ui.theme.SanctuaryEmeraldDark
import com.example.ui.theme.SanctuaryEmeraldMedium
import com.example.ui.theme.SanctuaryGoldAmber
import com.example.ui.theme.SanctuaryGoldBright
import com.example.ui.theme.SanctuaryGoldHalo
import com.example.ui.theme.SanctuaryGoldWarm
import com.example.ui.theme.SanctuaryParchmentWarm
import com.example.ui.viewmodel.QuranViewModel

private data class ParaDivisionGroup(
    val title: String,
    val subtitle: String,
    val rangeLabel: String,
    val paras: List<ParaInfo>
)

@Composable
fun QuranListScreen(
    quranViewModel: QuranViewModel,
    onOpenParaReader: (Int) -> Unit,
    onOpenSurahReader: (Int) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0 = Paras, 1 = Surahs, 2 = Bookmarks
    var searchQuery by remember { mutableStateOf("") }
    var selectedParaDivision by remember { mutableIntStateOf(0) } // 0 = All, 1 = 1..10, 2 = 11..20, 3 = 21..30
    var isGridMode by remember { mutableStateOf(false) }

    val allParas = quranViewModel.paraList
    val allSurahs = quranViewModel.surahList
    val lastReadBookmark by quranViewModel.lastReadBookmark.collectAsState()
    val allBookmarks by quranViewModel.allBookmarks.collectAsState()

    val filteredParas = remember(searchQuery, selectedParaDivision, allParas) {
        val divisionFiltered = when (selectedParaDivision) {
            1 -> allParas.filter { it.number in 1..10 }
            2 -> allParas.filter { it.number in 11..20 }
            3 -> allParas.filter { it.number in 21..30 }
            else -> allParas
        }
        if (searchQuery.isBlank()) {
            divisionFiltered
        } else {
            val q = searchQuery.trim().lowercase()
            divisionFiltered.filter {
                it.number.toString() == q ||
                    it.transliteration.lowercase().contains(q) ||
                    it.arabicTitle.contains(q) ||
                    it.surahRange.lowercase().contains(q)
            }
        }
    }

    val paraDivisionGroups = remember(filteredParas, searchQuery, selectedParaDivision) {
        if (searchQuery.isNotBlank() || selectedParaDivision != 0) {
            listOf(
                ParaDivisionGroup(
                    title = when (selectedParaDivision) {
                        1 -> "First Third • Paras 1 – 10"
                        2 -> "Middle Third • Paras 11 – 20"
                        3 -> "Final Third • Paras 21 – 30"
                        else -> "Matching Paras (${filteredParas.size})"
                    },
                    subtitle = when (selectedParaDivision) {
                        1 -> "Al-Fatihah to At-Tawbah"
                        2 -> "Yunus to Al-Ankabut"
                        3 -> "Ar-Rum to An-Nas"
                        else -> "Filtered results"
                    },
                    rangeLabel = "${filteredParas.size} Paras",
                    paras = filteredParas
                )
            )
        } else {
            listOf(
                ParaDivisionGroup(
                    title = "First Third • Paras 1 – 10",
                    subtitle = "From Surah Al-Fatihah to Surah At-Tawbah",
                    rangeLabel = "Juz 1–10",
                    paras = filteredParas.filter { it.number in 1..10 }
                ),
                ParaDivisionGroup(
                    title = "Middle Third • Paras 11 – 20",
                    subtitle = "From Surah At-Tawbah to Surah Al-Ankabut",
                    rangeLabel = "Juz 11–20",
                    paras = filteredParas.filter { it.number in 11..20 }
                ),
                ParaDivisionGroup(
                    title = "Final Third • Paras 21 – 30",
                    subtitle = "From Surah Al-Ankabut to Surah An-Nas",
                    rangeLabel = "Juz 21–30",
                    paras = filteredParas.filter { it.number in 21..30 }
                )
            ).filter { it.paras.isNotEmpty() }
        }
    }

    val filteredSurahs = remember(searchQuery, allSurahs) {
        if (searchQuery.isBlank()) {
            allSurahs
        } else {
            val q = searchQuery.trim().lowercase()
            allSurahs.filter {
                it.number.toString() == q ||
                    it.transliteration.lowercase().contains(q) ||
                    it.arabicName.contains(q) ||
                    it.englishMeaning.lowercase().contains(q) ||
                    "juz ${it.startingPara}".contains(q) ||
                    "para ${it.startingPara}".contains(q)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SanctuaryTopHeader(
            title = "The Noble Qur'an",
            onBack = onBack,
            leadingStaticGlyph = true,
            isQuranGlyph = true
        )

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val horizontalPad = if (maxWidth >= 640.dp) 28.dp else 20.dp

            LazyColumn(
                contentPadding = PaddingValues(
                    start = horizontalPad,
                    end = horizontalPad,
                    top = 18.dp,
                    bottom = 36.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("quran_index_list")
            ) {
                // 1. Segmented Mode Switcher Container (Paras / Surahs / Bookmarks)
                item(key = "mode_switcher") {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(5.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            QuranTabSegment(
                                title = "Paras (30)",
                                selected = selectedTabIndex == 0,
                                onClick = { selectedTabIndex = 0 },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("tab_paras")
                            )
                            QuranTabSegment(
                                title = "Surahs (114)",
                                selected = selectedTabIndex == 1,
                                onClick = { selectedTabIndex = 1 },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("tab_surahs")
                            )
                            QuranTabSegment(
                                title = "Saved (${allBookmarks.size})",
                                selected = selectedTabIndex == 2,
                                onClick = { selectedTabIndex = 2 },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("tab_bookmarks")
                            )
                        }
                    }
                }

                // 2. Continue Reading Card (scrolls naturally so the viewport stays airy)
                if (selectedTabIndex != 2 && searchQuery.isBlank()) {
                    lastReadBookmark?.let { bm ->
                        item(key = "continue_reading_banner") {
                            ContinueReadingCard(
                                bookmark = bm,
                                totalBookmarksCount = allBookmarks.size,
                                onResume = {
                                    if (bm.mode == ReadMode.PARA) {
                                        quranViewModel.selectPara(
                                            paraNumber = bm.id,
                                            targetAyah = bm.ayahNumber,
                                            targetSurah = bm.surahNumber
                                        )
                                        onOpenParaReader(bm.id)
                                    } else {
                                        quranViewModel.selectSurah(
                                            surahNumber = bm.id,
                                            targetAyah = bm.ayahNumber
                                        )
                                        onOpenSurahReader(bm.id)
                                    }
                                }
                            )
                        }
                    }
                }

                // 3. Search Bar & View Toggle Controls (for Paras and Surahs tabs)
                if (selectedTabIndex != 2) {
                    item(key = "search_and_controls") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("quran_search_input"),
                                    placeholder = {
                                        Text(
                                            text = if (selectedTabIndex == 0) "Search Para number, name, or Surah..."
                                            else "Search Surah name, number, or meaning...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(
                                                    imageVector = Icons.Default.Clear,
                                                    contentDescription = "Clear search",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )

                                if (selectedTabIndex == 0) {
                                    Surface(
                                        onClick = { isGridMode = !isGridMode },
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier
                                            .size(54.dp)
                                            .testTag("toggle_para_layout_button")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (isGridMode) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                                contentDescription = if (isGridMode) "Switch to spacious list" else "Switch to 2-column grid",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Quick Para Range Filter Pills so users don't face 30 unbroken rows
                            if (selectedTabIndex == 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ParaRangeFilterChip(
                                        label = "All 30",
                                        selected = selectedParaDivision == 0,
                                        onClick = { selectedParaDivision = 0 },
                                        modifier = Modifier.weight(1f)
                                    )
                                    ParaRangeFilterChip(
                                        label = "Juz 1–10",
                                        selected = selectedParaDivision == 1,
                                        onClick = { selectedParaDivision = 1 },
                                        modifier = Modifier.weight(1f)
                                    )
                                    ParaRangeFilterChip(
                                        label = "Juz 11–20",
                                        selected = selectedParaDivision == 2,
                                        onClick = { selectedParaDivision = 2 },
                                        modifier = Modifier.weight(1f)
                                    )
                                    ParaRangeFilterChip(
                                        label = "Juz 21–30",
                                        selected = selectedParaDivision == 3,
                                        onClick = { selectedParaDivision = 3 },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Tab Content with generous breathing room
                when (selectedTabIndex) {
                    0 -> {
                        if (filteredParas.isEmpty()) {
                            item(key = "empty_paras") {
                                PeacefulEmptyState(
                                    icon = Icons.Default.Search,
                                    title = "No Matching Para Found",
                                    description = "Try searching by Para number (1–30), transliterated name, or Surah range."
                                )
                            }
                        } else {
                            paraDivisionGroups.forEachIndexed { groupIndex, group ->
                                item(
                                    key = "division_header_${group.title}_$groupIndex",
                                    contentType = "division_header"
                                ) {
                                    ParaDivisionSectionHeader(
                                        title = group.title,
                                        subtitle = group.subtitle,
                                        badgeText = group.rangeLabel,
                                        modifier = Modifier.padding(
                                            top = if (groupIndex == 0) 4.dp else 14.dp,
                                            bottom = 2.dp
                                        )
                                    )
                                }

                                if (isGridMode) {
                                    val chunkedParas = group.paras.chunked(2)
                                    items(
                                        items = chunkedParas,
                                        key = { pair -> "para_grid_row_${pair.first().number}" },
                                        contentType = { "para_grid_row" }
                                    ) { pair ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            val isBm0 = lastReadBookmark?.id == pair[0].number && lastReadBookmark?.mode == ReadMode.PARA
                                            ParaGridTileCard(
                                                para = pair[0],
                                                isBookmarked = isBm0,
                                                onClick = { onOpenParaReader(pair[0].number) },
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (pair.size > 1) {
                                                val isBm1 = lastReadBookmark?.id == pair[1].number && lastReadBookmark?.mode == ReadMode.PARA
                                                ParaGridTileCard(
                                                    para = pair[1],
                                                    isBookmarked = isBm1,
                                                    onClick = { onOpenParaReader(pair[1].number) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                } else {
                                    items(
                                        items = group.paras,
                                        key = { "para_${it.number}" },
                                        contentType = { "para_card" }
                                    ) { para ->
                                        val isBm = lastReadBookmark?.id == para.number && lastReadBookmark?.mode == ReadMode.PARA
                                        ParaSpecimenCard(
                                            para = para,
                                            isBookmarked = isBm,
                                            onClick = { onOpenParaReader(para.number) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        if (filteredSurahs.isEmpty()) {
                            item(key = "empty_surahs") {
                                PeacefulEmptyState(
                                    icon = Icons.Default.Search,
                                    title = "No Matching Surah Found",
                                    description = "Try searching by Surah number (1–114), English meaning, or Arabic title."
                                )
                            }
                        } else {
                            items(
                                items = filteredSurahs,
                                key = { "surah_${it.number}" },
                                contentType = { "surah_card" }
                            ) { surah ->
                                val isBm = lastReadBookmark?.id == surah.number && lastReadBookmark?.mode == ReadMode.SURAH
                                SurahSpecimenCard(
                                    surah = surah,
                                    isBookmarked = isBm,
                                    onClick = { onOpenSurahReader(surah.number) }
                                )
                            }
                        }
                    }

                    else -> {
                        if (allBookmarks.isEmpty()) {
                            item(key = "empty_bookmarks") {
                                PeacefulEmptyState(
                                    icon = Icons.Default.BookmarkBorder,
                                    title = "No Saved Bookmarks Yet",
                                    description = "While reading any Para or Surah, tap the bookmark icon on any Ayah (or long-press the verse) to save multiple bookmarks and cycle through them effortlessly."
                                )
                            }
                        } else {
                            items(
                                items = allBookmarks,
                                key = { "${it.mode}_${it.id}_${it.surahNumber}_${it.ayahNumber}" }
                            ) { bm ->
                                SavedBookmarkSpecimenCard(
                                    bookmark = bm,
                                    onOpen = {
                                        if (bm.mode == ReadMode.PARA) {
                                            quranViewModel.selectPara(
                                                paraNumber = bm.id,
                                                targetAyah = bm.ayahNumber,
                                                targetSurah = bm.surahNumber
                                            )
                                            onOpenParaReader(bm.id)
                                        } else {
                                            quranViewModel.selectSurah(
                                                surahNumber = bm.id,
                                                targetAyah = bm.ayahNumber
                                            )
                                            onOpenSurahReader(bm.id)
                                        }
                                    },
                                    onRemove = {
                                        quranViewModel.removeBookmark(bm)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParaRangeFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier.height(38.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp
                ),
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ParaDivisionSectionHeader(
    title: String,
    subtitle: String,
    badgeText: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(
            thickness = 0.8.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
        )
    }
}

@Composable
private fun QuranTabSegment(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        modifier = modifier.height(42.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp
                ),
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ContinueReadingCard(
    bookmark: LastReadBookmark,
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
    totalBookmarksCount: Int = 1
) {
    Surface(
        onClick = onResume,
        modifier = modifier
            .fillMaxWidth()
            .testTag("continue_reading_card"),
        shape = RoundedCornerShape(20.dp),
        color = SanctuaryEmeraldDark,
        border = BorderStroke(1.2.dp, SanctuaryGoldWarm.copy(alpha = 0.85f)),
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SanctuaryGoldWarm.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        radius = 420f
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 18.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SanctuaryGoldWarm.copy(alpha = 0.2f))
                        .border(1.dp, SanctuaryGoldWarm, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = SanctuaryGoldBright,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "CONTINUE RECITATION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.1.sp,
                                fontSize = 10.sp
                            ),
                            color = SanctuaryGoldBright
                        )
                        if (totalBookmarksCount > 1) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SanctuaryGoldWarm.copy(alpha = 0.25f),
                                border = BorderStroke(0.6.dp, SanctuaryGoldBright.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "$totalBookmarksCount Saved",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    ),
                                    color = SanctuaryGoldBright,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = bookmark.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (bookmark.subtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = bookmark.subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color.White.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                ChiseledSanctuaryButton(
                    text = "Resume",
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    onClick = onResume,
                    modifier = Modifier.testTag("resume_reading_button")
                )
            }
        }
    }
}

/**
 * Illuminated Sanctuary Para card with gilded perimeter and live reading beacon.
 */
@Composable
private fun ParaSpecimenCard(
    para: ParaInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isBookmarked: Boolean = false
) {
    val borderColor = if (isBookmarked) SanctuaryGoldWarm else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)
    val borderWidth = if (isBookmarked) 1.5.dp else 1.dp

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(borderWidth, borderColor),
        tonalElevation = if (isBookmarked) 3.dp else 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("para_item_card_${para.number}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OctagonalNumberMedallion(
                number = para.number,
                size = 48.dp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "PARA ${para.number.toString().padStart(2, '0')}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp,
                            fontSize = 10.sp
                        ),
                        color = if (isBookmarked) SanctuaryGoldAmber else MaterialTheme.colorScheme.secondary
                    )
                    if (isBookmarked) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SanctuaryGoldWarm.copy(alpha = 0.18f),
                            border = BorderStroke(0.6.dp, SanctuaryGoldWarm.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "LAST READ",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 9.sp,
                                    letterSpacing = 0.5.sp
                                ),
                                color = SanctuaryGoldAmber,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = para.transliteration,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = para.surahRange,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right-aligned poised Arabic calligraphy badge & subtle verse count
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isBookmarked) SanctuaryEmeraldMedium.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    border = BorderStroke(
                        0.8.dp,
                        if (isBookmarked) SanctuaryGoldWarm.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                    )
                ) {
                    Text(
                        text = para.arabicTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }

                Text(
                    text = para.totalVersesDescription.substringBefore("•").trim(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * 30-Para Sacred Geometric Dial Tile for browsing Paras in an illuminated manuscript mosaic.
 */
@Composable
private fun ParaGridTileCard(
    para: ParaInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isBookmarked: Boolean = false
) {
    val borderColor = if (isBookmarked) SanctuaryGoldWarm else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
    val borderWidth = if (isBookmarked) 1.5.dp else 1.dp

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(borderWidth, borderColor),
        tonalElevation = if (isBookmarked) 3.dp else 1.dp,
        modifier = modifier.testTag("para_grid_card_${para.number}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OctagonalNumberMedallion(
                    number = para.number,
                    size = 40.dp
                )

                if (isBookmarked) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SanctuaryGoldWarm.copy(alpha = 0.2f),
                        border = BorderStroke(0.6.dp, SanctuaryGoldWarm)
                    ) {
                        Text(
                            text = "ACTIVE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = SanctuaryGoldAmber,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Text(
                        text = para.arabicTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            fontSize = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "PARA ${para.number.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.9.sp,
                        fontSize = 10.sp
                    ),
                    color = if (isBookmarked) SanctuaryGoldAmber else MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = para.transliteration,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = para.surahRange.substringBefore("—").trim(),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Airy, well-proportioned Surah card with clean typographic hierarchy and revelation emblems.
 */
@Composable
private fun SurahSpecimenCard(
    surah: SurahInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isBookmarked: Boolean = false
) {
    val isMakki = surah.revelationType == RevelationType.MAKKI
    val revLabel = if (isMakki) "MAKKI" else "MADANI"
    val borderColor = if (isBookmarked) SanctuaryGoldWarm else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
    val borderWidth = if (isBookmarked) 1.5.dp else 1.dp

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(borderWidth, borderColor),
        tonalElevation = if (isBookmarked) 3.dp else 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("surah_item_card_${surah.number}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OctagonalNumberMedallion(
                number = surah.number,
                size = 48.dp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "SURAH ${surah.number.toString().padStart(3, '0')} • PARA ${surah.startingPara}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            fontSize = 10.sp
                        ),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = if (isMakki) SanctuaryGoldWarm.copy(alpha = 0.18f) else SanctuaryEmeraldMedium.copy(alpha = 0.15f),
                        border = BorderStroke(0.5.dp, if (isMakki) SanctuaryGoldAmber.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = revLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            ),
                            color = if (isMakki) SanctuaryGoldAmber else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                Text(
                    text = surah.transliteration,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${surah.englishMeaning} • ${surah.versesCount} Verses",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
            ) {
                Text(
                    text = surah.arabicName,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        fontSize = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun SavedBookmarkSpecimenCard(
    bookmark: LastReadBookmark,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = bookmark.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (bookmark.subtitle.isNotBlank()) {
                        Text(
                            text = bookmark.subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (bookmark.ayahText.isNotBlank()) {
                        val cleanPreview = remember(bookmark.ayahText, bookmark.ayahNumber) {
                            parseAyah(bookmark.ayahText, bookmark.ayahNumber).arabicText
                        }
                        Text(
                            text = cleanPreview,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Serif,
                                fontSize = 15.sp
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove bookmark",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
