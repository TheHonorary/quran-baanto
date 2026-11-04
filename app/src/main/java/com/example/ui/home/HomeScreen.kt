package com.example.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.ui.components.ChiseledSanctuaryButton
import com.example.ui.components.CircularProgressArc
import com.example.ui.components.LuminousOrbButton
import com.example.ui.components.ManuscriptOrnamentDivider
import com.example.ui.components.OctagonalNumberMedallion
import com.example.ui.components.SanctuaryGeometricGlyph
import com.example.ui.distribution.ShareDistributionDialog
import com.example.ui.theme.LocalBlueAccent
import com.example.ui.theme.OnlineGoldAccent
import com.example.ui.theme.SanctuaryEmeraldDark
import com.example.ui.theme.SanctuaryEmeraldMedium
import com.example.ui.theme.SanctuaryGoldBright
import com.example.ui.theme.SanctuaryGoldHalo
import com.example.ui.theme.SanctuaryGoldWarm
import com.example.ui.viewmodel.DistributionViewModel
import com.example.ui.viewmodel.QuranViewModel
import com.example.ui.viewmodel.StreakViewModel
import com.example.util.HijriDateUtils
import java.util.Calendar

@Composable
fun HomeScreen(
    distributionViewModel: DistributionViewModel,
    quranViewModel: QuranViewModel? = null,
    streakViewModel: StreakViewModel? = null,
    onNavigateToQuran: () -> Unit,
    onNavigateToDistributions: () -> Unit,
    onNavigateToStreaks: () -> Unit = {},
    onCreateNewDistribution: (DistributionType) -> Unit = { onNavigateToDistributions() },
    onOpenDistributionDetail: (Long) -> Unit,
    onOpenParaReader: (Int, Int?, Int?) -> Unit = { _, _, _ -> },
    onOpenSurahReader: (Int, Int?) -> Unit = { _, _ -> },
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val distributions by distributionViewModel.distributions.collectAsState()
    val participantsByDistribution by distributionViewModel.participantsByDistribution.collectAsState()
    var activeShareSummaryText by remember { mutableStateOf<String?>(null) }

    val streak by streakViewModel?.streakState?.collectAsState() ?: remember { mutableStateOf(null) }
    val todayLog by streakViewModel?.todayLog?.collectAsState() ?: remember { mutableStateOf(null) }

    val lastReadBookmark by quranViewModel?.lastReadBookmark?.collectAsState()
        ?: remember { mutableStateOf(null) }
    val allBookmarks by quranViewModel?.allBookmarks?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) }

    val currentCal = remember { Calendar.getInstance() }
    val currentYear = remember { currentCal.get(Calendar.YEAR) }
    val currentMonth = remember { currentCal.get(Calendar.MONTH) + 1 }
    val currentDateInfo = remember(currentYear, currentMonth) {
        HijriDateUtils.getMonthInfo(
            year = currentYear,
            month = currentMonth
        )
    }

    val userProfileName by distributionViewModel.userProfileName.collectAsState()

    // Find pending Khatm where user has an uncompleted Para or active portion
    val pendingKhatmInfo = remember(distributions, participantsByDistribution, userProfileName, currentYear, currentMonth) {
        var found: Triple<DistributionEntity, Int, Boolean>? = null
        for (dist in distributions) {
            val parts = participantsByDistribution[dist.id].orEmpty()
            val userPart = parts.firstOrNull { p ->
                (userProfileName.isNotBlank() && (p.name.equals(userProfileName, ignoreCase = true) || p.addedByProfileName.equals(userProfileName, ignoreCase = true))) ||
                p.isPinned
            }
            if (userPart != null) {
                val isDone = if (dist.type == DistributionType.KHATM) {
                    userPart.isCompletedForMonth(currentYear, currentMonth)
                } else {
                    userPart.isCompleted
                }
                val assignedPara = if (dist.type == DistributionType.ONE_TIME_KHATM && userPart.specificParaNumber != null) {
                    userPart.specificParaNumber!!
                } else if (dist.type == DistributionType.KHATM) {
                    val assignments = distributionViewModel.computeAssignmentsForDistribution(dist, parts, currentYear, currentMonth)
                    assignments.firstOrNull { it.participantId == userPart.id }?.startPara ?: 1
                } else {
                    1
                }
                found = Triple(dist, assignedPara, isDone)
                if (!isDone) break
            }
        }
        found ?: distributions.firstOrNull()?.let { Triple(it, 1, false) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Daily Noor Green Intro Header with Rotating Ayahs & Settings Button (matching reference drawing)
        DailyNoorTopBanner(
            hijriDate = currentDateInfo.hijriFullText,
            gregorianDate = currentDateInfo.gregorianFullText,
            onOpenSettings = onOpenSettings
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth >= 640.dp
            val horizontalPad = if (isWideScreen) 28.dp else 20.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                // 2. Continue From Last Bookmark (Prominent Spotlight beneath header, matching user drawing)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPad)
                ) {
                    ContinueFromLastBookmarkCard(
                        bookmark = lastReadBookmark,
                        totalBookmarks = allBookmarks.size,
                        onResume = {
                            lastReadBookmark?.let { bm ->
                                if (bm.mode == ReadMode.PARA) {
                                    quranViewModel?.selectPara(bm.id, bm.ayahNumber, bm.surahNumber)
                                    onOpenParaReader(bm.id, bm.ayahNumber, bm.surahNumber)
                                } else {
                                    quranViewModel?.selectSurah(bm.id, bm.ayahNumber)
                                    onOpenSurahReader(bm.id, bm.ayahNumber)
                                }
                            }
                        },
                        onStartQuran = onNavigateToQuran
                    )
                }

                // 3. Scroll Wheel: Latest Khatm & Highlights
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = horizontalPad),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Latest Khatm & Highlights",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Swipe →",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = horizontalPad),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Card 1: Active Khatm where user's portion is left to recite
                        if (pendingKhatmInfo != null) {
                            val (dist, assignedPara, isDone) = pendingKhatmInfo
                            item {
                                LatestKhatmDeckCard(
                                    distribution = dist,
                                    assignedPara = assignedPara,
                                    isAssignedCompleted = isDone,
                                    onOpenDistribution = { onOpenDistributionDetail(dist.id) },
                                    onRecitePara = { p -> onOpenParaReader(p, null, null) },
                                    modifier = Modifier.width(290.dp)
                                )
                            }
                        } else {
                            // Empty state card encouraging creating or joining a Khatm
                            item {
                                Surface(
                                    onClick = onNavigateToDistributions,
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.2.dp, OnlineGoldAccent.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .width(280.dp)
                                        .height(175.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = OnlineGoldAccent.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "Community Khatm",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = OnlineGoldAccent
                                                ),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = "No Active Khatm Session",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Create a new Khatm room or join with a room code.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Text(
                                                text = "Open Khatm →",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Card 2: Live Streak Flame Ember
                        if (streak?.isOptedIn == true) {
                            item {
                                StreakEmberDeckCard(
                                    streakDays = streak?.currentStreakDays ?: 0,
                                    ayahsRead = todayLog?.ayahsReadCount ?: 0,
                                    targetAyahs = todayLog?.targetAyahsCount ?: 10,
                                    isGoalMet = todayLog?.hasMetGoal == true,
                                    onContinue = {
                                        val p = streak?.lastReadPara ?: 1
                                        val s = streak?.lastReadSurah ?: 1
                                        val a = streak?.lastReadAyah ?: 1
                                        quranViewModel?.selectPara(p, a, s)
                                        onOpenParaReader(p, a, s)
                                    },
                                    onOpenStreaks = onNavigateToStreaks,
                                    modifier = Modifier.width(280.dp)
                                )
                            }
                        } else {
                            item {
                                Surface(
                                    onClick = onNavigateToStreaks,
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.2.dp, Color(0xFFFF9800).copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .width(280.dp)
                                        .height(175.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFFF9800).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "Recitation Streak",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFFF9800)
                                                ),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = "Ignite Daily Streak",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Build a consistent habit of Quran recitation.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Text(
                                                text = "Start Streak →",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFFF9800)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Shortcut 1: Surah Yasin (Heart of the Qur'an)
                        item {
                            HighlightShortcutDeckCard(
                                title = "Surah Yasin",
                                arabicTitle = "سُورَة يس",
                                badgeText = "Heart of Qur'an",
                                subtitle = "Surah 36 • 83 Verses • Makki",
                                accentColor = SanctuaryEmeraldMedium,
                                actionLabel = "Recite Yasin",
                                onClick = {
                                    quranViewModel?.selectSurah(36, 1)
                                    onOpenSurahReader(36, 1)
                                }
                            )
                        }

                        // Shortcut 2: Surah Al-Mulk (Nightly Protection)
                        item {
                            HighlightShortcutDeckCard(
                                title = "Surah Al-Mulk",
                                arabicTitle = "سُورَة الْمُلْك",
                                badgeText = "Nightly Protection",
                                subtitle = "Surah 67 • 30 Verses • Tabarak",
                                accentColor = Color(0xFF283593),
                                actionLabel = "Recite Al-Mulk",
                                onClick = {
                                    quranViewModel?.selectSurah(67, 1)
                                    onOpenSurahReader(67, 1)
                                }
                            )
                        }

                        // Shortcut 3: Surah Al-Kahf (Friday Sunnah)
                        item {
                            HighlightShortcutDeckCard(
                                title = "Surah Al-Kahf",
                                arabicTitle = "سُورَة الْكَهْف",
                                badgeText = "Friday Sunnah",
                                subtitle = "Surah 18 • 110 Verses • Luminescence",
                                accentColor = SanctuaryGoldWarm,
                                actionLabel = "Recite Al-Kahf",
                                onClick = {
                                    quranViewModel?.selectSurah(18, 1)
                                    onOpenSurahReader(18, 1)
                                }
                            )
                        }

                        // Shortcut 4: Ayat al-Kursi (The Throne Verse)
                        item {
                            HighlightShortcutDeckCard(
                                title = "Ayat al-Kursi",
                                arabicTitle = "آيَةُ الْكُرْسِي",
                                badgeText = "Greatest Verse",
                                subtitle = "Surah Al-Baqarah 2:255 • Protection",
                                accentColor = Color(0xFF8E24AA),
                                actionLabel = "Recite Ayah",
                                onClick = {
                                    quranViewModel?.selectSurah(2, 255)
                                    onOpenSurahReader(2, 255)
                                }
                            )
                        }

                        // Shortcut 5: Join Room with Passcode
                        item {
                            HighlightShortcutDeckCard(
                                title = "Join With Passcode",
                                arabicTitle = "انْضِمَام",
                                badgeText = "Online Khatm",
                                subtitle = "Enter room code from friends or family",
                                accentColor = OnlineGoldAccent,
                                actionLabel = "Join Room",
                                icon = Icons.Default.Groups,
                                onClick = onNavigateToDistributions
                            )
                        }

                        // Shortcut 6: Sacred Manuscript Index
                        item {
                            HighlightShortcutDeckCard(
                                title = "All 114 Surahs",
                                arabicTitle = "الْقُرْآنُ",
                                badgeText = "Manuscript",
                                subtitle = "Full index of 114 Surahs & 30 Paras",
                                accentColor = SanctuaryEmeraldDark,
                                actionLabel = "Open Index",
                                icon = Icons.AutoMirrored.Filled.MenuBook,
                                onClick = onNavigateToQuran
                            )
                        }
                    }
                }

                // 4. Horizontal Active Sessions Shelf (when user has active distributions)
                if (distributions.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = horizontalPad),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Active Recitation Circles",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "${distributions.size}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = "View All",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable(onClick = onNavigateToDistributions)
                            )
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = horizontalPad),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(distributions) { dist ->
                                val participants = participantsByDistribution[dist.id].orEmpty()
                                val completedCount = if (dist.type == DistributionType.KHATM) {
                                    participants.count { it.isCompletedForMonth(currentYear, currentMonth) }
                                } else {
                                    participants.count { it.isCompleted }
                                }
                                ActiveSessionShelfCard(
                                    dist = dist,
                                    participantsCount = participants.size,
                                    completedCount = completedCount,
                                    onClick = { onOpenDistributionDetail(dist.id) },
                                    onShare = {
                                        val summary = distributionViewModel.generateShareSummaryForDistribution(
                                            distribution = dist,
                                            participants = participants,
                                            targetYear = currentYear,
                                            targetMonth = currentMonth
                                        )
                                        if (summary.isNotBlank()) activeShareSummaryText = summary
                                    },
                                    modifier = Modifier.width(280.dp)
                                )
                            }
                        }
                    }
                }

                // Manuscript Divider for elegant breathing space
                ManuscriptOrnamentDivider(modifier = Modifier.padding(horizontal = horizontalPad))

                // Generous bottom spacer so the floating 3-button nav dock never overlaps content
                Spacer(modifier = Modifier.height(110.dp))
            }
        }
    }

    // Share summary dialog
    activeShareSummaryText?.let { textToShare ->
        ShareDistributionDialog(
            shareText = textToShare,
            onDismiss = { activeShareSummaryText = null }
        )
    }
}

