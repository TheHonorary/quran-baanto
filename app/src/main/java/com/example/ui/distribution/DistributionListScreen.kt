package com.example.ui.distribution

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diversity1
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.LinearProgressIndicator
import com.example.data.db.DistributionEntity
import com.example.data.db.DistributionType
import com.example.data.db.ParticipantEntity
import com.example.data.db.RoomMode
import com.example.ui.components.ChiseledSanctuaryButton
import com.example.ui.components.CircularProgressArc
import com.example.ui.components.ManuscriptOrnamentDivider
import com.example.ui.components.PeacefulEmptyState
import com.example.ui.components.PrimarySanctuaryButton
import com.example.ui.components.ResizableSectionContainer
import com.example.ui.components.SanctuaryGeometricGlyph
import com.example.ui.components.SanctuaryLoadingState
import com.example.ui.components.SanctuaryTopHeader
import com.example.ui.components.SecondaryEditorialButton
import com.example.ui.theme.LocalBlueAccent
import com.example.ui.theme.LocalBlueGlow
import com.example.ui.theme.LocalBlueSurfaceDark
import com.example.ui.theme.LocalBlueSurfaceLight
import com.example.ui.theme.OnlineGoldAccent
import com.example.ui.theme.OnlineGoldGlow
import com.example.ui.theme.OnlineGoldSurfaceDark
import com.example.ui.theme.OnlineGoldSurfaceLight
import com.example.ui.theme.SanctuaryEmeraldDark
import com.example.ui.theme.SanctuaryEmeraldMedium
import com.example.ui.theme.SanctuaryGoldBright
import com.example.ui.theme.SanctuaryGoldWarm
import com.example.ui.viewmodel.DistributionViewModel
import com.example.util.KhatmNotificationHelper
import com.example.util.ServerSupportAdManager
import java.util.Calendar

private enum class DistributionCategoryFilter {
    ALL,
    QURAN,
    AMAL
}

