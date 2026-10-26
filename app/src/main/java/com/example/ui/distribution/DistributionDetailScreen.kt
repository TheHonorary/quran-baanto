package com.example.ui.distribution

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.db.DistributionType
import com.example.data.db.ParticipantEntity
import com.example.domain.ParaAssignment
import com.example.domain.RotationEngine
import com.example.ui.components.ContiguousParaBlockBar
import com.example.ui.components.OctagonalNumberMedallion
import com.example.ui.components.PrimarySanctuaryButton
import com.example.ui.components.ResizableSectionContainer
import com.example.ui.components.SanctuaryGeometricGlyph
import com.example.ui.components.SanctuaryLoadingState
import com.example.ui.components.SanctuaryTopHeader
import com.example.ui.components.SecondaryEditorialButton
import com.example.ui.components.rememberAllocationPalette
import com.example.ui.theme.LocalBlueAccent
import com.example.ui.theme.OnlineGoldAccent
import com.example.ui.viewmodel.DistributionViewModel
import com.example.util.KhatmNotificationHelper
import com.example.util.MonthDateInfo
import com.example.util.ServerSupportAdManager
import java.util.Calendar

private enum class OneTimeParaFilter {
    ALL,
    AVAILABLE,
    CLAIMED
}

@Composable
fun DistributionDetailScreen(
    distributionId: Long,
    distributionViewModel: DistributionViewModel,
    onEditDistribution: (Long) -> Unit,
    onOpenParaReader: (Int) -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeState by distributionViewModel.activeDistributionState.collectAsState()
    val savedProfileName by distributionViewModel.userProfileName.collectAsState()
    val celebrationMessage by distributionViewModel.celebrationMessage.collectAsState()
    val reopenedPasscode by distributionViewModel.reopenedRoomPasscode.collectAsState()
    val shouldPromptNotificationOptIn by distributionViewModel.shouldPromptNotificationOptIn.collectAsState()

    var showShareDialog by remember { mutableStateOf(false) }
    var showQrCodeDialog by remember { mutableStateOf(false) }
    var showDueReminderDialog by remember { mutableStateOf(false) }
    var activeDueScheduleLabel by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showOneTimeClaimDialogForPara by remember { mutableStateOf<Int?>(null) }
    var showOneTimeClaimModal by remember { mutableStateOf(false) }
    var showJoinSlotDialog by remember { mutableStateOf(false) }
    var showReopenAmalDialog by remember { mutableStateOf(false) }
    var oneTimeParaFilter by remember { mutableStateOf(OneTimeParaFilter.ALL) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        distributionViewModel.completeNotificationOptInPrompt(enabled = granted)
    }

    fun ensureNotificationPermissionRequested() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !KhatmNotificationHelper.hasNotificationPermission(context)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(distributionId) {
        distributionViewModel.selectDistribution(distributionId)
        activeDueScheduleLabel = distributionViewModel.getScheduledDueReminderLabel(distributionId)
    }

    val dist = activeState.distribution
    if (dist == null) {
        Scaffold(
            topBar = {
                SanctuaryTopHeader(
                    title = "Session Details",
                    onBack = onBack
                )
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = MaterialTheme.colorScheme.background,
            modifier = modifier
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 22.dp, vertical = 28.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                SanctuaryLoadingState(
                    message = "Loading session schedule..."
                )
            }
        }
        return
    }

    val isAmal = dist.type == DistributionType.AMAL
    val isOneTimeKhatm = dist.type == DistributionType.ONE_TIME_KHATM
    val isMonthlyKhatm = dist.type == DistributionType.KHATM

    // Refresh active reminder label when switching rooms or months
    LaunchedEffect(dist.id, activeState.participants, activeState.selectedYear, activeState.selectedMonth) {
        activeDueScheduleLabel = distributionViewModel.getScheduledDueReminderLabel(dist.id)
    }

    val currentCal = remember { Calendar.getInstance() }
    val isViewingCurrentMonth = activeState.selectedYear == currentCal.get(Calendar.YEAR) &&
        activeState.selectedMonth == (currentCal.get(Calendar.MONTH) + 1)

    // Strictly ordered 1 to 30 in ascending order of lowest Para
    val orderedAssignments = remember(activeState.assignments) {
        activeState.assignments.sortedWith(RotationEngine.paraFirstComparator)
    }

    val participantById = remember(activeState.participants) {
        activeState.participants.associateBy { it.id }
    }

    // Pinned participants / assignments for the dedicated "Pinned — My Paras" container
    val pinnedMonthlyAssignments = remember(orderedAssignments, participantById) {
        orderedAssignments.filter { participantById[it.participantId]?.isPinned == true }
    }
    val pinnedOneTimeClaims = remember(isOneTimeKhatm, activeState.participants) {
        if (!isOneTimeKhatm) emptyList()
        else activeState.participants
            .filter { it.isPinned }
            .sortedBy { it.specificParaNumber ?: it.displayOrder }
    }
    val pinnedAmalParticipants = remember(isAmal, activeState.participants) {
        if (!isAmal) emptyList()
        else activeState.participants.filter { it.isPinned }
    }

    // Map Para 1..30 -> ParticipantEntity for One-Time Khatm
    val oneTimeClaimsByPara = remember(isOneTimeKhatm, activeState.participants) {
        if (!isOneTimeKhatm) emptyMap()
        else activeState.participants.associateBy { it.specificParaNumber ?: it.displayOrder }
    }

    val totalAllocatedCount = remember(activeState.participants) {
        activeState.participants.sumOf { it.allocatedCount }
    }
    val remainingCapacity = (dist.totalTarget - totalAllocatedCount).coerceAtLeast(0)

    val allocationBlockSizes = remember(isAmal, isOneTimeKhatm, activeState.participants, orderedAssignments) {
        when {
            isAmal -> activeState.participants.map { it.allocatedCount }
            isOneTimeKhatm -> activeState.participants
                .groupingBy { it.name.trim().lowercase() }
                .eachCount()
                .values
                .toList()
            else -> orderedAssignments.map { it.allocatedCount }
        }
    }

    val palette = rememberAllocationPalette()

    Scaffold(
        topBar = {
            SanctuaryTopHeader(
                title = dist.name,
                badgeText = when {
                    dist.isClosedForever -> "CLOSED"
                    dist.isOnline -> "ONLINE"
                    else -> "LOCAL"
                },
                onBack = onBack
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
            val horizontalPad = if (maxWidth >= 640.dp) 32.dp else 22.dp

            LazyColumn(
                contentPadding = PaddingValues(
                    top = 20.dp,
                    bottom = 48.dp,
                    start = horizontalPad,
                    end = horizontalPad
                ),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // 0. Online Room Passcode, QR Code & Due Reminder Banner (if Online Room)
                if (dist.isOnline && !dist.roomPasscode.isNullOrBlank()) {
                    item(key = "online_room_passcode_banner") {
                        OnlineRoomPasscodeBanner(
                            passcode = dist.roomPasscode,
                            isClosed = dist.isClosedForever,
                            hasPendingSync = dist.hasPendingSync,
                            onShowQrCode = { showQrCodeDialog = true },
                            onCopyCode = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Room Passcode", dist.roomPasscode))
                                Toast.makeText(context, "Room Passcode copied", Toast.LENGTH_SHORT).show()
                            },
                            onOpenDueReminder = {
                                ensureNotificationPermissionRequested()
                                showDueReminderDialog = true
                            }
                        )
                    }
                }

                // 1. Spacious Overview Card + Multi-Color Allocation Breakdown Bar
                item(key = "overview_section") {
                    when {
                        isOneTimeKhatm && dist.isClosedForever -> {
                            ClosedForeverKhatmCard(
                                roomName = dist.name,
                                summaryText = dist.closedSummaryText ?: "30/30 Paras Completed • Closed"
                            )
                        }
                        isOneTimeKhatm -> {
                            val claimedCount = oneTimeClaimsByPara.size
                            val completedCount = oneTimeClaimsByPara.values.count { it.isCompleted }
                            OneTimeKhatmSummaryCard(
                                isOnline = dist.isOnline,
                                claimedCount = claimedCount,
                                completedCount = completedCount,
                                blockSizes = allocationBlockSizes
                            )
                        }
                        isAmal -> {
                            val completedSum = activeState.participants.sumOf {
                                if (it.isCompleted) it.allocatedCount else it.completedCount.coerceIn(0, it.allocatedCount)
                            }
                            AmalSummarySpecimenCard(
                                isOnline = dist.isOnline,
                                totalTarget = dist.totalTarget,
                                claimedSum = totalAllocatedCount,
                                completedSum = completedSum,
                                roundNumber = dist.roundNumber,
                                participantCount = activeState.participants.size,
                                blockSizes = allocationBlockSizes
                            )
                        }
                        else -> {
                            val completedCount = activeState.participants.count {
                                it.isCompletedForMonth(activeState.selectedYear, activeState.selectedMonth)
                            }
                            MonthCycleNavigatorCard(
                                isOnline = dist.isOnline,
                                dateInfo = activeState.dateInfo,
                                isCurrentMonth = isViewingCurrentMonth,
                                completedMembersCount = completedCount,
                                totalMembersCount = activeState.participants.size,
                                blockSizes = allocationBlockSizes,
                                onPreviousMonth = { distributionViewModel.navigateMonth(-1) },
                                onNextMonth = { distributionViewModel.navigateMonth(1) },
                                onResetCurrent = { distributionViewModel.resetToCurrentMonth() }
                            )
                        }
                    }
                }

                // 2. Dedicated Primary Action Buttons Section (Claim / Share / Copy / Due Reminder in their own clean space)
                item(key = "primary_actions_section") {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        if (!dist.isClosedForever) {
                            if (isOneTimeKhatm && oneTimeClaimsByPara.size < 30 && dist.isOnline) {
                                PrimarySanctuaryButton(
                                    text = "Claim Para (For Self or Guest)",
                                    icon = Icons.Default.PersonAdd,
                                    onClick = {
                                        showOneTimeClaimDialogForPara = null
                                        showOneTimeClaimModal = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("claim_para_button")
                                )
                            } else if ((dist.isOnline || activeState.participants.isEmpty()) && remainingCapacity > 0 && !isOneTimeKhatm) {
                                PrimarySanctuaryButton(
                                    text = if (isAmal) {
                                        "Pledge Recitations (Self or Guest • $remainingCapacity left)"
                                    } else {
                                        "Claim Monthly Para (Self or Guest • $remainingCapacity left)"
                                    },
                                    icon = Icons.Default.PersonAdd,
                                    onClick = { showJoinSlotDialog = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("join_and_claim_slot_button")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PrimarySanctuaryButton(
                                text = if (dist.isOnline) "Share Room" else "Share Schedule",
                                icon = Icons.Default.Share,
                                onClick = { showShareDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("bottom_share_schedule_button")
                            )

                            SecondaryEditorialButton(
                                text = "Copy Text",
                                icon = Icons.Default.ContentCopy,
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Quran Baanto", activeState.shareText)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied schedule to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Dedicated Customized Reminder Bar (Online: Remaining Paras/Amals; Local: Monthly Quran Notification)
                        if (!dist.isClosedForever && (dist.isOnline || isMonthlyKhatm)) {
                            DueReminderBannerButton(
                                isOnline = dist.isOnline,
                                activeScheduleLabel = activeDueScheduleLabel,
                                onClick = {
                                    ensureNotificationPermissionRequested()
                                    showDueReminderDialog = true
                                }
                            )
                        }
                    }
                }

                // 2.5 Dedicated "Pinned — My Paras" Section (when user pins their Paras or Amal share)
                if (!dist.isClosedForever && (
                        pinnedMonthlyAssignments.isNotEmpty() ||
                            pinnedOneTimeClaims.isNotEmpty() ||
                            pinnedAmalParticipants.isNotEmpty()
                        )
                ) {
                    item(key = "pinned_my_paras_section") {
                        PinnedMyParasSectionCard(
                            isOnline = dist.isOnline,
                            isMonthlyKhatm = isMonthlyKhatm,
                            isOneTimeKhatm = isOneTimeKhatm,
                            isAmal = isAmal,
                            pinnedMonthlyAssignments = pinnedMonthlyAssignments,
                            pinnedOneTimeClaims = pinnedOneTimeClaims,
                            pinnedAmalParticipants = pinnedAmalParticipants,
                            participantById = participantById,
                            selectedYear = activeState.selectedYear,
                            selectedMonth = activeState.selectedMonth,
                            onOpenParaReader = onOpenParaReader,
                            onToggleComplete = { participantId, label ->
                                if (dist.isOnline) {
                                    ensureNotificationPermissionRequested()
                                    distributionViewModel.toggleParticipantCompletion(
                                        distributionId = dist.id,
                                        participantId = participantId,
                                        assignedParaLabel = label
                                    )
                                }
                            },
                            onUnpin = { participantId ->
                                distributionViewModel.toggleParticipantPin(participantId)
                            }
                        )
                    }
                }

                // 3. Clean Schedule / Para Board Section Header
                if (!dist.isClosedForever) {
                    item(key = "schedule_header") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when {
                                        isOneTimeKhatm -> "30-Para Schedule (1 → 30)"
                                        isAmal -> "Member Allocations"
                                        else -> "Monthly Schedule (1 → 30)"
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                Text(
                                    text = when {
                                        isAmal && dist.isOnline -> "Tap + to count or tick done"
                                        isAmal -> "Tap + to count recitations"
                                        dist.isOnline -> "Pin 📌 or tick done"
                                        else -> "Pin 📌 or Jump to Para"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (isOneTimeKhatm && dist.isOnline) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val claimedCount = oneTimeClaimsByPara.size
                                        val availCount = (30 - claimedCount).coerceAtLeast(0)
                                        ParaFilterSegment(
                                            title = "All (30)",
                                            selected = oneTimeParaFilter == OneTimeParaFilter.ALL,
                                            onClick = { oneTimeParaFilter = OneTimeParaFilter.ALL },
                                            modifier = Modifier.weight(1f)
                                        )
                                        ParaFilterSegment(
                                            title = "Available ($availCount)",
                                            selected = oneTimeParaFilter == OneTimeParaFilter.AVAILABLE,
                                            onClick = { oneTimeParaFilter = OneTimeParaFilter.AVAILABLE },
                                            modifier = Modifier.weight(1f)
                                        )
                                        ParaFilterSegment(
                                            title = "Claimed ($claimedCount)",
                                            selected = oneTimeParaFilter == OneTimeParaFilter.CLAIMED,
                                            onClick = { oneTimeParaFilter = OneTimeParaFilter.CLAIMED },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(
                                thickness = 0.8.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
                            )
                        }
                    }

                    // 4. Individual Member Allocation Cards / 30-Para Interactive Slots
                    if (isOneTimeKhatm) {
                        if (!dist.isOnline && orderedAssignments.isNotEmpty()) {
                            // Local One-Time Khatm where all members were named at setup: show clean ascending member blocks
                            itemsIndexed(
                                items = orderedAssignments,
                                key = { _, item -> "local_onetime_${item.startPara}_${item.participantId}" }
                            ) { index, assignment ->
                                val partEntity = participantById[assignment.participantId]
                                val isPinned = partEntity?.isPinned ?: false

                                KhatmAssignmentSpecimenCard(
                                    isOnline = false,
                                    assignment = assignment,
                                    isCompleted = false,
                                    isGuest = false,
                                    isPinned = isPinned,
                                    accentColor = palette[index % palette.size],
                                    onTogglePin = {
                                        distributionViewModel.toggleParticipantPin(assignment.participantId)
                                    },
                                    onOpenReader = {
                                        onOpenParaReader(assignment.startPara)
                                    },
                                    onToggleComplete = {}
                                )
                            }
                        } else {
                            val visibleParas = (1..30).filter { paraNum ->
                                val isClaimed = oneTimeClaimsByPara.containsKey(paraNum)
                                when (oneTimeParaFilter) {
                                    OneTimeParaFilter.ALL -> true
                                    OneTimeParaFilter.AVAILABLE -> !isClaimed
                                    OneTimeParaFilter.CLAIMED -> isClaimed
                                }
                            }

                            items(
                                items = visibleParas,
                                key = { paraNum -> "one_time_para_$paraNum" }
                            ) { paraNum ->
                                val claim = oneTimeClaimsByPara[paraNum]
                                OneTimeParaSlotCard(
                                    isOnline = dist.isOnline,
                                    paraNumber = paraNum,
                                    claim = claim,
                                    accentColor = palette[(paraNum - 1) % palette.size],
                                    onClaimClick = {
                                        showOneTimeClaimDialogForPara = paraNum
                                        showOneTimeClaimModal = true
                                    },
                                    onToggleComplete = {
                                        if (claim != null && dist.isOnline) {
                                            ensureNotificationPermissionRequested()
                                            distributionViewModel.toggleParticipantCompletion(
                                                distributionId = dist.id,
                                                participantId = claim.id,
                                                assignedParaLabel = "Para $paraNum"
                                            )
                                        }
                                    },
                                    onTogglePin = {
                                        if (claim != null) {
                                            distributionViewModel.toggleParticipantPin(claim.id)
                                        }
                                    },
                                    onOpenReader = { onOpenParaReader(paraNum) },
                                    onUnclaim = {
                                        if (claim != null) {
                                            distributionViewModel.removeParticipantClaim(claim.id)
                                        }
                                    }
                                )
                            }
                        }
                    } else if (isAmal) {
                        if (activeState.participants.isEmpty()) {
                            item(key = "empty_online_amal_members") {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "No pledges yet in this round",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Tap 'Pledge Recitations' above or share the Room Passcode so members can join.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            itemsIndexed(
                                items = activeState.participants,
                                key = { _, item -> item.id }
                            ) { index, participant ->
                                AmalParticipantCard(
                                    isOnline = dist.isOnline,
                                    index = index + 1,
                                    participant = participant,
                                    accentColor = palette[index % palette.size],
                                    onUpdateCount = { newCount ->
                                        if (dist.isOnline) ensureNotificationPermissionRequested()
                                        distributionViewModel.updateAmalCompletedCount(
                                            distributionId = dist.id,
                                            participantId = participant.id,
                                            newCount = newCount
                                        )
                                    },
                                    onTogglePin = {
                                        distributionViewModel.toggleParticipantPin(participant.id)
                                    },
                                    onToggleComplete = {
                                        if (dist.isOnline) {
                                            ensureNotificationPermissionRequested()
                                            distributionViewModel.toggleParticipantCompletion(
                                                distributionId = dist.id,
                                                participantId = participant.id
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    } else if (isMonthlyKhatm) {
                        if (orderedAssignments.isEmpty()) {
                            item(key = "empty_online_monthly_members") {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Waiting for members to claim Paras",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Tap 'Claim Monthly Para' above or share the Room Passcode with your group.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            itemsIndexed(
                                items = orderedAssignments,
                                key = { _, item -> "${item.startPara}_${item.participantId}" }
                            ) { index, assignment ->
                                val partEntity = participantById[assignment.participantId]
                                val isDone = if (dist.isOnline) {
                                    partEntity?.isCompletedForMonth(
                                        activeState.selectedYear,
                                        activeState.selectedMonth
                                    ) ?: false
                                } else false
                                val isGuest = partEntity?.isGuestEntry ?: false
                                val isPinned = partEntity?.isPinned ?: false

                                KhatmAssignmentSpecimenCard(
                                    isOnline = dist.isOnline,
                                    assignment = assignment,
                                    isCompleted = isDone,
                                    isGuest = isGuest,
                                    isPinned = isPinned,
                                    accentColor = palette[index % palette.size],
                                    onTogglePin = {
                                        distributionViewModel.toggleParticipantPin(assignment.participantId)
                                    },
                                    onOpenReader = {
                                        onOpenParaReader(assignment.startPara)
                                    },
                                    onToggleComplete = {
                                        if (dist.isOnline) {
                                            ensureNotificationPermissionRequested()
                                            distributionViewModel.toggleParticipantCompletion(
                                                distributionId = dist.id,
                                                participantId = assignment.participantId,
                                                assignedParaLabel = assignment.displayRangeText
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // 5. Dedicated Session Management Section (Reopen Amal / Edit / Delete in their own clean space)
                item(key = "session_management_section") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        HorizontalDivider(
                            thickness = 0.8.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )

                        if (isAmal) {
                            PrimarySanctuaryButton(
                                text = "Reopen Room for New Round (Round ${dist.roundNumber + 1})",
                                icon = Icons.Default.Refresh,
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary,
                                onClick = { showReopenAmalDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reopen_amal_room_button")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (!dist.isClosedForever) {
                                SecondaryEditorialButton(
                                    text = "Edit Session",
                                    icon = Icons.Default.Edit,
                                    onClick = { onEditDistribution(distributionId) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("edit_distribution_button")
                                )
                            }

                            SecondaryEditorialButton(
                                text = "Delete Session",
                                icon = Icons.Default.Delete,
                                accentColor = MaterialTheme.colorScheme.error,
                                onClick = { showDeleteConfirmation = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("delete_distribution_button")
                            )
                        }
                    }
                }
            }
        }
    }

    if (shouldPromptNotificationOptIn) {
        NotificationPermissionOptInDialog(
            isOnlineRoomContext = dist.isOnline,
            onEnableReminders = {
                distributionViewModel.completeNotificationOptInPrompt(enabled = true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !KhatmNotificationHelper.hasNotificationPermission(context)
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onNotNow = {
                distributionViewModel.dismissNotificationOptInPrompt()
            }
        )
    }

    if (showOneTimeClaimModal) {
        val takenSet = remember(oneTimeClaimsByPara) { oneTimeClaimsByPara.keys }
        OneTimeParaClaimDialog(
            initialParaNumber = showOneTimeClaimDialogForPara,
            takenParaNumbers = takenSet,
            savedProfileName = savedProfileName,
            onConfirmClaim = { selectedParas, memberName, isGuest ->
                distributionViewModel.claimMultipleOneTimeParas(
                    distributionId = dist.id,
                    paraNumbers = selectedParas,
                    memberName = memberName,
                    isGuest = isGuest
                ) { count ->
                    showOneTimeClaimModal = false
                    if (count > 0) {
                        Toast.makeText(
                            context,
                            "Claimed $count ${if (count == 1) "Para" else "Paras"} for $memberName",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            onDismiss = { showOneTimeClaimModal = false }
        )
    }

    if (showJoinSlotDialog) {
        JoinAndClaimSlotDialog(
            isAmal = isAmal,
            remainingCapacity = remainingCapacity,
            savedProfileName = savedProfileName,
            onConfirmClaim = { memberName, count, isGuest ->
                distributionViewModel.joinAndClaimAllocation(
                    distributionId = dist.id,
                    memberName = memberName,
                    requestedCount = count,
                    isGuest = isGuest
                ) { success ->
                    showJoinSlotDialog = false
                    if (success) {
                        Toast.makeText(
                            context,
                            "Added $memberName ($count)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            onDismiss = { showJoinSlotDialog = false }
        )
    }

    if (showReopenAmalDialog) {
        ReopenAmalRoomDialog(
            roomName = dist.name,
            currentRound = dist.roundNumber,
            isOnlineRoom = dist.isOnline,
            onConfirmReopen = { keepExisting ->
                showReopenAmalDialog = false
                distributionViewModel.reopenAmalRoom(
                    distributionId = dist.id,
                    keepMembersAndResetTicks = keepExisting
                )
                if (!dist.isOnline) {
                    Toast.makeText(
                        context,
                        "Reopened \"${dist.name}\" for Round ${dist.roundNumber + 1}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onDismiss = { showReopenAmalDialog = false }
        )
    }

    reopenedPasscode?.let { newCode ->
        ReopenedRoomPasscodeDialog(
            roomName = dist.name,
            roundNumber = dist.roundNumber,
            newPasscode = newCode,
            onShareClick = { showShareDialog = true },
            onDismiss = { distributionViewModel.dismissReopenedRoomPasscode() }
        )
    }

    celebrationMessage?.let { msg ->
        KhatmCelebrationDialog(
            message = msg,
            onDismiss = { distributionViewModel.dismissCelebrationMessage() }
        )
    }

    if (showShareDialog) {
        ShareDistributionDialog(
            shareText = activeState.shareText,
            roomPasscode = if (dist.isOnline) dist.roomPasscode else null,
            onShowQrCode = if (dist.isOnline && !dist.roomPasscode.isNullOrBlank()) {
                { showQrCodeDialog = true }
            } else null,
            onDismiss = { showShareDialog = false }
        )
    }

    if (showQrCodeDialog && !dist.roomPasscode.isNullOrBlank()) {
        val typeLabel = when (dist.type) {
            DistributionType.KHATM -> "Online Monthly 30-Para Khatm"
            DistributionType.ONE_TIME_KHATM -> "Online One-Time 30-Para Khatm"
            DistributionType.AMAL -> "Online Custom Amal (${dist.totalTarget} Target)"
        }
        RoomQrCodeDialog(
            roomName = dist.name,
            passcode = dist.roomPasscode,
            typeSubtitle = typeLabel,
            onDismiss = { showQrCodeDialog = false }
        )
    }

    if (showDueReminderDialog) {
        if (dist.isOnline) {
            val remainingSummary = remember(
                dist.id,
                activeState.participants,
                activeState.assignments,
                activeState.selectedYear,
                activeState.selectedMonth,
                savedProfileName
            ) {
                distributionViewModel.buildOnlineUserRemainingSummary(
                    dist = dist,
                    participants = activeState.participants,
                    assignments = activeState.assignments,
                    year = activeState.selectedYear,
                    month = activeState.selectedMonth,
                    profileName = savedProfileName
                )
            }
            OnlineRemainingReminderDialog(
                roomName = dist.name,
                memberName = savedProfileName,
                remainingSummary = remainingSummary,
                activeScheduleLabel = activeDueScheduleLabel,
                onSelectPreset = { preset ->
                    val message = distributionViewModel.scheduleOnlineReminderForActiveRoom(preset)
                    activeDueScheduleLabel = distributionViewModel.getScheduledDueReminderLabel(dist.id)
                    showDueReminderDialog = false
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                },
                onCancelScheduled = {
                    distributionViewModel.cancelDueReminderForActiveRoom()
                    activeDueScheduleLabel = null
                    showDueReminderDialog = false
                    Toast.makeText(context, "Cleared scheduled reminder", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showDueReminderDialog = false }
            )
        } else {
            LocalMonthlyNotificationDialog(
                roomName = dist.name,
                activeScheduleLabel = activeDueScheduleLabel,
                onEnableMonthlyAlert = {
                    val message = distributionViewModel.enableLocalMonthlyNotificationForActiveRoom()
                    activeDueScheduleLabel = distributionViewModel.getScheduledDueReminderLabel(dist.id)
                    showDueReminderDialog = false
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                },
                onDisableMonthlyAlert = {
                    distributionViewModel.cancelDueReminderForActiveRoom()
                    activeDueScheduleLabel = null
                    showDueReminderDialog = false
                    Toast.makeText(context, "Disabled monthly notification", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showDueReminderDialog = false }
            )
        }
    }

    if (showDeleteConfirmation) {
        DeleteConfirmationDialog(
            distributionName = dist.name,
            onConfirmDelete = {
                distributionViewModel.deleteDistribution(distributionId)
                showDeleteConfirmation = false
                onBack()
            },
            onDismiss = { showDeleteConfirmation = false }
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
            roomName = pendingRoomName.ifBlank { dist.name },
            isRoomCreation = isRoomCreation,
            onAccept = {
                ServerSupportAdManager.acceptVoluntaryAd(adScope)
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
}

@Composable
private fun OnlineRoomPasscodeBanner(
    passcode: String,
    isClosed: Boolean,
    hasPendingSync: Boolean = false,
    onShowQrCode: () -> Unit,
    onCopyCode: () -> Unit,
    onOpenDueReminder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = OnlineGoldAccent.copy(alpha = 0.12f),
        border = BorderStroke(1.2.dp, OnlineGoldAccent.copy(alpha = 0.55f)),
        modifier = modifier.fillMaxWidth()
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
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(OnlineGoldAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = Color(0xFF1A1405),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "ONLINE ROOM PASSCODE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.7.sp,
                                fontSize = 10.sp
                            ),
                            color = OnlineGoldAccent
                        )
                        if (hasPendingSync) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = "Offline Sync Queued",
                                tint = OnlineGoldAccent,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                    Text(
                        text = passcode,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = onShowQrCode,
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("banner_qr_code_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = "Show Room QR Code",
                            tint = OnlineGoldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (!isClosed) {
                    Surface(
                        onClick = onOpenDueReminder,
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("banner_due_reminder_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Schedule Due Reminder",
                                tint = OnlineGoldAccent,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                Surface(
                    onClick = onCopyCode,
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("banner_copy_passcode_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Room Passcode",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DueReminderBannerButton(
    isOnline: Boolean,
    activeScheduleLabel: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = if (isOnline) OnlineGoldAccent else LocalBlueAccent
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("schedule_due_reminder_bar_button")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = when {
                            !activeScheduleLabel.isNullOrBlank() -> "Reminder: $activeScheduleLabel"
                            isOnline -> "Remind Me of My Remaining Share"
                            else -> "Monthly New Distribution Alert"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isOnline) {
                            "Custom alerts only for your remaining Paras or Amal recitations"
                        } else {
                            "Get notified on the 1st of each month when the new schedule is ready to read & share"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Configure Reminder",
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Dedicated "Pinned — My Paras" container that sits cleanly above the schedule list
 * whenever the user pins one or more assignments for quick access.
 */
@Composable
private fun PinnedMyParasSectionCard(
    isOnline: Boolean,
    isMonthlyKhatm: Boolean,
    isOneTimeKhatm: Boolean,
    isAmal: Boolean,
    pinnedMonthlyAssignments: List<ParaAssignment>,
    pinnedOneTimeClaims: List<ParticipantEntity>,
    pinnedAmalParticipants: List<ParticipantEntity>,
    participantById: Map<Long, ParticipantEntity>,
    selectedYear: Int,
    selectedMonth: Int,
    onOpenParaReader: (Int) -> Unit,
    onToggleComplete: (Long, String?) -> Unit,
    onUnpin: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.28f),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("pinned_my_paras_section")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = "PINNED • MY PARAS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Text(
                    text = "Quick Access",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isMonthlyKhatm) {
                pinnedMonthlyAssignments.forEach { assignment ->
                    val isDone = if (isOnline) {
                        participantById[assignment.participantId]
                            ?.isCompletedForMonth(selectedYear, selectedMonth) ?: false
                    } else false
                    PinnedRowItem(
                        isOnline = isOnline,
                        name = assignment.participantName,
                        rightValue = assignment.commaSeparatedParas,
                        subtitle = assignment.displayRangeText,
                        jumpParaNumber = assignment.startPara,
                        isCompleted = isDone,
                        onOpenReader = { onOpenParaReader(assignment.startPara) },
                        onToggleComplete = {
                            onToggleComplete(assignment.participantId, assignment.displayRangeText)
                        },
                        onUnpin = { onUnpin(assignment.participantId) }
                    )
                }
            } else if (isOneTimeKhatm) {
                pinnedOneTimeClaims.forEach { claim ->
                    val paraNum = claim.specificParaNumber ?: claim.displayOrder
                    PinnedRowItem(
                        isOnline = isOnline,
                        name = claim.name,
                        rightValue = "$paraNum",
                        subtitle = "Para $paraNum",
                        jumpParaNumber = paraNum,
                        isCompleted = if (isOnline) claim.isCompleted else false,
                        onOpenReader = { onOpenParaReader(paraNum) },
                        onToggleComplete = { onToggleComplete(claim.id, "Para $paraNum") },
                        onUnpin = { onUnpin(claim.id) }
                    )
                }
            } else if (isAmal) {
                pinnedAmalParticipants.forEach { part ->
                    PinnedRowItem(
                        isOnline = isOnline,
                        name = part.name,
                        rightValue = "${part.completedCount}/${part.allocatedCount}",
                        subtitle = "${part.allocatedCount} Recitations",
                        jumpParaNumber = null,
                        isCompleted = if (isOnline) part.isCompleted else false,
                        onOpenReader = null,
                        onToggleComplete = { onToggleComplete(part.id, null) },
                        onUnpin = { onUnpin(part.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PinnedRowItem(
    isOnline: Boolean,
    name: String,
    rightValue: String,
    subtitle: String,
    jumpParaNumber: Int?,
    isCompleted: Boolean,
    onOpenReader: (() -> Unit)?,
    onToggleComplete: () -> Unit,
    onUnpin: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.9.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = rightValue,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    IconButton(
                        onClick = onUnpin,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Unpin",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (isOnline) {
                        CompletionTickButton(
                            isCompleted = isCompleted,
                            onClick = onToggleComplete
                        )
                    }
                }
            }

            if (onOpenReader != null && jumpParaNumber != null) {
                JumpToParaActionContainer(
                    paraLabel = subtitle,
                    buttonText = "Jump to Para $jumpParaNumber",
                    accentColor = MaterialTheme.colorScheme.primary,
                    onClick = onOpenReader
                )
            }
        }
    }
}

@Composable
private fun OneTimeKhatmSummaryCard(
    isOnline: Boolean,
    claimedCount: Int,
    completedCount: Int,
    blockSizes: List<Int>,
    modifier: Modifier = Modifier
) {
    ResizableSectionContainer(
        modifier = modifier,
        contentPadding = PaddingValues(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SanctuaryGeometricGlyph(
                isQuranGlyph = true,
                size = 52.dp
            )

            Spacer(modifier = Modifier.width(18.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "One-Time 30-Para Khatm",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isOnline) {
                        "Claimed: $claimedCount/30 Paras • Completed: $completedCount/30"
                    } else {
                        "Assigned: 30/30 Paras • Local Reference Schedule"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (blockSizes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            ContiguousParaBlockBar(
                blockSizes = blockSizes,
                totalTarget = 30
            )
        }
    }
}

@Composable
private fun ClosedForeverKhatmCard(
    roomName: String,
    summaryText: String,
    modifier: Modifier = Modifier
) {
    ResizableSectionContainer(
        modifier = modifier,
        contentPadding = PaddingValues(26.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(30.dp)
                )
            }

            Text(
                text = "Alhamdulillah! Khatm Completed",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            ) {
                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Text(
                text = "All 30 Paras of \"$roomName\" were completed. Detailed slot data has been automatically cleaned up to save space and this room is now closed forever.",
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ParaFilterSegment(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        modifier = modifier.height(38.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 12.sp
                ),
                color = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun OneTimeParaSlotCard(
    isOnline: Boolean,
    paraNumber: Int,
    claim: ParticipantEntity?,
    accentColor: Color,
    onClaimClick: () -> Unit,
    onToggleComplete: () -> Unit,
    onTogglePin: () -> Unit,
    onOpenReader: () -> Unit,
    onUnclaim: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isClaimed = claim != null
    val isCompleted = isOnline && claim?.isCompleted == true
    val isPinned = claim?.isPinned == true

    Surface(
        onClick = {
            if (!isClaimed) onClaimClick()
            else if (isOnline) onToggleComplete()
            else onOpenReader()
        },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (isCompleted) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (isCompleted || isPinned) 1.3.dp else 1.dp,
            color = when {
                isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                isPinned -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f)
                else -> MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    OctagonalNumberMedallion(
                        number = paraNumber,
                        size = 42.dp,
                        primaryColor = if (isClaimed) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        if (claim != null) {
                            // Name-first display with Para number badge on the right
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = claim.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (claim.isGuestEntry) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = "Guest",
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
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = when {
                                    isCompleted -> "Para $paraNumber • Completed ✓"
                                    isOnline -> "Para $paraNumber • In progress"
                                    else -> "Para $paraNumber Assigned"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isCompleted) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Para $paraNumber",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Available • Tap to claim",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (claim == null) {
                    Surface(
                        onClick = onClaimClick,
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                        border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Claim",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = accentColor.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "$paraNumber",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(
                            onClick = onTogglePin,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                contentDescription = if (isPinned) "Unpin Para $paraNumber" else "Pin Para $paraNumber",
                                tint = if (isPinned) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        if (isOnline && !isCompleted) {
                            IconButton(
                                onClick = onUnclaim,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Release Para $paraNumber",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        if (isOnline) {
                            CompletionTickButton(
                                isCompleted = isCompleted,
                                onClick = onToggleComplete
                            )
                        }
                    }
                }
            }

            if (claim != null) {
                JumpToParaActionContainer(
                    paraLabel = "Para $paraNumber",
                    buttonText = "Jump to Para $paraNumber",
                    accentColor = accentColor,
                    onClick = onOpenReader
                )
            }
        }
    }
}

@Composable
private fun CompletionTickButton(
    isCompleted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isCompleted) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = BorderStroke(
            width = 0.9.dp,
            color = if (isCompleted) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (isCompleted) "Completed" else "Mark Done",
                tint = if (isCompleted) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (isCompleted) "Done" else "Tick",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isCompleted) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
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

@Composable
private fun AmalSummarySpecimenCard(
    isOnline: Boolean,
    totalTarget: Int,
    claimedSum: Int,
    completedSum: Int,
    roundNumber: Int,
    participantCount: Int,
    blockSizes: List<Int>,
    modifier: Modifier = Modifier
) {
    ResizableSectionContainer(
        modifier = modifier,
        contentPadding = PaddingValues(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SanctuaryGeometricGlyph(
                isQuranGlyph = false,
                size = 52.dp
            )

            Spacer(modifier = Modifier.width(18.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$totalTarget Total Recitations",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (roundNumber > 1) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "Round $roundNumber",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = if (isOnline) {
                        "$participantCount Members • Recited: $completedSum/$claimedSum"
                    } else {
                        "$participantCount Members • Assigned: $claimedSum/$totalTarget"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        if (blockSizes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            ContiguousParaBlockBar(
                blockSizes = blockSizes,
                totalTarget = totalTarget
            )
        }
    }
}

@Composable
private fun MonthCycleNavigatorCard(
    isOnline: Boolean,
    dateInfo: MonthDateInfo,
    isCurrentMonth: Boolean,
    completedMembersCount: Int,
    totalMembersCount: Int,
    blockSizes: List<Int>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetCurrent: () -> Unit,
    modifier: Modifier = Modifier
) {
    ResizableSectionContainer(
        modifier = modifier,
        contentPadding = PaddingValues(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onPreviousMonth,
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous Month",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = dateInfo.gregorianFullText,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateInfo.hijriFullText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    ),
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )
                if (totalMembersCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isOnline) {
                            "Completed: $completedMembersCount/$totalMembersCount Members"
                        } else {
                            "30 Paras Assigned ($totalMembersCount Members) • Local Schedule"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                onClick = onNextMonth,
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next Month",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        if (blockSizes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            ContiguousParaBlockBar(
                blockSizes = blockSizes,
                totalTarget = 30
            )
        }

        if (!isCurrentMonth) {
            Spacer(modifier = Modifier.height(18.dp))
            SecondaryEditorialButton(
                text = "Return to Current Month",
                icon = Icons.Default.Today,
                onClick = onResetCurrent,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Custom Amal participant card featuring Name-first layout, Pin toggle,
 * and a dedicated Tactile Counter sub-container with haptic feedback (+1, +5, +10, -1).
 */
@Composable
private fun AmalParticipantCard(
    isOnline: Boolean,
    index: Int,
    participant: ParticipantEntity,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onUpdateCount: (Int) -> Unit,
    onTogglePin: () -> Unit,
    onToggleComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val isCompleted = isOnline && participant.isCompleted
    val isPinned = participant.isPinned
    val currentCount = if (isCompleted) participant.allocatedCount
    else participant.completedCount.coerceIn(0, participant.allocatedCount)
    val progressFraction = if (participant.allocatedCount > 0) {
        (currentCount.toFloat() / participant.allocatedCount.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("amal_participant_card_${participant.id}"),
        shape = RoundedCornerShape(20.dp),
        color = if (isCompleted) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (isCompleted || isPinned) 1.3.dp else 1.dp,
            color = when {
                isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                isPinned -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f)
                else -> MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Medallion + Name-first + Count badge + Pin (+ Tick for Online)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    OctagonalNumberMedallion(
                        number = index,
                        size = 42.dp,
                        primaryColor = accentColor
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = participant.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (participant.isGuestEntry) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = "Guest",
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
                        Text(
                            text = "Recited: $currentCount / ${participant.allocatedCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = accentColor.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "${participant.allocatedCount}",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp
                            ),
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (isPinned) "Unpin ${participant.name}" else "Pin ${participant.name}",
                            tint = if (isPinned) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    if (isOnline) {
                        CompletionTickButton(
                            isCompleted = isCompleted,
                            onClick = onToggleComplete
                        )
                    }
                }
            }

            // Dedicated Tactile Counter Container
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        color = accentColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = {
                                    if (currentCount > 0) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onUpdateCount(currentCount - 1)
                                    }
                                },
                                enabled = currentCount > 0,
                                shape = RoundedCornerShape(9.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Decrement count",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            if (participant.allocatedCount >= 5 && !isCompleted) {
                                Surface(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onUpdateCount((currentCount + 5).coerceAtMost(participant.allocatedCount))
                                    },
                                    shape = RoundedCornerShape(9.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Text(
                                        text = "+5",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                    )
                                }
                            }

                            if (participant.allocatedCount >= 10 && !isCompleted) {
                                Surface(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onUpdateCount((currentCount + 10).coerceAtMost(participant.allocatedCount))
                                    },
                                    shape = RoundedCornerShape(9.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Text(
                                        text = "+10",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }

                        // Primary Tactile Tap Button (+1)
                        Surface(
                            onClick = {
                                if (!isCompleted && currentCount < participant.allocatedCount) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onUpdateCount((currentCount + 1).coerceAtMost(participant.allocatedCount))
                                }
                            },
                            enabled = !isCompleted && currentCount < participant.allocatedCount,
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCompleted || currentCount >= participant.allocatedCount) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else accentColor,
                            modifier = Modifier.testTag("amal_tactile_tap_button_${participant.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCompleted || currentCount >= participant.allocatedCount) {
                                        Icons.Default.CheckCircle
                                    } else Icons.Default.Add,
                                    contentDescription = "Tap to count recitation",
                                    tint = if (isCompleted || currentCount >= participant.allocatedCount) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isCompleted || currentCount >= participant.allocatedCount) {
                                        "Completed"
                                    } else "Tap +1 ($currentCount/${participant.allocatedCount})",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isCompleted || currentCount >= participant.allocatedCount) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else Color.White
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
 * Displays a single member's contiguous Para allocation ordered strictly in ascending order of Paras (1 -> 30),
 * formatted with the Participant Name on the left and the Para numbers aligned on the right:
 *
 * Abbas             1, 2
 * Khadija g.        3
 * ...
 * Qurat             28, 29, 30
 *
 * Includes a dedicated, well-proportioned Jump to Para container & button underneath so the user
 * can open the Quran Reader cleanly without cluttering the name/number row.
 */
@Composable
private fun KhatmAssignmentSpecimenCard(
    isOnline: Boolean,
    assignment: ParaAssignment,
    isCompleted: Boolean,
    isGuest: Boolean,
    isPinned: Boolean,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onTogglePin: () -> Unit,
    onOpenReader: () -> Unit,
    onToggleComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val effectiveCompleted = isOnline && isCompleted
    Surface(
        onClick = {
            if (isOnline) onToggleComplete() else onOpenReader()
        },
        modifier = modifier
            .fillMaxWidth()
            .testTag("khatm_assignment_card_${assignment.startPara}"),
        shape = RoundedCornerShape(20.dp),
        color = if (effectiveCompleted) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (effectiveCompleted || isPinned) 1.3.dp else 1.dp,
            color = when {
                effectiveCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                isPinned -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f)
                else -> MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Medallion + Participant Name on left, Ascending Para Numbers Pill + Pin (+ Tick if Online) on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    OctagonalNumberMedallion(
                        number = assignment.startPara,
                        size = 42.dp,
                        primaryColor = accentColor
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = assignment.participantName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    lineHeight = 22.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isGuest) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = "Guest",
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

                        Text(
                            text = assignment.displayRangeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = accentColor.copy(alpha = 0.14f),
                        border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = assignment.commaSeparatedParas,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp
                            ),
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("pin_assignment_button_${assignment.startPara}")
                    ) {
                        Icon(
                            imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (isPinned) "Unpin ${assignment.participantName}" else "Pin ${assignment.participantName}",
                            tint = if (isPinned) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    if (isOnline) {
                        CompletionTickButton(
                            isCompleted = effectiveCompleted,
                            onClick = onToggleComplete
                        )
                    }
                }
            }

            // Dedicated Jump to Para Container & Button
            JumpToParaActionContainer(
                paraLabel = assignment.displayRangeText,
                buttonText = "Jump to Para ${assignment.startPara}",
                accentColor = accentColor,
                onClick = onOpenReader,
                modifier = Modifier.testTag("jump_to_para_button_${assignment.startPara}")
            )
        }
    }
}

/**
 * Dedicated container and structured button for jumping directly to an assigned Para in the Quran Reader.
 * Gives the Jump action its own clean, balanced space rather than a cramped linked text or tiny icon.
 */
@Composable
private fun JumpToParaActionContainer(
    paraLabel: String,
    buttonText: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(13.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(0.9.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Text(
                    text = "Quran Reader • $paraLabel",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                onClick = onClick,
                shape = RoundedCornerShape(9.dp),
                color = accentColor.copy(alpha = 0.16f),
                border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = buttonText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = accentColor
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = buttonText,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