/**
 * Daily Noor Top Banner: The illuminated green sanctuary header with rotating ayahs
 * and settings icon in top right, as specified in the reference sketch.
 */
@Composable
private fun DailyNoorTopBanner(
    hijriDate: String,
    gregorianDate: String,
    onOpenSettings: () -> Unit
) {
    val rotatingAyahs = remember {
        listOf(
            "وَرَتِّلِ الْقُرْآنَ تَرْتِيلًا" to "“And recite the Qur'an with measured recitation.” — Surah Al-Muzzammil (73:4)",
            "أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ" to "“Verily, in the remembrance of Allah do hearts find rest.” — Surah Ar-Ra'd (13:28)",
            "إِنَّ هَٰذَا الْقُرْآنَ يَهْدِي لِلَّتِي هِيَ أَقْوَمُ" to "“Indeed, this Qur'an guides to that which is most suitable.” — Surah Al-Isra (17:9)",
            "خَيْرُكُمْ مَنْ تَعَلَّمَ الْقُرْآنَ وَعَلَّمَهُ" to "“The best among you are those who learn the Qur'an and teach it.”"
        )
    }
    var currentAyahIndex by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(7000)
            currentAyahIndex = (currentAyahIndex + 1) % rotatingAyahs.size
        }
    }
    val currentAyah = rotatingAyahs[currentAyahIndex]

    Surface(
        color = SanctuaryEmeraldDark,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // Subtle warm golden radial halo at center top
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(
                                SanctuaryGoldHalo.copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.5f, 0f),
                            radius = size.width * 0.6f
                        )
                    )
                }
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Row: App Brand & Date on left, Settings button on right (per sketch)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(SanctuaryEmeraldMedium)
                                .border(1.2.dp, SanctuaryGoldWarm.copy(alpha = 0.9f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            SanctuaryGeometricGlyph(
                                size = 22.dp,
                                primaryColor = SanctuaryGoldBright,
                                accentColor = SanctuaryGoldWarm,
                                isQuranGlyph = true
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "Quran Baanto",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.4.sp
                                ),
                                color = SanctuaryGoldBright
                            )
                            Text(
                                text = "$hijriDate • $gregorianDate",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Settings Button in top right (per sketch)
                    Surface(
                        onClick = onOpenSettings,
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, SanctuaryGoldWarm.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("home_settings_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = SanctuaryGoldBright,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Rotating Ayah reflection banner inside the illuminated green header
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.18f),
                    border = BorderStroke(0.8.dp, SanctuaryGoldWarm.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = currentAyah.first,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = SanctuaryGoldBright,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = currentAyah.second,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp
                            ),
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}



/**
 * Deck Card 2: Live Streak Ember Card.
 */
@Composable
private fun StreakEmberDeckCard(
    streakDays: Int,
    ayahsRead: Int,
    targetAyahs: Int,
    isGoalMet: Boolean,
    onContinue: () -> Unit,
    onOpenStreaks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = (ayahsRead.toFloat() / targetAyahs.coerceAtLeast(1)).coerceIn(0f, 1f)

    Card(
        modifier = modifier
            .height(175.dp)
            .clickable(onClick = onOpenStreaks),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        border = BorderStroke(1.2.dp, Color(0xFFFF9800).copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$streakDays Day Streak",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isGoalMet) Color(0xFF2E7D32).copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = if (isGoalMet) "Met ✨" else "$ayahsRead/$targetAyahs",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isGoalMet) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column {
                Text(
                    text = if (isGoalMet) "Today's goal completed! Keep shining." else "${(targetAyahs - ayahsRead).coerceAtLeast(0)} Ayahs remaining today",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = if (isGoalMet) Color(0xFF2E7D32) else Color(0xFFFF9800),
                    trackColor = MaterialTheme.colorScheme.surface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    onClick = onContinue,
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Continue", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.White))
                    }
                }
            }
        }
    }
}

