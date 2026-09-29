package com.example.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.DistributionEntity
import com.example.data.db.DistributionType
import com.example.data.quran.ReadMode
import com.example.ui.components.ManuscriptOrnamentDivider
import com.example.ui.components.PrimarySanctuaryButton
import com.example.ui.components.ResizableSectionContainer
import com.example.ui.components.SanctuaryGeometricGlyph
import com.example.ui.components.SanctuaryTopHeader
import com.example.ui.components.SecondaryEditorialButton
import com.example.ui.distribution.ShareDistributionDialog
import com.example.ui.quran.ContinueReadingCard
import com.example.ui.viewmodel.DistributionViewModel
import com.example.ui.viewmodel.QuranViewModel
import com.example.util.HijriDateUtils
import java.util.Calendar

@Composable
fun HomeScreen(
    distributionViewModel: DistributionViewModel,
    quranViewModel: QuranViewModel? = null,
    onNavigateToQuran: () -> Unit,
    onNavigateToDistributions: () -> Unit,
    onCreateNewDistribution: (DistributionType) -> Unit = { onNavigateToDistributions() },
    onOpenDistributionDetail: (Long) -> Unit,
    onOpenParaReader: (Int) -> Unit = {},
    onOpenSurahReader: (Int) -> Unit = {},
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val distributions by distributionViewModel.distributions.collectAsState()
    val participantsByDistribution by distributionViewModel.participantsByDistribution.collectAsState()
    var activeShareSummaryText by remember { mutableStateOf<String?>(null) }

    val lastReadBookmark by quranViewModel?.lastReadBookmark?.collectAsState()
        ?: remember { mutableStateOf(null) }
    val allBookmarks by quranViewModel?.allBookmarks?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) }

    val currentDateInfo = remember {
        val cal = Calendar.getInstance()
        HijriDateUtils.getMonthInfo(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SanctuaryTopHeader(
            title = "Quran Baanto",
            subtitle = currentDateInfo.hijriFullText,
            leadingLogo = true,
            actions = {
                Surface(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("home_settings_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        )

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val isWideScreen = maxWidth >= 640.dp
            val horizontalPad = if (isWideScreen) 28.dp else 20.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizontalPad, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 1. Clean Bismillah Header Card
                BismillahHeaderCard(
                    gregorianDateText = currentDateInfo.gregorianFullText,
                    hijriArabicText = currentDateInfo.hijriMonthArabic
                )

                // 2. Continue Reading Container (if user has a saved bookmark)
                lastReadBookmark?.let { bookmark ->
                    ContinueReadingCard(
                        bookmark = bookmark,
                        totalBookmarksCount = allBookmarks.size,
                        onResume = {
                            if (bookmark.mode == ReadMode.PARA) {
                                quranViewModel?.selectPara(
                                    paraNumber = bookmark.id,
                                    targetAyah = bookmark.ayahNumber,
                                    targetSurah = bookmark.surahNumber
                                )
                                onOpenParaReader(bookmark.id)
                            } else {
                                quranViewModel?.selectSurah(
                                    surahNumber = bookmark.id,
                                    targetAyah = bookmark.ayahNumber
                                )
                                onOpenSurahReader(bookmark.id)
                            }
                        }
                    )
                }

                // 3. Primary Sections (Qur'an & Khatm Distributions) with clean static vector icons
                if (isWideScreen) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        QuranHomeSectionCard(
                            onNavigateToQuran = onNavigateToQuran,
                            modifier = Modifier.weight(1f)
                        )
                        DistributionsHomeSectionCard(
                            onNavigateToDistributions = onNavigateToDistributions,
                            onCreateNewKhatm = { onCreateNewDistribution(DistributionType.KHATM) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    QuranHomeSectionCard(
                        onNavigateToQuran = onNavigateToQuran
                    )
                    DistributionsHomeSectionCard(
                        onNavigateToDistributions = onNavigateToDistributions,
                        onCreateNewKhatm = { onCreateNewDistribution(DistributionType.KHATM) }
                    )
                }

                // 4. Current Khatm & Amal Sessions
                if (distributions.isNotEmpty()) {
                    ResizableSectionContainer(
                        title = "Active Sessions",
                        badgeText = "${distributions.size}",
                        badgeColor = MaterialTheme.colorScheme.secondaryContainer,
                        badgeContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        contentPadding = PaddingValues(24.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            distributions.take(3).forEach { dist ->
                                val distParticipants = participantsByDistribution[dist.id].orEmpty()
                                HomeDistributionRowCard(
                                    dist = dist,
                                    onClick = { onOpenDistributionDetail(dist.id) },
                                    onShareClick = {
                                        val summary = distributionViewModel.generateShareSummaryForDistribution(
                                            distribution = dist,
                                            participants = distParticipants
                                        )
                                        if (summary.isNotBlank()) {
                                            activeShareSummaryText = summary
                                        }
                                    }
                                )
                            }

                            if (distributions.size > 3) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = "View All (${distributions.size}) →",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .clickable(onClick = onNavigateToDistributions)
                                            .padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Minimalist Studio Footer
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ManuscriptOrnamentDivider()
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Version 2.0 • Created by Hambasy Studios",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }

    activeShareSummaryText?.let { summaryText ->
        ShareDistributionDialog(
            shareText = summaryText,
            onDismiss = { activeShareSummaryText = null }
        )
    }
}

@Composable
private fun BismillahHeaderCard(
    gregorianDateText: String,
    hijriArabicText: String,
    modifier: Modifier = Modifier
) {
    val goldYellow = Color(0xFFFCE4B3)
    val warmGoldAccent = Color(0xFFE5B966)
    val softGoldSubtext = Color(0xFFEBD7AD)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF0A382A),
        border = BorderStroke(1.2.dp, warmGoldAccent.copy(alpha = 0.55f)),
        shadowElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF072B20),
                            Color(0xFF0E4937),
                            Color(0xFF06241B)
                        )
                    )
                )
        ) {
            // Subtle, low-visibility Islamic geometric rosette/lattice pattern in the background
            Canvas(modifier = Modifier.matchParentSize()) {
                val patternColor = warmGoldAccent.copy(alpha = 0.055f)
                val stroke = Stroke(width = 1.dp.toPx())
                val step = 56.dp.toPx()
                val boxSize = 26.dp.toPx()

                var x = step / 2f
                while (x < size.width + step) {
                    var y = step / 2f
                    while (y < size.height + step) {
                        val centerPt = Offset(x, y)
                        val topLeft = Offset(x - boxSize / 2f, y - boxSize / 2f)
                        drawRect(
                            color = patternColor,
                            topLeft = topLeft,
                            size = Size(boxSize, boxSize),
                            style = stroke
                        )
                        rotate(degrees = 45f, pivot = centerPt) {
                            drawRect(
                                color = patternColor,
                                topLeft = topLeft,
                                size = Size(boxSize, boxSize),
                                style = stroke
                            )
                        }
                        y += step
                    }
                    x += step
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = gregorianDateText.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp,
                            fontSize = 11.sp
                        ),
                        color = softGoldSubtext.copy(alpha = 0.85f)
                    )

                    if (hijriArabicText.isNotBlank()) {
                        Text(
                            text = hijriArabicText,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = warmGoldAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        letterSpacing = 0.4.sp
                    ),
                    color = goldYellow,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "In the Name of Allah, the Most Gracious, the Most Merciful",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = softGoldSubtext.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun QuranHomeSectionCard(
    onNavigateToQuran: () -> Unit,
    modifier: Modifier = Modifier
) {
    ResizableSectionContainer(
        modifier = modifier.testTag("home_quran_module_card"),
        onClick = onNavigateToQuran,
        contentPadding = PaddingValues(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SanctuaryGeometricGlyph(
                isQuranGlyph = true,
                size = 54.dp
            )

            Spacer(modifier = Modifier.width(18.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "The Noble Qur'an",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Read by Para or Surah with saved bookmarks.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 19.sp,
                        fontSize = 13.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        PrimarySanctuaryButton(
            text = "Open Qur'an",
            icon = Icons.AutoMirrored.Filled.MenuBook,
            onClick = onNavigateToQuran,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DistributionsHomeSectionCard(
    onNavigateToDistributions: () -> Unit,
    onCreateNewKhatm: () -> Unit,
    modifier: Modifier = Modifier
) {
    ResizableSectionContainer(
        modifier = modifier.testTag("home_distributions_module_card"),
        onClick = onNavigateToDistributions,
        contentPadding = PaddingValues(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SanctuaryGeometricGlyph(
                isQuranGlyph = false,
                size = 54.dp
            )

            Spacer(modifier = Modifier.width(18.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Khatm & Amal",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Monthly rotating Para schedules and custom group recitations.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 19.sp,
                        fontSize = 13.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrimarySanctuaryButton(
                text = "New Session",
                icon = Icons.Default.Add,
                onClick = onCreateNewKhatm,
                modifier = Modifier.weight(1f)
            )
            SecondaryEditorialButton(
                text = "View All",
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                onClick = onNavigateToDistributions,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HomeDistributionRowCard(
    dist: DistributionEntity,
    onClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val isAmal = dist.type == DistributionType.AMAL

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.65f),
        border = BorderStroke(0.9.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                SanctuaryGeometricGlyph(
                    isQuranGlyph = !isAmal,
                    size = 40.dp
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = dist.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                onClick = onShareClick,
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
                modifier = Modifier.testTag("home_share_distribution_${dist.id}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share ${dist.name}",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Share",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                onClick = onClick,
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Open",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}
