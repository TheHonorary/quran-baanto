package com.example.ui.distribution

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.DistributionType
import com.example.data.db.RoomMode
import com.example.domain.ParticipantAllocation
import com.example.ui.components.ManuscriptOrnamentDivider
import com.example.ui.components.PrimarySanctuaryButton
import com.example.ui.components.SanctuaryTopHeader
import com.example.ui.components.SecondaryEditorialButton
import com.example.ui.theme.LocalBlueAccent
import com.example.ui.theme.LocalBlueGlow
import com.example.ui.theme.OnlineGoldAccent
import com.example.ui.theme.OnlineGoldGlow
import com.example.ui.viewmodel.DistributionViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateEditDistributionScreen(
    distributionId: Long?,
    initialType: DistributionType = DistributionType.KHATM,
    initialRoomMode: RoomMode = RoomMode.LOCAL,
    distributionViewModel: DistributionViewModel,
    onSaved: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeState by distributionViewModel.activeDistributionState.collectAsState()
    val savedProfileName by distributionViewModel.userProfileName.collectAsState()
    val isEditMode = distributionId != null && distributionId > 0L

    var distributionName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(initialType) }
    var selectedRoomMode by remember { mutableStateOf(initialRoomMode) }
    var totalTargetText by remember {
        mutableStateOf(if (initialType == DistributionType.AMAL) "100" else "30")
    }
    var showModeSwitcherInForm by remember { mutableStateOf(false) }

    // Local Room full roster (or existing Online Room roster when editing)
    val allocations = remember { mutableStateListOf<ParticipantAllocation>() }

    // Local Room "Add Person" builder state
    var newParticipantName by remember { mutableStateOf("") }
    var newParticipantCount by remember { mutableIntStateOf(1) }
    var newParticipantCountText by remember { mutableStateOf("1") }

    // Online Room optional Host Initial Claim state
    var hostWantsInitialClaim by remember { mutableStateOf(false) }
    var hostClaimName by remember(savedProfileName) { mutableStateOf(savedProfileName) }
    var hostClaimCount by remember { mutableIntStateOf(1) }
    var hostClaimCountText by remember { mutableStateOf("1") }

    var initializedForId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(distributionId, activeState.distribution, activeState.participants) {
        if (isEditMode) {
            val dist = activeState.distribution
            if (dist != null && dist.id == distributionId && initializedForId != distributionId) {
                distributionName = dist.name
                selectedType = dist.type
                selectedRoomMode = dist.roomMode
                totalTargetText = dist.totalTarget.toString()
                allocations.clear()

                if (dist.type == DistributionType.ONE_TIME_KHATM && dist.roomMode == RoomMode.LOCAL) {
                    // Group consecutive Paras back by participant name for editing Local One-Time Khatm
                    val grouped = linkedMapOf<String, Int>()
                    activeState.participants
                        .sortedBy { it.specificParaNumber ?: it.displayOrder }
                        .forEach { p ->
                            grouped[p.name] = (grouped[p.name] ?: 0) + 1
                        }
                    var idx = 1
                    grouped.forEach { (pName, count) ->
                        allocations.add(
                            ParticipantAllocation(
                                id = 0L,
                                name = pName,
                                count = count,
                                displayOrder = idx++
                            )
                        )
                    }
                } else {
                    allocations.addAll(
                        activeState.participants.map {
                            ParticipantAllocation(
                                id = it.id,
                                name = it.name,
                                count = it.allocatedCount,
                                displayOrder = it.displayOrder
                            )
                        }
                    )
                }
                initializedForId = distributionId
            }
        }
    }

    val isOnlineMode = selectedRoomMode == RoomMode.ONLINE
    val isAmalMode = selectedType == DistributionType.AMAL
    val isOneTimeKhatmMode = selectedType == DistributionType.ONE_TIME_KHATM
    val isMonthlyKhatmMode = selectedType == DistributionType.KHATM

    val activeAccent = if (isOnlineMode) OnlineGoldAccent else LocalBlueAccent
    val activeGlow = if (isOnlineMode) OnlineGoldGlow else LocalBlueGlow

    val effectiveTarget = if (isAmalMode) {
        (totalTargetText.toIntOrNull() ?: 0).coerceAtLeast(0)
    } else {
        30
    }

    val context = LocalContext.current
    var hasAttemptedSave by remember { mutableStateOf(false) }
    var participantInputError by remember { mutableStateOf<String?>(null) }

    val currentSum = allocations.sumOf { it.count }
    val remainingUnits = effectiveTarget - currentSum

    // Comprehensive Form Validation Rules:
    // Computes all missing / invalid items so user is shown EXACTLY what is missing and why the form cannot proceed
    val validationErrors = remember(
        isOnlineMode,
        isEditMode,
        distributionName,
        effectiveTarget,
        allocations.toList(),
        currentSum,
        hostWantsInitialClaim,
        hostClaimName,
        hostClaimCount,
        hostClaimCountText,
        isAmalMode
    ) {
        val errors = mutableListOf<String>()
        if (distributionName.trim().isBlank()) {
            errors.add("Room Title / Name is compulsory (cannot be empty)")
        }
        if (effectiveTarget <= 0) {
            errors.add("Total target count must be greater than 0")
        }
        if (!isOnlineMode) {
            if (allocations.isEmpty()) {
                errors.add(
                    if (isAmalMode) "Roster is empty: Please add participants and allocate recitations"
                    else "Roster is empty: Please add participants until all 30 Paras are assigned"
                )
            } else if (currentSum != effectiveTarget) {
                val diff = effectiveTarget - currentSum
                if (diff > 0) {
                    errors.add("$diff ${if (isAmalMode) "recitations" else "Paras"} unassigned ($currentSum / $effectiveTarget assigned). Please assign all $effectiveTarget to proceed.")
                } else {
                    errors.add("Over-allocated by ${-diff} ${if (isAmalMode) "recitations" else "Paras"} ($currentSum / $effectiveTarget assigned). Please adjust to match $effectiveTarget.")
                }
            }
            if (allocations.any { it.name.trim().isBlank() }) {
                errors.add("Every member in the roster must have a valid name")
            }
        } else {
            if (hostWantsInitialClaim && !isEditMode) {
                if (hostClaimName.trim().isBlank()) {
                    errors.add("Your display name is compulsory when pre-claiming a share")
                }
                val parsedHostCount = if (isAmalMode) {
                    (hostClaimCountText.toIntOrNull() ?: 0)
                } else {
                    hostClaimCount
                }
                if (parsedHostCount !in 1..effectiveTarget.coerceAtLeast(1)) {
                    errors.add("Pre-claim count must be between 1 and $effectiveTarget")
                }
            }
        }
        errors
    }

    val isValidToSave = validationErrors.isEmpty()

    val addLocalParticipantAction = {
        val trimmed = newParticipantName.trim()
        val countToUse = if (isAmalMode) {
            (newParticipantCountText.toIntOrNull() ?: 1).coerceAtLeast(1)
        } else {
            newParticipantCount.coerceIn(1, 30)
        }
        if (trimmed.isBlank()) {
            participantInputError = "Participant name is compulsory to add them to the roster"
        } else if (countToUse <= 0) {
            participantInputError = "Allocated count must be at least 1"
        } else {
            participantInputError = null
            allocations.add(
                ParticipantAllocation(
                    id = 0L,
                    name = trimmed,
                    count = countToUse,
                    displayOrder = allocations.size + 1
                )
            )
            newParticipantName = ""
            val newRemaining = (effectiveTarget - allocations.sumOf { it.count }).coerceAtLeast(1)
            newParticipantCount = newRemaining.coerceIn(1, 3)
            newParticipantCountText = newRemaining.coerceAtMost(25).toString()
        }
    }

    val distributeRemainingEquallyAction = {
        if (allocations.isNotEmpty() && effectiveTarget > 0) {
            val count = allocations.size
            val baseShare = effectiveTarget / count
            val remainder = effectiveTarget % count
            if (baseShare >= 1) {
                for (i in allocations.indices) {
                    val share = baseShare + if (i < remainder) 1 else 0
                    allocations[i] = allocations[i].copy(count = share)
                }
            }
        }
    }

    val screenTitle = when {
        isEditMode && isOnlineMode -> "Edit Online Room"
        isEditMode && !isOnlineMode -> "Edit Local Schedule"
        isOnlineMode -> "Setup Online Room"
        else -> "Setup Local Schedule"
    }

    Scaffold(
        topBar = {
            SanctuaryTopHeader(
                title = screenTitle,
                badgeText = if (isOnlineMode) "ONLINE" else "LOCAL",
                onBack = onNavigateBack
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(0.8.dp, activeAccent.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isOnlineMode) "ONLINE ROOM • GOLD GLOW" else "LOCAL SCHEDULE • BLUE GLOW",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.7.sp,
                                fontSize = 10.sp
                            ),
                            color = activeAccent
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when {
                                hasAttemptedSave && validationErrors.isNotEmpty() ->
                                    "⚠️ ${validationErrors.size} ${if (validationErrors.size == 1) "Requirement" else "Requirements"} Missing"
                                isOnlineMode && !isEditMode -> {
                                    if (distributionName.isBlank()) {
                                        "Room Title Required *"
                                    } else if (hostWantsInitialClaim && hostClaimName.isNotBlank()) {
                                        "Passcode Room + Initial Claim by ${hostClaimName.trim()}"
                                    } else {
                                        "Ready to create Passcode & QR Room"
                                    }
                                }
                                !isOnlineMode -> {
                                    val unitName = if (isAmalMode) "Recitations" else "Paras"
                                    "$currentSum / $effectiveTarget $unitName Named (${allocations.size} ${if (allocations.size == 1) "Person" else "People"})"
                                }
                                else -> "${allocations.size} Members"
                            },
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (hasAttemptedSave && validationErrors.isNotEmpty()) {
                                MaterialTheme.colorScheme.error
                            } else if (!isOnlineMode && currentSum != effectiveTarget) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    PrimarySanctuaryButton(
                        text = when {
                            !isValidToSave && hasAttemptedSave -> "Fix Requirements"
                            isEditMode -> "Save Changes"
                            isOnlineMode -> "Create Online Room"
                            else -> "Create Schedule"
                        },
                        icon = if (isValidToSave) Icons.Default.Check else Icons.Default.ErrorOutline,
                        onClick = {
                            if (!isValidToSave) {
                                hasAttemptedSave = true
                                val firstErr = validationErrors.firstOrNull() ?: "Please complete all compulsory fields"
                                Toast.makeText(context, firstErr, Toast.LENGTH_SHORT).show()
                            } else {
                                val finalAllocations = if (isOnlineMode && !isEditMode) {
                                    if (hostWantsInitialClaim && hostClaimName.trim().isNotBlank()) {
                                        val c = if (isAmalMode) {
                                            (hostClaimCountText.toIntOrNull() ?: 1).coerceIn(1, effectiveTarget.coerceAtLeast(1))
                                        } else {
                                            hostClaimCount.coerceIn(1, 30)
                                        }
                                        listOf(
                                            ParticipantAllocation(
                                                id = 0L,
                                                name = hostClaimName.trim(),
                                                count = c,
                                                displayOrder = 1
                                            )
                                        )
                                    } else {
                                        emptyList()
                                    }
                                } else {
                                    allocations.toList()
                                }

                                distributionViewModel.saveDistribution(
                                    name = distributionName.trim(),
                                    type = selectedType,
                                    roomMode = selectedRoomMode,
                                    totalTarget = effectiveTarget.coerceAtLeast(1),
                                    allocations = finalAllocations,
                                    existingId = if (isEditMode) distributionId else null,
                                    onSaved = onSaved
                                )
                            }
                        },
                        modifier = Modifier
                            .width(205.dp)
                            .testTag("save_distribution_button")
                    )
                }
            }
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

            // Compute running startPara for each person in Local Qur'an rooms so user sees exact Paras at setup!
            val runningStartParas = remember(allocations.toList()) {
                val starts = mutableListOf<Int>()
                var cursor = 1
                allocations.forEach { item ->
                    starts.add(cursor)
                    cursor += item.count
                }
                starts
            }

            LazyColumn(
                contentPadding = PaddingValues(
                    start = horizontalPad,
                    end = horizontalPad,
                    top = 18.dp,
                    bottom = 110.dp
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Glowing Mode & Format Summary Banner (with optional expandable switcher)
                item(key = "wizard_mode_summary_card") {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.6.dp, activeAccent.copy(alpha = 0.7f)),
                        tonalElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 10.dp,
                                shape = RoundedCornerShape(24.dp),
                                ambientColor = activeGlow,
                                spotColor = activeGlow
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            activeAccent.copy(alpha = 0.12f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(activeAccent.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isOnlineMode) Icons.Default.Public else Icons.Default.Smartphone,
                                            contentDescription = null,
                                            tint = activeAccent,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Column {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = activeAccent.copy(alpha = 0.16f)
                                        ) {
                                            Text(
                                                text = if (isOnlineMode) "ONLINE ROOM • GOLD GLOW" else "LOCAL ROOM • BLUE GLOW",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 0.6.sp,
                                                    fontSize = 10.sp
                                                ),
                                                color = activeAccent,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = when (selectedType) {
                                                DistributionType.KHATM -> "Monthly Rotating 30-Para Khatm"
                                                DistributionType.ONE_TIME_KHATM -> "One-Time 30-Para Qur'an Khatm"
                                                DistributionType.AMAL -> "Custom Amal Target Distribution"
                                            },
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 17.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                if (!isEditMode) {
                                    Surface(
                                        onClick = { showModeSwitcherInForm = !showModeSwitcherInForm },
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Text(
                                            text = if (showModeSwitcherInForm) "Hide Options" else "Change",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = if (isOnlineMode) {
                                    "Instant Online Setup: Name your room below and create it. You'll get a unique Passcode & QR Code so members can join from their own phones and claim their own portions."
                                } else {
                                    "Local Coordinator Setup: Name each person in your group right below and assign their Paras or Amal counts now. You can share the formatted schedule anytime."
                                },
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Expandable Spacious Mode & Type Switcher (if user taps "Change")
                            AnimatedVisibility(visible = showModeSwitcherInForm && !isEditMode) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    Text(
                                        text = "1. ROOM STYLE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.8.sp
                                        ),
                                        color = activeAccent
                                    )

                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        SpaciousSetupOptionCard(
                                            title = "Online Room (Gold Glow • Passcode Sync)",
                                            subtitle = "Members join via Passcode/QR and claim their own share",
                                            selected = selectedRoomMode == RoomMode.ONLINE,
                                            accentColor = OnlineGoldAccent,
                                            onClick = { selectedRoomMode = RoomMode.ONLINE }
                                        )
                                        SpaciousSetupOptionCard(
                                            title = "Local Room (Blue Glow • Name Roster Now)",
                                            subtitle = "You name each person and assign their share during setup",
                                            selected = selectedRoomMode == RoomMode.LOCAL,
                                            accentColor = LocalBlueAccent,
                                            onClick = { selectedRoomMode = RoomMode.LOCAL }
                                        )
                                    }

                                    Text(
                                        text = "2. DISTRIBUTION TYPE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.8.sp
                                        ),
                                        color = activeAccent
                                    )

                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        SpaciousSetupOptionCard(
                                            title = "Monthly Qur'an Khatm (30 Paras)",
                                            subtitle = "Rotates everyone's contiguous Paras automatically every month",
                                            selected = selectedType == DistributionType.KHATM,
                                            accentColor = activeAccent,
                                            onClick = {
                                                selectedType = DistributionType.KHATM
                                                totalTargetText = "30"
                                            }
                                        )
                                        SpaciousSetupOptionCard(
                                            title = "One-Time Qur'an Khatm (30 Paras)",
                                            subtitle = "Complete all 30 Paras once for a special occasion",
                                            selected = selectedType == DistributionType.ONE_TIME_KHATM,
                                            accentColor = activeAccent,
                                            onClick = {
                                                selectedType = DistributionType.ONE_TIME_KHATM
                                                totalTargetText = "30"
                                            }
                                        )
                                        SpaciousSetupOptionCard(
                                            title = "Custom Amal (Target Count)",
                                            subtitle = "Surah Yaseen, Salawat, Tasbih, or custom recitation goal",
                                            selected = selectedType == DistributionType.AMAL,
                                            accentColor = activeAccent,
                                            onClick = {
                                                selectedType = DistributionType.AMAL
                                                if (totalTargetText == "30") totalTargetText = "110"
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Session Title & Target Count Card
                item(key = "session_identity_card") {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = if (isOnlineMode) "Online Room Details" else "Local Schedule Details",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            val isNameError = hasAttemptedSave && distributionName.trim().isBlank()

                            AnimatedVisibility(visible = hasAttemptedSave && validationErrors.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.error),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("form_validation_error_banner")
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ErrorOutline,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "Form Incomplete • Action Required:",
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                        validationErrors.forEach { errorMsg ->
                                            Row(
                                                verticalAlignment = Alignment.Top,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "•",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                                Text(
                                                    text = errorMsg,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = distributionName,
                                onValueChange = {
                                    distributionName = it
                                    if (it.isNotBlank()) hasAttemptedSave = false
                                },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            when {
                                                isAmalMode -> "Amal Name"
                                                isOneTimeKhatmMode -> "One-Time Khatm Title"
                                                else -> "Monthly Khatm Title"
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("*", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.ExtraBold)
                                    }
                                },
                                placeholder = {
                                    Text(
                                        when {
                                            isAmalMode -> "e.g. Friday Salawat Circle (Compulsory)"
                                            isOneTimeKhatmMode -> "e.g. Blessed Ramadan Khatm (Compulsory)"
                                            else -> "e.g. Household Monthly Khatm (Compulsory)"
                                        }
                                    )
                                },
                                isError = isNameError,
                                supportingText = {
                                    if (isNameError) {
                                        Text(
                                            text = "Room Title is compulsory. Please enter a name to continue.",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = activeAccent.copy(alpha = 0.16f)
                                            ) {
                                                Text(
                                                    text = "COMPULSORY *",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 9.sp,
                                                        letterSpacing = 0.5.sp
                                                    ),
                                                    color = activeAccent,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "A descriptive name for your group",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                trailingIcon = {
                                    if (isNameError) {
                                        Icon(
                                            imageVector = Icons.Default.ErrorOutline,
                                            contentDescription = "Room name is required",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    } else if (distributionName.trim().isNotBlank()) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Valid name",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next
                                ),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = activeAccent,
                                    unfocusedBorderColor = if (isNameError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
                                    errorBorderColor = MaterialTheme.colorScheme.error,
                                    focusedContainerColor = MaterialTheme.colorScheme.background,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.background
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("distribution_name_input")
                            )

                            if (isAmalMode) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = totalTargetText,
                                        onValueChange = { valClean ->
                                            if (valClean.all { it.isDigit() } && valClean.length <= 7) {
                                                totalTargetText = valClean
                                            }
                                        },
                                        label = { Text("Total Target Recitations") },
                                        placeholder = { Text("e.g. 110, 313, 1000") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Done
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = activeAccent,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                            focusedContainerColor = MaterialTheme.colorScheme.background,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("amal_target_input")
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Presets:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        listOf(40, 100, 110, 313, 1000).forEach { preset ->
                                            val selected = effectiveTarget == preset
                                            FilterChip(
                                                selected = selected,
                                                onClick = { totalTargetText = preset.toString() },
                                                label = {
                                                    Text(
                                                        text = preset.toString(),
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    )
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = activeAccent.copy(alpha = 0.2f),
                                                    selectedLabelColor = activeAccent
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3A. ONLINE ROOM EXPERIENCE (When creating a new Online Room)
                if (isOnlineMode && !isEditMode) {
                    item(key = "online_room_passcode_preview_card") {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.2.dp, OnlineGoldAccent.copy(alpha = 0.55f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(22.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(OnlineGoldAccent.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCode2,
                                            contentDescription = null,
                                            tint = OnlineGoldAccent,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Self-Service Member Claiming",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "No need to type everyone's name yourself",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = when (selectedType) {
                                        DistributionType.ONE_TIME_KHATM ->
                                            "All 30 Paras will open inside your Online Room. Share the generated Passcode or QR Code on WhatsApp so members can pick their own Para(s) first-come, first-served."
                                        DistributionType.KHATM ->
                                            "Members join with your Room Passcode or QR Code to claim their monthly Para count, or you can add elderly guests inside the room."
                                        DistributionType.AMAL ->
                                            "Members join with your Room Passcode or QR Code, pledge how many recitations they'll read, and use the built-in tactile counter."
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                // Optional Host Self-Claim Toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Claim my own share now (Optional)",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Pre-claim your own portion as the room creator",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = hostWantsInitialClaim,
                                        onCheckedChange = { hostWantsInitialClaim = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = OnlineGoldAccent
                                        )
                                    )
                                }

                                AnimatedVisibility(visible = hostWantsInitialClaim) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        val isHostClaimNameError = hasAttemptedSave && hostClaimName.trim().isBlank()

                                        OutlinedTextField(
                                            value = hostClaimName,
                                            onValueChange = { hostClaimName = it },
                                            label = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("Your Display Name")
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("*", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.ExtraBold)
                                                }
                                            },
                                            placeholder = { Text("e.g. Abbas (Compulsory)") },
                                            isError = isHostClaimNameError,
                                            supportingText = {
                                                if (isHostClaimNameError) {
                                                    Text(
                                                        text = "Display name is compulsory when pre-claiming a share",
                                                        color = MaterialTheme.colorScheme.error,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                                    )
                                                } else {
                                                    Text(
                                                        text = "Shown on your assigned Para / Recitation share",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            },
                                            trailingIcon = {
                                                if (isHostClaimNameError) {
                                                    Icon(
                                                        imageVector = Icons.Default.ErrorOutline,
                                                        contentDescription = "Name required",
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                } else if (hostClaimName.trim().isNotBlank()) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = "Valid",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            },
                                            singleLine = true,
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = OnlineGoldAccent,
                                                unfocusedBorderColor = if (isHostClaimNameError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
                                                errorBorderColor = MaterialTheme.colorScheme.error,
                                                focusedContainerColor = MaterialTheme.colorScheme.background,
                                                unfocusedContainerColor = MaterialTheme.colorScheme.background
                                            )
                                        )

                                        if (isAmalMode) {
                                            OutlinedTextField(
                                                value = hostClaimCountText,
                                                onValueChange = { v ->
                                                    if (v.all { it.isDigit() } && v.length <= 6) {
                                                        hostClaimCountText = v
                                                    }
                                                },
                                                label = { Text("Your Recitation Share") },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        } else {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Text(
                                                    text = "Your Paras:",
                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                listOf(1, 2, 3, 5).forEach { countPreset ->
                                                    FilterChip(
                                                        selected = hostClaimCount == countPreset,
                                                        onClick = { hostClaimCount = countPreset },
                                                        label = {
                                                            Text(
                                                                "$countPreset ${if (countPreset == 1) "Para" else "Paras"}",
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        },
                                                        colors = FilterChipDefaults.filterChipColors(
                                                            selectedContainerColor = OnlineGoldAccent.copy(alpha = 0.2f),
                                                            selectedLabelColor = OnlineGoldAccent
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // 3B. LOCAL ROOM FULL ROSTER SETUP (Or Editing an Existing Room)
                    item(key = "local_allocation_tracker_card") {
                        val progress = if (effectiveTarget > 0) {
                            (currentSum.toFloat() / effectiveTarget.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        val statusColor = when {
                            effectiveTarget <= 0 -> MaterialTheme.colorScheme.error
                            currentSum == effectiveTarget -> MaterialTheme.colorScheme.primary
                            currentSum > effectiveTarget -> MaterialTheme.colorScheme.error
                            else -> LocalBlueAccent
                        }

                        val unitLabel = if (isAmalMode) "Recitations" else "Paras"

                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.3.dp, statusColor.copy(alpha = 0.55f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(22.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "STEP 2 • NAME & ASSIGN EVERY PERSON",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.8.sp
                                            ),
                                            color = statusColor
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "$currentSum / $effectiveTarget $unitLabel Assigned",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 20.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Icon(
                                        imageVector = if (currentSum == effectiveTarget && effectiveTarget > 0) {
                                            Icons.Default.CheckCircle
                                        } else {
                                            Icons.Default.ErrorOutline
                                        },
                                        contentDescription = null,
                                        tint = statusColor,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { progress },
                                    color = statusColor,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(9.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )

                                Text(
                                    text = when {
                                        remainingUnits == 0 ->
                                            "All $effectiveTarget $unitLabel are assigned! You're ready to save this Local Schedule."
                                        remainingUnits > 0 ->
                                            "Add people below until all $effectiveTarget $unitLabel are assigned ($remainingUnits remaining)."
                                        else ->
                                            "Over-allocated by ${-remainingUnits} $unitLabel. Reduce someone's share or tap Distribute Equally."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (allocations.isNotEmpty() && currentSum != effectiveTarget && effectiveTarget >= allocations.size) {
                                    SecondaryEditorialButton(
                                        text = "Distribute All $effectiveTarget $unitLabel Equally (${allocations.size} People)",
                                        icon = Icons.Default.AutoFixHigh,
                                        onClick = distributeRemainingEquallyAction,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("distribute_equally_button")
                                    )
                                }
                            }
                        }
                    }

                    // 4. Add Person Card for Local Room Setup
                    item(key = "add_person_builder_card") {
                        val nextStartPara = (currentSum + 1).coerceAtMost(30)
                        val nextEndPara = (nextStartPara + newParticipantCount - 1).coerceAtMost(30)
                        val paraPreviewBadge = if (!isAmalMode && currentSum < 30) {
                            if (nextStartPara == nextEndPara) {
                                "Will receive: Para $nextStartPara"
                            } else {
                                "Will receive: Paras $nextStartPara–$nextEndPara"
                            }
                        } else null

                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.2.dp, LocalBlueAccent.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(22.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(LocalBlueAccent.copy(alpha = 0.16f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PersonAdd,
                                                contentDescription = null,
                                                tint = LocalBlueAccent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Text(
                                            text = "Name a Person & Assign Share",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    if (paraPreviewBadge != null) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = LocalBlueAccent.copy(alpha = 0.14f)
                                        ) {
                                            Text(
                                                text = paraPreviewBadge,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = LocalBlueAccent,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = newParticipantName,
                                    onValueChange = {
                                        newParticipantName = it
                                        if (it.isNotBlank()) participantInputError = null
                                    },
                                    label = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Person's Name")
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("*", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.ExtraBold)
                                        }
                                    },
                                    placeholder = { Text("e.g. Abbas, Khadija, Dadi (Compulsory)") },
                                    isError = participantInputError != null,
                                    supportingText = {
                                        if (participantInputError != null) {
                                            Text(
                                                text = participantInputError!!,
                                                color = MaterialTheme.colorScheme.error,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                            )
                                        } else {
                                            Text(
                                                text = "Required: Enter the participant's name",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    trailingIcon = {
                                        if (participantInputError != null) {
                                            Icon(
                                                imageVector = Icons.Default.ErrorOutline,
                                                contentDescription = "Name required",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        } else if (newParticipantName.trim().isNotBlank()) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Valid",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Words,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { addLocalParticipantAction() }
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = LocalBlueAccent,
                                        unfocusedBorderColor = if (participantInputError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
                                        errorBorderColor = MaterialTheme.colorScheme.error,
                                        focusedContainerColor = MaterialTheme.colorScheme.background,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.background
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("new_participant_name_input")
                                )

                                if (isAmalMode) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = newParticipantCountText,
                                            onValueChange = { v ->
                                                if (v.all { it.isDigit() } && v.length <= 6) {
                                                    newParticipantCountText = v
                                                }
                                            },
                                            label = { Text("Recitation Count") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Number,
                                                imeAction = ImeAction.Done
                                            ),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("new_participant_amal_count_input")
                                        )

                                        if (remainingUnits > 0) {
                                            SecondaryEditorialButton(
                                                text = "Remaining ($remainingUnits)",
                                                onClick = { newParticipantCountText = remainingUnits.toString() }
                                            )
                                        }
                                    }
                                } else {
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(1, 2, 3, 5).forEach { countOption ->
                                            FilterChip(
                                                selected = newParticipantCount == countOption,
                                                onClick = { newParticipantCount = countOption },
                                                label = {
                                                    Text(
                                                        text = "$countOption ${if (countOption == 1) "Para" else "Paras"}",
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = LocalBlueAccent.copy(alpha = 0.2f),
                                                    selectedLabelColor = LocalBlueAccent
                                                )
                                            )
                                        }
                                        if (remainingUnits in 1..30 && remainingUnits !in listOf(1, 2, 3, 5)) {
                                            FilterChip(
                                                selected = newParticipantCount == remainingUnits,
                                                onClick = { newParticipantCount = remainingUnits },
                                                label = {
                                                    Text(
                                                        text = "Remaining ($remainingUnits)",
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = LocalBlueAccent.copy(alpha = 0.2f),
                                                    selectedLabelColor = LocalBlueAccent
                                                )
                                            )
                                        }
                                    }
                                }

                                PrimarySanctuaryButton(
                                    text = "Add Person to Schedule",
                                    icon = Icons.Default.Add,
                                    enabled = newParticipantName.isNotBlank(),
                                    onClick = addLocalParticipantAction,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("add_participant_button")
                                )
                            }
                        }
                    }

                    // 5. Named Roster List Header
                    item(key = "participants_roster_header") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Named Participants (${allocations.size})",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = if (isAmalMode) "Tap - / + to adjust count" else "Ascending Para order (1 → 30)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (allocations.isEmpty()) {
                        item(key = "empty_participants_hint") {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isAmalMode) {
                                        "No participants named yet. Add each person above until all $effectiveTarget recitations are assigned."
                                    } else {
                                        "No participants named yet. Add each reader above until all 30 Paras are assigned."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = allocations,
                            key = { index, item -> "${item.name}_$index" }
                        ) { index, item ->
                            val startPara = runningStartParas.getOrElse(index) { 1 }
                            val endPara = startPara + item.count - 1
                            val paraRangePreview = if (!isAmalMode) {
                                val list = (startPara..endPara).map { ((it - 1) % 30) + 1 }
                                "Para ${list.joinToString(", ")}"
                            } else {
                                "${item.count} Recitations"
                            }

                            ParticipantAllocationRowCard(
                                index = index + 1,
                                allocation = item,
                                paraPreviewLabel = paraRangePreview,
                                isAmal = isAmalMode,
                                maxCount = if (isAmalMode) effectiveTarget.coerceAtLeast(1000) else 30,
                                accentColor = activeAccent,
                                onIncrement = {
                                    val maxAllowed = if (isAmalMode) 100000 else 30
                                    if (item.count < maxAllowed) {
                                        allocations[index] = item.copy(count = item.count + 1)
                                    }
                                },
                                onDecrement = {
                                    if (item.count > 1) {
                                        allocations[index] = item.copy(count = item.count - 1)
                                    }
                                },
                                onRemove = {
                                    allocations.removeAt(index)
                                }
                            )
                        }
                    }
                }

                item(key = "bottom_ornament") {
                    ManuscriptOrnamentDivider(
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SpaciousSetupOptionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) accentColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.background,
        border = BorderStroke(
            width = if (selected) 1.8.dp else 1.dp,
            color = if (selected) accentColor else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ParticipantAllocationRowCard(
    index: Int,
    allocation: ParticipantAllocation,
    paraPreviewLabel: String,
    isAmal: Boolean,
    maxCount: Int,
    accentColor: Color,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.38f)),
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
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = index.toString(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = accentColor
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = allocation.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = paraPreviewLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = accentColor
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onDecrement,
                    enabled = allocation.count > 1,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease count",
                        modifier = Modifier.size(18.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = if (isAmal) "${allocation.count}" else "${allocation.count}P",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                IconButton(
                    onClick = onIncrement,
                    enabled = allocation.count < maxCount,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase count",
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Remove ${allocation.name}",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