/**
 * Elegant Shortcut Deck Card in the horizontal Highlights Scroll Wheel.
 */
@Composable
private fun HighlightShortcutDeckCard(
    title: String,
    arabicTitle: String,
    badgeText: String,
    subtitle: String,
    accentColor: Color,
    actionLabel: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.PlayArrow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(260.dp)
            .height(175.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = arabicTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    onClick = onClick,
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = actionLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Prominent Continue From Last Bookmark Card, placed directly beneath the header banner
 * as sketched in the user's reference drawing.
 */
@Composable
private fun ContinueFromLastBookmarkCard(
    bookmark: com.example.data.quran.LastReadBookmark?,
    totalBookmarks: Int,
    onResume: () -> Unit,
    onStartQuran: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = if (bookmark != null) onResume else onStartQuran),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.2.dp, SanctuaryGoldWarm.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Open book illustration / emblem as drawn in user sketch
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(SanctuaryEmeraldMedium.copy(alpha = 0.16f))
                    .border(1.dp, SanctuaryGoldWarm.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = SanctuaryEmeraldMedium,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Continue from last bookmark",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = bookmark?.title ?: "Begin Your Recitation",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = bookmark?.subtitle ?: "Surah Al-Fatihah • Verse 1",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Resume/Read Button
            Surface(
                onClick = if (bookmark != null) onResume else onStartQuran,
                shape = RoundedCornerShape(12.dp),
                color = SanctuaryEmeraldMedium,
                border = BorderStroke(0.8.dp, SanctuaryGoldBright.copy(alpha = 0.6f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = SanctuaryGoldBright,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (bookmark != null) "Resume" else "Read",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = SanctuaryGoldBright
                        )
                    )
                }
            }
        }
    }
}