@Composable
fun DistributionListScreen(
    distributionViewModel: DistributionViewModel,
    onCreateNewDistribution: (DistributionType, RoomMode) -> Unit,
    onOpenDistributionDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val distributions by distributionViewModel.distributions.collectAsState()
    val isLoading by distributionViewModel.isLoadingDistributions.collectAsState()
    val participantsByDistribution by distributionViewModel.participantsByDistribution.collectAsState()
    val userProfileName by distributionViewModel.userProfileName.collectAsState()
    val pendingOfflineSyncCount by distributionViewModel.pendingOfflineSyncCount.collectAsState()
    val isSyncingOfflineQueue by distributionViewModel.isSyncingOfflineQueue.collectAsState()
    val lastOfflineSyncMessage by distributionViewModel.lastOfflineSyncMessage.collectAsState()
    val shouldPromptNotificationOptIn by distributionViewModel.shouldPromptNotificationOptIn.collectAsState()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        distributionViewModel.completeNotificationOptInPrompt(enabled = isGranted)
        if (isGranted) {
            Toast.makeText(context, "Custom reading reminders enabled!", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(lastOfflineSyncMessage) {
        val msg = lastOfflineSyncMessage
        if (!msg.isNullOrBlank()) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            distributionViewModel.clearOfflineSyncMessage()
        }
    }

    var showTypePicker by remember { mutableStateOf(false) }
    var showJoinRoomDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(DistributionCategoryFilter.ALL) }
    var activeShareSummaryText by remember { mutableStateOf<String?>(null) }
    var activeShareDistribution by remember { mutableStateOf<DistributionEntity?>(null) }
    var qrCodeDistribution by remember { mutableStateOf<DistributionEntity?>(null) }
    var distributionPendingDelete by remember { mutableStateOf<DistributionEntity?>(null) }
    var adminDistributionId by remember { mutableStateOf<Long?>(null) }

    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val currentMonth = remember { Calendar.getInstance().get(Calendar.MONTH) + 1 }

    val filteredDistributions = remember(distributions, selectedFilter) {
        when (selectedFilter) {
            DistributionCategoryFilter.ALL -> distributions
            DistributionCategoryFilter.QURAN -> distributions.filter {
                it.type == DistributionType.KHATM || it.type == DistributionType.ONE_TIME_KHATM
            }
            DistributionCategoryFilter.AMAL -> distributions.filter {
                it.type == DistributionType.AMAL
            }
        }
    }

    val quranCount = remember(distributions) {
        distributions.count { it.type == DistributionType.KHATM || it.type == DistributionType.ONE_TIME_KHATM }
    }
    val amalCount = remember(distributions) {
        distributions.count { it.type == DistributionType.AMAL }
    }

    Scaffold(
        topBar = {
            SanctuaryTopHeader(
                title = "Distributions",
                customLeadingIcon = Icons.Filled.Groups,
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = { showTypePicker = true },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                            modifier = Modifier.testTag("top_new_session_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "New Session",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "New Session",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Surface(
                            onClick = { showJoinRoomDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            border = BorderStroke(0.9.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.testTag("top_join_room_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = "Join Online Room",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Join",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val horizontalPad = if (maxWidth >= 640.dp) 28.dp else 20.dp

            LazyColumn(
                contentPadding = PaddingValues(
                    start = horizontalPad,
                    end = horizontalPad,
                    top = 18.dp,
                    bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("distributions_list")
            ) {
                // 1. Segmented Mode Switcher Container matching QuranListScreen style
                item(key = "distribution_mode_switcher") {
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
                            DistributionTabSegment(
                                title = "All (${distributions.size})",
                                selected = selectedFilter == DistributionCategoryFilter.ALL,
                                onClick = { selectedFilter = DistributionCategoryFilter.ALL },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("filter_all")
                            )
                            DistributionTabSegment(
                                title = "Qur'an ($quranCount)",
                                selected = selectedFilter == DistributionCategoryFilter.QURAN,
                                onClick = { selectedFilter = DistributionCategoryFilter.QURAN },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("filter_khatm")
                            )
                            DistributionTabSegment(
                                title = "Amal ($amalCount)",
                                selected = selectedFilter == DistributionCategoryFilter.AMAL,
                                onClick = { selectedFilter = DistributionCategoryFilter.AMAL },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("filter_amal")
                            )
                        }
                    }
                }

                // 2. Collective Progress Orbit (shows aggregated completion across circles)
                if (distributions.isNotEmpty()) {
                    val totalPortions = distributions.sumOf { it.totalTarget }
                    val completedPortions = distributions.sumOf { dist ->
                        val pList = participantsByDistribution[dist.id].orEmpty()
                        if (dist.type == DistributionType.KHATM) {
                            pList.count { it.isCompletedForMonth(currentYear, currentMonth) }
                        } else {
                            pList.count { it.isCompleted }
                        }
                    }
                    val orbitProgress = if (totalPortions > 0) (completedPortions.toFloat() / totalPortions).coerceIn(0f, 1f) else 0f
                    val orbitPct = (orbitProgress * 100).toInt()

                    item(key = "collective_progress_orbit") {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = SanctuaryEmeraldDark,
                            border = BorderStroke(1.4.dp, SanctuaryGoldWarm.copy(alpha = 0.85f)),
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("collective_progress_orbit_card")
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                SanctuaryGoldWarm.copy(alpha = 0.16f),
                                                Color.Transparent
                                            ),
                                            radius = 450f
                                        )
                                    )
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.size(76.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressArc(
                                            progress = orbitProgress,
                                            strokeWidth = 9.dp,
                                            progressColor = SanctuaryGoldBright,
                                            trackColor = SanctuaryEmeraldMedium.copy(alpha = 0.35f),
                                            modifier = Modifier.size(76.dp)
                                        )
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "$orbitPct%",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = SanctuaryGoldBright,
                                                    fontSize = 16.sp
                                                )
                                            )
                                            Text(
                                                text = "COMPLETED",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White.copy(alpha = 0.7f),
                                                    fontSize = 7.5.sp,
                                                    letterSpacing = 0.5.sp
                                                )
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "COLLECTIVE ORBIT",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.1.sp,
                                                    fontSize = 10.sp
                                                ),
                                                color = SanctuaryGoldBright
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = SanctuaryGoldWarm.copy(alpha = 0.25f)
                                            ) {
                                                Text(
                                                    text = "${distributions.size} Circles",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 9.sp
                                                    ),
                                                    color = SanctuaryGoldBright,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = "$completedPortions of $totalPortions portions recited",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 15.sp
                                            )
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = if (orbitProgress >= 1f) "All shared Khatms completed! BarakAllahu feekum."
                                            else "Every recited ayah joins the shared tapestry of remembrance.",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color.White.copy(alpha = 0.75f),
                                                fontSize = 11.5.sp
                                            ),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (pendingOfflineSyncCount > 0) {
                    item(key = "offline_sync_queue_banner") {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.2.dp, OnlineGoldAccent.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Offline Sync Queue ($pendingOfflineSyncCount pending)",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Saved safely on device. Will sync automatically when online.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Surface(
                                    onClick = { distributionViewModel.syncOfflineChangesNow() },
                                    shape = RoundedCornerShape(10.dp),
                                    color = OnlineGoldAccent.copy(alpha = 0.18f),
                                    border = BorderStroke(1.dp, OnlineGoldAccent.copy(alpha = 0.55f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Sync Offline Changes",
                                            tint = OnlineGoldAccent,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = if (isSyncingOfflineQueue) "Syncing..." else "Sync Now",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Loading State, Empty State, or Session List
                if (isLoading && distributions.isEmpty()) {
                    item(key = "loading_distributions_state") {
                        SanctuaryLoadingState(
                            message = "Loading your distribution sessions..."
                        )
                    }
                } else if (distributions.isEmpty()) {
                    item(key = "empty_distributions_state") {
                        PeacefulEmptyState(
                            icon = Icons.Default.Diversity1,
                            title = "No Distribution Sessions Yet",
                            description = "Create an Online Room with a passcode or a Local Session for Monthly Khatm, One-Time Qur'an Khatm, or Custom Amal.",
                            actionText = "Create Session",
                            onActionClick = { showTypePicker = true }
                        )
                    }
                } else if (filteredDistributions.isEmpty()) {
                    item(key = "empty_filtered_distributions_state") {
                        val isAmalFilter = selectedFilter == DistributionCategoryFilter.AMAL
                        PeacefulEmptyState(
                            icon = Icons.Default.Diversity1,
                            title = if (isAmalFilter) "No Custom Amal Sessions" else "No Qur'an Khatm Sessions",
                            description = if (isAmalFilter) {
                                "Start an Online or Local target recitation circle for Surah Yaseen, Salawat, or any group Amal."
                            } else {
                                "Start a Monthly Rotating Khatm or a One-Time 30-Para Qur'an Khatm for your family or group."
                            },
                            actionText = if (isAmalFilter) "New Amal Session" else "New Khatm Session",
                            onActionClick = { showTypePicker = true }
                        )
                    }
                } else {
                    // Section Header following QuranListScreen & HomeScreen editorial style
                    item(key = "active_sessions_section_header") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (selectedFilter) {
                                        DistributionCategoryFilter.QURAN -> "Qur'an Khatm Sessions"
                                        DistributionCategoryFilter.AMAL -> "Custom Amal Sessions"
                                        DistributionCategoryFilter.ALL -> "Active Sessions"
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                    border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = "${filteredDistributions.size} ${if (filteredDistributions.size == 1) "Session" else "Sessions"}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
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

                    item(key = "distributions_swipe_pager") {
                        val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { filteredDistributions.size })

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            androidx.compose.foundation.pager.HorizontalPager(
                                state = pagerState,
                                contentPadding = PaddingValues(end = if (filteredDistributions.size > 1) 32.dp else 0.dp),
                                pageSpacing = 16.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(440.dp)
                            ) { page ->
                                val dist = filteredDistributions[page]
                                val distParticipants = participantsByDistribution[dist.id].orEmpty()

                                DistributionCompressedSwipeCard(
                                    distribution = dist,
                                    participants = distParticipants,
                                    userProfileName = userProfileName,
                                    currentYear = currentYear,
                                    currentMonth = currentMonth,
                                    onClick = { onOpenDistributionDetail(dist.id) },
                                    onDeleteClick = { distributionPendingDelete = dist },
                                    onShareClick = {
                                        val summary = distributionViewModel.generateShareSummaryForDistribution(
                                            distribution = dist,
                                            participants = distParticipants,
                                            targetYear = currentYear,
                                            targetMonth = currentMonth
                                        )
                                        if (summary.isNotBlank()) {
                                            activeShareDistribution = dist
                                            activeShareSummaryText = summary
                                        }
                                    },
                                    onShowQrClick = if (dist.isOnline && !dist.roomPasscode.isNullOrBlank()) {
                                        { qrCodeDistribution = dist }
                                    } else null,
                                    onOpenAdmin = { adminDistributionId = dist.id },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Dot Page Indicator
                            if (filteredDistributions.size > 1) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(filteredDistributions.size) { iteration ->
                                        val isSelected = pagerState.currentPage == iteration
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 4.dp)
                                                .size(if (isSelected) 10.dp else 6.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.outlineVariant
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item(key = "distributions_footer_ornament") {
                        ManuscriptOrnamentDivider(
                            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                        )
                    }
                }
            }
        }
    }

    if (showTypePicker) {
        DistributionModePickerDialog(
            onSelectTypeAndMode = { type, roomMode ->
                showTypePicker = false
                onCreateNewDistribution(type, roomMode)
            },
            onOpenJoinRoom = {
                showTypePicker = false
                showJoinRoomDialog = true
            },
            onDismiss = { showTypePicker = false }
        )
    }

    if (showJoinRoomDialog) {
        JoinOnlineRoomDialog(
            initialUserName = userProfileName,
            onJoin = { passcodeInput, customRoomName, userName ->
                distributionViewModel.joinRoomByPasscode(
                    rawInput = passcodeInput,
                    customRoomTitle = customRoomName,
                    userNameToSave = userName
                ) { joinedId ->
                    if (joinedId != null) {
                        showJoinRoomDialog = false
                        onOpenDistributionDetail(joinedId)
                    } else {
                        Toast.makeText(
                            context,
                            "Please enter a valid Room Passcode (e.g. QBO-7K4M)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            onDismiss = { showJoinRoomDialog = false }
        )
    }

    activeShareSummaryText?.let { summaryText ->
        val shareDist = activeShareDistribution
        ShareDistributionDialog(
            shareText = summaryText,
            roomPasscode = if (shareDist?.isOnline == true) shareDist.roomPasscode else null,
            onShowQrCode = if (shareDist?.isOnline == true && !shareDist.roomPasscode.isNullOrBlank()) {
                { qrCodeDistribution = shareDist }
            } else null,
            onDismiss = {
                activeShareSummaryText = null
                activeShareDistribution = null
            }
        )
    }

    qrCodeDistribution?.let { qrDist ->
        val passcode = qrDist.roomPasscode
        if (!passcode.isNullOrBlank()) {
            val typeLabel = when (qrDist.type) {
                DistributionType.KHATM -> "Online Monthly 30-Para Khatm"
                DistributionType.ONE_TIME_KHATM -> "Online One-Time 30-Para Khatm"
                DistributionType.AMAL -> "Online Custom Amal (${qrDist.totalTarget} Target)"
            }
            RoomQrCodeDialog(
                roomName = qrDist.name,
                passcode = passcode,
                typeSubtitle = typeLabel,
                onDismiss = { qrCodeDistribution = null }
            )
        }
    }

    distributionPendingDelete?.let { distToDelete ->
        DeleteSessionConfirmationDialog(
            distributionName = distToDelete.name,
            onConfirmDelete = {
                distributionViewModel.deleteDistribution(distToDelete.id)
                distributionPendingDelete = null
            },
            onDismiss = { distributionPendingDelete = null }
        )
    }

    if (shouldPromptNotificationOptIn) {
        NotificationPermissionOptInDialog(
            isOnlineRoomContext = true,
            onEnableReminders = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !KhatmNotificationHelper.hasNotificationPermission(context)
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    distributionViewModel.completeNotificationOptInPrompt(enabled = true)
                    Toast.makeText(context, "Custom reading reminders enabled!", Toast.LENGTH_SHORT).show()
                }
            },
            onNotNow = {
                distributionViewModel.dismissNotificationOptInPrompt()
            }
        )
    }

    val showServerSupportPrompt by ServerSupportAdManager.showServerSupportPrompt.collectAsState()
    val isAdPlaying by ServerSupportAdManager.isAdPlaying.collectAsState()
    val adCountdownSeconds by ServerSupportAdManager.adCountdownSeconds.collectAsState()
    val showGratitudeDialog by ServerSupportAdManager.showGratitudeDialog.collectAsState()
    val pendingRoomName by ServerSupportAdManager.pendingRoomName.collectAsState()
    val isRoomCreation by ServerSupportAdManager.isRoomCreation.collectAsState()
    val adScope = rememberCoroutineScope()

    if (showServerSupportPrompt) {
        ServerSupportPromptDialog(
            roomName = pendingRoomName,
            isRoomCreation = isRoomCreation,
            onAccept = {
                ServerSupportAdManager.acceptVoluntaryAd(context, adScope)
            },
            onDecline = {
                ServerSupportAdManager.dismissPrompt()
            }
        )
    }

    if (isAdPlaying) {
        ServerSupportAdPlayerDialog(
            remainingSeconds = adCountdownSeconds,
            onCloseEarly = {
                ServerSupportAdManager.closeAdPlayer()
            }
        )
    }

    if (showGratitudeDialog) {
        ServerSupportGratitudeDialog(
            onDismiss = {
                ServerSupportAdManager.dismissGratitude()
            }
        )
    }

    adminDistributionId?.let { adminId ->
        Dialog(
            onDismissRequest = { adminDistributionId = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            DistributionAdminScreen(
                distributionId = adminId,
                distributionViewModel = distributionViewModel,
                onBack = { adminDistributionId = null }
            )
        }
    }
}

@Composable
private fun DistributionTabSegment(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = bg,
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 12.sp
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun JoinOnlineRoomDialog(
    initialUserName: String,
    onJoin: (passcodeInput: String, customRoomTitle: String?, userName: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var passcodeText by remember { mutableStateOf("") }
    var userNameText by remember(initialUserName) { mutableStateOf(initialUserName) }
    var hasAttemptedJoin by remember { mutableStateOf(false) }

    val isCirclePasscode = passcodeText.trim().uppercase().startsWith("CRCL-") || passcodeText.trim().uppercase().startsWith("STRK-")
    val isPasscodeError = (hasAttemptedJoin && passcodeText.trim().length < 4) || isCirclePasscode

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.2.dp, OnlineGoldAccent.copy(alpha = 0.5f)),
            tonalElevation = 6.dp,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .widthIn(max = 480.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        SanctuaryGeometricGlyph(
                            isQuranGlyph = false,
                            size = 44.dp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Join Online Room",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Enter a room passcode or paste an invite",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                ResizableSectionContainer(
                    contentPadding = PaddingValues(18.dp)
                ) {
                    OutlinedTextField(
                        value = passcodeText,
                        onValueChange = {
                            passcodeText = it
                            if (it.trim().length >= 4) hasAttemptedJoin = false
                        },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Room Passcode")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("*", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.ExtraBold)
                            }
                        },
                        placeholder = { Text("e.g. QBO-7K4M (Compulsory)") },
                        isError = isPasscodeError,
                        supportingText = {
                            if (isCirclePasscode) {
                                Text(
                                    text = "This passcode belongs to a Streak Circle. Please go to the Streaks & Plans tab to join.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                            } else if (isPasscodeError) {
                                Text(
                                    text = "Room Passcode is compulsory to join (e.g. QBO-7K4M)",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                            } else {
                                Text(
                                    text = "Enter the Khatm passcode shared by the room host",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        trailingIcon = {
                            if (isPasscodeError) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Passcode required",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            } else if (passcodeText.trim().length >= 4) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Valid passcode format",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        singleLine = false,
                        maxLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OnlineGoldAccent,
                            unfocusedBorderColor = if (isPasscodeError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
                            errorBorderColor = MaterialTheme.colorScheme.error,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = userNameText,
                        onValueChange = { userNameText = it },
                        label = { Text("Your Name (Optional)") },
                        placeholder = { Text("e.g. Hamza") },
                        supportingText = {
                            Text(
                                text = "Saved for your claimed Paras or recitations",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SecondaryEditorialButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    PrimarySanctuaryButton(
                        text = "Join Room",
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        onClick = {
                            if (isCirclePasscode) {
                                Toast.makeText(
                                    context,
                                    "This code is for a Streak Circle. Please go to the Streaks & Plans tab to join.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else if (passcodeText.trim().length < 4) {
                                hasAttemptedJoin = true
                                Toast.makeText(
                                    context,
                                    "Please enter a valid Room Passcode (e.g. QBO-7K4M)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                onJoin(
                                    passcodeText.trim(),
                                    null,
                                    userNameText.trim()
                                )
                            }
                        },
                        modifier = Modifier.weight(1.25f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteSessionConfirmationDialog(
    distributionName: String,
    onConfirmDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Delete \"$distributionName\"?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "This will permanently remove this session and all member allocations from your device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SecondaryEditorialButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = onConfirmDelete,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Clean, Spacious 2-Step Pop-Up Form Wizard:
 * - Step 1 of 2: Choose Online Room (Gold Glow) vs Local Room (Blue Glow)
 * - Step 2 of 2: Choose Distribution Type (Monthly Khatm, One-Time Khatm, or Custom Amal)
 * Includes a 2-segment progress bar at the top and gives every option generous breathing room.
 */
@Composable
fun DistributionModePickerDialog(
    onSelectTypeAndMode: (DistributionType, RoomMode) -> Unit,
    onOpenJoinRoom: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) }
    var selectedRoomMode by remember { mutableStateOf(RoomMode.ONLINE) }
    var highlightedType by remember { mutableStateOf(DistributionType.KHATM) }
    val stepAccent = if (selectedRoomMode == RoomMode.ONLINE) OnlineGoldAccent else LocalBlueAccent

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.3.dp, stepAccent.copy(alpha = 0.55f)),
            tonalElevation = 8.dp,
            shadowElevation = 22.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 26.dp, vertical = 26.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                // Top Header + Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (currentStep == 1) "STEP 1 OF 2 • CHOOSE ROOM STYLE" else "STEP 2 OF 2 • SELECT FORMAT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.9.sp,
                                fontSize = 11.sp
                            ),
                            color = stepAccent
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (currentStep == 1) "How Would You Like to Setup?" else "Choose Distribution Format",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // 2-Step Visual Progress Bar with generous spacing
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(stepAccent)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (currentStep >= 2) stepAccent
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "1. Room Mode",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (currentStep == 1) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (currentStep == 1) stepAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "2. Distribution Type",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (currentStep == 2) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (currentStep == 2) stepAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Animated 2-Mini-Page Content with generous vertical spacing so cards never feel smudged
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "create_distribution_wizard_step"
                ) { step ->
                    if (step == 1) {
                        // MINI-PAGE 1: Local or Online Room
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            RoomModeStepCard(
                                title = "Online Room",
                                badge = "GOLD GLOW • LIVE PASSCODE",
                                description = "Create an instant online room with a shareable Passcode & QR code. Friends and family join from their own phones, claim their own Para or Amal, and check off their progress.",
                                icon = Icons.Default.Public,
                                glowAccent = OnlineGoldAccent,
                                isSelected = selectedRoomMode == RoomMode.ONLINE,
                                onClick = {
                                    selectedRoomMode = RoomMode.ONLINE
                                    currentStep = 2
                                }
                            )

                            RoomModeStepCard(
                                title = "Local Room",
                                badge = "BLUE GLOW • NAME MEMBERS NOW",
                                description = "Set up the complete roster right on your device. You name each person and assign their Paras or Amal counts during setup, then share the clean schedule anytime.",
                                icon = Icons.Default.Smartphone,
                                glowAccent = LocalBlueAccent,
                                isSelected = selectedRoomMode == RoomMode.LOCAL,
                                onClick = {
                                    selectedRoomMode = RoomMode.LOCAL
                                    currentStep = 2
                                }
                            )

                            if (onOpenJoinRoom != null) {
                                Surface(
                                    onClick = onOpenJoinRoom,
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 18.dp, vertical = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(OnlineGoldAccent.copy(alpha = 0.16f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Key,
                                                    contentDescription = null,
                                                    tint = OnlineGoldAccent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column {
                                                Text(
                                                    text = "Already have a room passcode?",
                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Tap to join an existing online room",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = OnlineGoldAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // MINI-PAGE 2: Select Distribution Type
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = stepAccent.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, stepAccent.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (selectedRoomMode == RoomMode.ONLINE) Icons.Default.Public else Icons.Default.Smartphone,
                                        contentDescription = null,
                                        tint = stepAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (selectedRoomMode == RoomMode.ONLINE) {
                                            "Selected: Online Room (Gold Glow • Passcode Sync)"
                                        } else {
                                            "Selected: Local Room (Blue Glow • Name Roster at Setup)"
                                        },
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            ModeSpecimenOptionCard(
                                title = "Monthly Qur'an Khatm",
                                description = if (selectedRoomMode == RoomMode.LOCAL) {
                                    "Name each reader now to allocate all 30 Paras. Automatically rotates everyone's Paras each new month and alerts you when ready to share."
                                } else {
                                    "Divides 30 Paras into contiguous blocks that rotate automatically every month as members join."
                                },
                                isQuranGlyph = true,
                                isSelected = highlightedType == DistributionType.KHATM,
                                accentColor = stepAccent,
                                onSelectCard = { highlightedType = DistributionType.KHATM }
                            )

                            ModeSpecimenOptionCard(
                                title = "One-Time Qur'an Khatm",
                                description = if (selectedRoomMode == RoomMode.LOCAL) {
                                    "Name each reader at setup to assign all 30 Paras for a single complete Khatm."
                                } else {
                                    "Open all 30 Paras so members can claim specific Paras first-come, first-served with a passcode."
                                },
                                isQuranGlyph = true,
                                isSelected = highlightedType == DistributionType.ONE_TIME_KHATM,
                                accentColor = stepAccent,
                                onSelectCard = { highlightedType = DistributionType.ONE_TIME_KHATM }
                            )

                            ModeSpecimenOptionCard(
                                title = "Custom Amal",
                                description = if (selectedRoomMode == RoomMode.LOCAL) {
                                    "Set a target count (e.g. 110 Yaseen or 1000 Salawat) and name each person's share during setup."
                                } else {
                                    "Create a live target count for Surah Yaseen, Salawat, or Tasbih where members pledge and use the tactile counter."
                                },
                                isQuranGlyph = false,
                                isSelected = highlightedType == DistributionType.AMAL,
                                accentColor = stepAccent,
                                onSelectCard = { highlightedType = DistributionType.AMAL }
                            )
                        }
                    }
                }

                ManuscriptOrnamentDivider(modifier = Modifier.padding(vertical = 2.dp))

                // Bottom Navigation Buttons for Step 1 & Step 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentStep == 1) {
                        SecondaryEditorialButton(
                            text = "Cancel",
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        )
                        PrimarySanctuaryButton(
                            text = "Next Step",
                            icon = Icons.AutoMirrored.Filled.ArrowForward,
                            onClick = { currentStep = 2 },
                            modifier = Modifier.weight(1.3f)
                        )
                    } else {
                        SecondaryEditorialButton(
                            text = "Back",
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            onClick = { currentStep = 1 },
                            modifier = Modifier.weight(1f)
                        )
                        PrimarySanctuaryButton(
                            text = if (selectedRoomMode == RoomMode.LOCAL) "Name Members" else "Setup Room",
                            icon = Icons.AutoMirrored.Filled.ArrowForward,
                            onClick = { onSelectTypeAndMode(highlightedType, selectedRoomMode) },
                            modifier = Modifier.weight(1.35f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomModeStepCard(
    title: String,
    badge: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    glowAccent: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.2.dp,
            color = if (isSelected) glowAccent else glowAccent.copy(alpha = 0.38f)
        ),
        shadowElevation = if (isSelected) 10.dp else 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isSelected) 12.dp else 4.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = glowAccent,
                spotColor = glowAccent
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            glowAccent.copy(alpha = if (isSelected) 0.14f else 0.06f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 22.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(glowAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = glowAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = glowAccent.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.5.sp
                                ),
                                color = glowAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = glowAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ModeSpecimenOptionCard(
    title: String,
    description: String,
    isQuranGlyph: Boolean,
    isSelected: Boolean,
    accentColor: Color,
    onSelectCard: () -> Unit
) {
    Surface(
        onClick = onSelectCard,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.1.dp,
            color = if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant
        ),
        shadowElevation = if (isSelected) 6.dp else 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isSelected) accentColor.copy(alpha = 0.08f) else Color.Transparent
                )
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SanctuaryGeometricGlyph(
                isQuranGlyph = isQuranGlyph,
                size = 48.dp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 19.sp
                )
            }

            if (isSelected) {
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Large "compressed look" swipe card for existing distributions,
 * matching the user's hand-drawn reference sketch (Image 1).
 */
@Composable
private fun DistributionCompressedSwipeCard(
    distribution: DistributionEntity,
    participants: List<ParticipantEntity>,
    userProfileName: String,
    currentYear: Int,
    currentMonth: Int,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onShareClick: () -> Unit,
    onShowQrClick: (() -> Unit)? = null,
    onOpenAdmin: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isOnline = distribution.isOnline
    val isDark = isSystemInDarkTheme()
    val glowColor = if (isOnline) OnlineGoldGlow else LocalBlueGlow
    val accentColor = if (isOnline) OnlineGoldAccent else LocalBlueAccent
    val glowSurfaceTint = if (isOnline) {
        if (isDark) OnlineGoldSurfaceDark else OnlineGoldSurfaceLight
    } else {
        if (isDark) LocalBlueSurfaceDark else LocalBlueSurfaceLight
    }

    val isHost = !isOnline || distribution.roomPasscode != null ||
        participants.any { userProfileName.isNotBlank() && (it.name.equals(userProfileName, ignoreCase = true) || it.addedByProfileName.equals(userProfileName, ignoreCase = true)) }

    val memberCount = participants.size
    val userPart = participants.firstOrNull {
        (userProfileName.isNotBlank() && (it.name.equals(userProfileName, ignoreCase = true) || it.addedByProfileName.equals(userProfileName, ignoreCase = true))) ||
        it.isPinned
    }

    val yourParaLabel = when {
        distribution.type == DistributionType.ONE_TIME_KHATM -> {
            if (userPart?.specificParaNumber != null) "Para ${userPart.specificParaNumber}"
            else if (userPart != null) "Claimed"
            else "Not Claimed"
        }
        distribution.type == DistributionType.KHATM -> {
            if (userPart != null) "${userPart.allocatedCount} Para(s)"
            else "Not Joined"
        }
        else -> {
            if (userPart != null) "${userPart.completedCount}/${userPart.allocatedCount} Recited"
            else "Not Joined"
        }
    }

    val completedCount = if (distribution.type == DistributionType.KHATM) {
        participants.count { it.isCompletedForMonth(currentYear, currentMonth) }
    } else {
        participants.count { it.isCompleted }
    }
    val progress = if (distribution.totalTarget > 0) (completedCount.toFloat() / distribution.totalTarget).coerceIn(0f, 1f) else 0f

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, accentColor),
        tonalElevation = 6.dp,
        shadowElevation = 14.dp,
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = glowColor,
                spotColor = glowColor
            )
            .testTag("distribution_compressed_card_${distribution.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            glowSurfaceTint.copy(alpha = if (isDark) 0.55f else 0.75f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(22.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top: Title, Mode Pill, and Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = distribution.name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                letterSpacing = 0.3.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (distribution.type) {
                                DistributionType.KHATM -> "Monthly Rotating Khatm"
                                DistributionType.ONE_TIME_KHATM -> "One-Time 30-Para Khatm"
                                DistributionType.AMAL -> "Custom Target Amal"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Mode Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = accentColor.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isOnline) "Cloud • ${distribution.roomPasscode.orEmpty()}" else "Local",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = accentColor
                            )
                        }
                    }
                }

                // Center: Big "16 Members" as drawn by user
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$memberCount",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 54.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (memberCount == 1) "Member" else "Members",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(8.dp)
                            .clip(CircleShape),
                        color = accentColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$completedCount of ${distribution.totalTarget} portions completed (${(progress * 100).toInt()}%)",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Bottom Row: "Your Para:" on left, "Admin Powers" badge on right (as sketched in Image 1)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Your Para:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = yourParaLabel,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = accentColor
                            )
                        )
                    }

                    if (isHost && onOpenAdmin != null) {
                        Surface(
                            onClick = onOpenAdmin,
                            shape = RoundedCornerShape(12.dp),
                            color = accentColor.copy(alpha = 0.18f),
                            border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.6f)),
                            modifier = Modifier.testTag("admin_powers_button_${distribution.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "💪",
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Admin Powers",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = accentColor
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
private fun DistributionSpecimenCard(
    distribution: DistributionEntity,
    onOpenClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onShareClick: () -> Unit,
    onShowQrClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isAmal = distribution.type == DistributionType.AMAL
    val isOnline = distribution.isOnline
    val isDark = isSystemInDarkTheme()

    val glowColor = if (isOnline) OnlineGoldGlow else LocalBlueGlow
    val accentColor = if (isOnline) OnlineGoldAccent else LocalBlueAccent
    val glowSurfaceTint = when {
        isOnline && isDark -> OnlineGoldSurfaceDark
        isOnline && !isDark -> OnlineGoldSurfaceLight
        !isOnline && isDark -> LocalBlueSurfaceDark
        else -> LocalBlueSurfaceLight
    }
    val compactButtonPadding = PaddingValues(horizontal = 10.dp, vertical = 11.dp)

    val typeBadgeLabel = when (distribution.type) {
        DistributionType.KHATM -> "MONTHLY KHATM"
        DistributionType.ONE_TIME_KHATM -> "ONE-TIME KHATM"
        DistributionType.AMAL -> "CUSTOM AMAL"
    }

    Surface(
        onClick = onOpenClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.6.dp,
            color = accentColor.copy(alpha = 0.68f)
        ),
        tonalElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = glowColor,
                spotColor = glowColor
            )
            .testTag("distribution_card_${distribution.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            glowSurfaceTint.copy(alpha = if (isDark) 0.55f else 0.72f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            // Luminous top visual signature bar: Gold Glow for Online vs Blue Glow for Local
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                glowColor,
                                accentColor,
                                glowColor.copy(alpha = 0.55f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Distinct visual emblem with Gold Glow (Online) vs Blue Glow (Local)
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = accentColor.copy(alpha = 0.16f),
                                border = BorderStroke(
                                    width = 1.2.dp,
                                    color = accentColor.copy(alpha = 0.65f)
                                ),
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    SanctuaryGeometricGlyph(
                                        isQuranGlyph = !isAmal,
                                        size = 36.dp
                                    )
                                }
                            }

                            // Small corner mode badge (Gold Globe for Online vs Blue Phone for Local)
                            Surface(
                                shape = CircleShape,
                                color = accentColor,
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
                                modifier = Modifier.size(20.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isOnline) Icons.Default.Public else Icons.Default.Smartphone,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(15.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(7.dp),
                                    color = accentColor.copy(alpha = 0.16f),
                                    border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.45f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(accentColor)
                                        )
                                        Text(
                                            text = if (isOnline && !distribution.roomPasscode.isNullOrBlank()) {
                                                "ONLINE • ${distribution.roomPasscode}"
                                            } else {
                                                "LOCAL • $typeBadgeLabel"
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                letterSpacing = 0.4.sp
                                            ),
                                            color = accentColor
                                        )
                                    }
                                }

                                if (isOnline && distribution.hasPendingSync) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = OnlineGoldAccent.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "OFFLINE SAVED",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            ),
                                            color = OnlineGoldAccent,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                if (distribution.isClosedForever) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = "CLOSED",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(5.dp))

                            Text(
                                text = distribution.name,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (onShowQrClick != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            onClick = onShowQrClick,
                            shape = RoundedCornerShape(12.dp),
                            color = accentColor.copy(alpha = 0.16f),
                            border = BorderStroke(0.9.dp, accentColor.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("qr_distribution_card_button_${distribution.id}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = "Show Room QR Code",
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PrimarySanctuaryButton(
                        text = "Open",
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        onClick = onOpenClick,
                        contentPadding = compactButtonPadding,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_distribution_button_${distribution.id}")
                    )

                    SecondaryEditorialButton(
                        text = "Delete",
                        icon = Icons.Default.Delete,
                        accentColor = MaterialTheme.colorScheme.error,
                        onClick = onDeleteClick,
                        contentPadding = compactButtonPadding,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("delete_distribution_card_button_${distribution.id}")
                    )

                    SecondaryEditorialButton(
                        text = "Share",
                        icon = Icons.Default.Share,
                        onClick = onShareClick,
                        contentPadding = compactButtonPadding,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_distribution_button_${distribution.id}")
                    )
                }
            }
        }
    }
}