/**
 * Deck Card: Latest Khatm / User's Active Distribution Card.
 */
@Composable
private fun LatestKhatmDeckCard(
    distribution: DistributionEntity,
    assignedPara: Int,
    isAssignedCompleted: Boolean,
    onOpenDistribution: () -> Unit,
    onRecitePara: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(175.dp)
            .clickable(onClick = onOpenDistribution),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.2.dp,
            if (distribution.isOnline) OnlineGoldAccent.copy(alpha = 0.65f) else LocalBlueAccent.copy(alpha = 0.65f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (distribution.isOnline) OnlineGoldAccent.copy(alpha = 0.15f) else LocalBlueAccent.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (distribution.isOnline) "Active Cloud Khatm" else "Local Session",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (distribution.isOnline) OnlineGoldAccent else LocalBlueAccent
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = if (isAssignedCompleted) "Completed ✨" else "Portion Left",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isAssignedCompleted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                    )
                )
            }

            Column {
                Text(
                    text = distribution.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Your Assigned: Para $assignedPara",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    onClick = { onRecitePara(assignedPara) },
                    shape = RoundedCornerShape(8.dp),
                    color = SanctuaryEmeraldMedium
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = SanctuaryGoldBright,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Recite Para $assignedPara",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = SanctuaryGoldBright
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Deck Card 3: Saved Bookmark Spotlight Card.
 */
@Composable
private fun BookmarkSpotlightDeckCard(
    bookmark: com.example.data.quran.LastReadBookmark,
    totalBookmarks: Int,
    onResume: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(175.dp)
            .clickable(onClick = onResume),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.2.dp, SanctuaryGoldWarm.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Saved Bookmark",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "$totalBookmarks saved",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column {
                Text(
                    text = bookmark.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = bookmark.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    onClick = onResume,
                    shape = RoundedCornerShape(8.dp),
                    color = SanctuaryEmeraldMedium
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SanctuaryGoldBright, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resume", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = SanctuaryGoldBright))
                    }
                }
            }
        }
    }
}



/**
 * Active Session Shelf Card with circular progress arc and member count.
 */
@Composable
private fun ActiveSessionShelfCard(
    dist: DistributionEntity,
    participantsCount: Int,
    completedCount: Int,
    onClick: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCount = if (dist.type == DistributionType.AMAL) dist.totalTarget.coerceAtLeast(1) else 30
    val progress = (completedCount.toFloat() / totalCount).coerceIn(0f, 1f)

    Card(
        modifier = modifier
            .height(150.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.2.dp, if (dist.roomMode == com.example.data.db.RoomMode.ONLINE) OnlineGoldAccent.copy(alpha = 0.5f) else LocalBlueAccent.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dist.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$participantsCount Members • ${if (dist.type == DistributionType.AMAL) "Amal" else "Khatm"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Circular Progress Arc
                Box(
                    modifier = Modifier.size(38.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressArc(
                        progress = progress,
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 3.5.dp,
                        progressColor = if (dist.roomMode == com.example.data.db.RoomMode.ONLINE) OnlineGoldAccent else LocalBlueAccent
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$completedCount / $totalCount completed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    onClick = onShare,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
