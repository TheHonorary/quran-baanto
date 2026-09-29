package com.example.ui.distribution

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.db.DistributionType
import com.example.domain.ParticipantAllocation
import com.example.domain.RotationEngine
import com.example.ui.components.PrimarySanctuaryButton
import com.example.ui.components.ResizableSectionContainer
import com.example.ui.components.SanctuaryTopHeader
import com.example.ui.components.SecondaryEditorialButton
import com.example.ui.viewmodel.DistributionViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateEditDistributionScreen(
    distributionViewModel: DistributionViewModel,
    existingDistributionId: Long? = null,
    initialType: DistributionType = DistributionType.KHATM,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeState by distributionViewModel.activeDistributionState.collectAsState()

    var distributionType by remember { mutableStateOf(initialType) }
    var distributionName by remember { mutableStateOf("") }
    var totalTargetText by remember { mutableStateOf(if (initialType == DistributionType.AMAL) "110" else "30") }
    val participants = remember { mutableStateListOf<ParticipantAllocation>() }

    // Load existing distribution data if editing, or initialize default balanced members
    LaunchedEffect(existingDistributionId, activeState.distribution) {
        if (existingDistributionId != null && activeState.distribution?.id == existingDistributionId) {
            val dist = activeState.distribution!!
            distributionType = dist.type
            distributionName = dist.name
            totalTargetText = dist.totalTarget.toString()
            participants.clear()
            activeState.participants.forEach {
                participants.add(
                    ParticipantAllocation(
                        id = it.id,
                        name = it.name,
                        count = it.allocatedCount,
                        displayOrder = it.displayOrder
                    )
                )
            }
        } else if (existingDistributionId == null && participants.isEmpty()) {
            if (initialType == DistributionType.AMAL) {
                distributionName = "Surah Yaseen Circle"
                totalTargetText = "110"
                val sizes = listOf(22, 22, 22, 22, 22)
                sizes.forEachIndexed { idx, c ->
                    participants.add(
                        ParticipantAllocation(
                            name = "Member ${idx + 1}",
                            count = c,
                            displayOrder = idx + 1
                        )
                    )
                }
            } else {
                distributionName = "Family Khatm"
                totalTargetText = "30"
                for (i in 1..5) {
                    participants.add(
                        ParticipantAllocation(
                            name = "Member $i",
                            count = 6,
                            displayOrder = i
                        )
                    )
                }
            }
        }
    }

    val isAmal = distributionType == DistributionType.AMAL
    val targetCount = if (isAmal) (totalTargetText.toIntOrNull() ?: 0) else 30
    val totalAllocated = participants.sumOf { it.count }
    val isExactMatch = targetCount > 0 && totalAllocated == targetCount && participants.isNotEmpty()
    val canSave = distributionName.isNotBlank() && isExactMatch &&
        participants.all { it.name.isNotBlank() && it.count > 0 } &&
        (!isAmal && participants.size <= 30 || isAmal)

    // Precompute live Month 1 contiguous Para range for each participant in Khatm mode
    val previewRanges = remember(participants.toList(), isAmal) {
        if (isAmal) emptyList() else RotationEngine.previewInitialRanges(participants)
    }

    val screenTitle = if (existingDistributionId == null) {
        if (isAmal) "New Custom Amal" else "New Qur'an Khatm"
    } else {
        if (isAmal) "Edit Custom Amal" else "Edit Qur'an Khatm"
    }

    Scaffold(
        topBar = {
            SanctuaryTopHeader(
                title = screenTitle,
                onBack = onBack
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    if (!isExactMatch) {
                        val unitLabel = if (isAmal) "recitation" else "Para"
                        val unitsLabel = if (isAmal) "recitations" else "Paras"
                        val diff = targetCount - totalAllocated
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (diff > 0) {
                                            "Assign $diff more ${if (diff == 1) unitLabel else unitsLabel} to complete $targetCount."
                                        } else {
                                            "Reduce by ${-diff} ${if (-diff == 1) unitLabel else unitsLabel} to match $targetCount."
                                        },
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                if (participants.isNotEmpty() && targetCount > 0) {
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Surface(
                                        onClick = { distributeEvenly(participants, targetCount) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            text = "Balance",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    PrimarySanctuaryButton(
                        text = if (existingDistributionId == null) {
                            if (isAmal) "Create Amal Session" else "Save Khatm Session"
                        } else "Save Changes",
                        icon = Icons.Default.Check,
                        enabled = canSave,
                        onClick = {
                            if (canSave) {
                                distributionViewModel.saveDistribution(
                                    name = distributionName.trim(),
                                    type = distributionType,
                                    totalTarget = targetCount,
                                    allocations = participants,
                                    existingId = existingDistributionId
                                )
                                onBack()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
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
            val horizontalPad = if (maxWidth >= 640.dp) 32.dp else 22.dp

            LazyColumn(
                contentPadding = PaddingValues(
                    top = 24.dp,
                    bottom = 36.dp,
                    start = horizontalPad,
                    end = horizontalPad
                ),
                verticalArrangement = Arrangement.spacedBy(22.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                // 1. Session Identity & Mode Container
                item(key = "session_details_card") {
                    ResizableSectionContainer(
                        title = "Session Details",
                        contentPadding = PaddingValues(24.dp)
                    ) {
                        if (existingDistributionId == null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 18.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ModeTogglePill(
                                    label = "Qur'an Khatm",
                                    selected = !isAmal,
                                    onClick = {
                                        if (isAmal) {
                                            distributionType = DistributionType.KHATM
                                            totalTargetText = "30"
                                            if (distributionName == "Surah Yaseen Circle") {
                                                distributionName = "Family Khatm"
                                            }
                                            distributeEvenly(participants, 30)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                ModeTogglePill(
                                    label = "Custom Amal",
                                    selected = isAmal,
                                    onClick = {
                                        if (!isAmal) {
                                            distributionType = DistributionType.AMAL
                                            totalTargetText = "110"
                                            if (distributionName == "Family Khatm") {
                                                distributionName = "Surah Yaseen Circle"
                                            }
                                            distributeEvenly(participants, 110)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = distributionName,
                            onValueChange = { distributionName = it },
                            label = { Text(if (isAmal) "Amal Title" else "Khatm Name") },
                            placeholder = {
                                Text(if (isAmal) "e.g. Surah Yaseen" else "e.g. Family Khatm")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedContainerColor = MaterialTheme.colorScheme.background,
                                unfocusedContainerColor = MaterialTheme.colorScheme.background
                            )
                        )

                        if (isAmal) {
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = totalTargetText,
                                onValueChange = { input ->
                                    totalTargetText = input.filter { it.isDigit() }
                                },
                                label = { Text("Total Recitations Target") },
                                placeholder = { Text("e.g. 110, 40, 100, 1000") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    focusedContainerColor = MaterialTheme.colorScheme.background,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.background
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(40, 70, 100, 110, 313, 1000).forEach { target ->
                                    FilterChip(
                                        selected = targetCount == target,
                                        onClick = {
                                            totalTargetText = target.toString()
                                            distributeEvenly(participants, target)
                                        },
                                        label = { Text("$target") },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Group Size & Dedicated Action Buttons Container
                item(key = "group_controls_card") {
                    ResizableSectionContainer(
                        title = "Group Setup",
                        badgeText = "$totalAllocated / $targetCount",
                        badgeColor = if (isExactMatch) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.secondaryContainer,
                        badgeContentColor = if (isExactMatch) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSecondaryContainer,
                        contentPadding = PaddingValues(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Number of Members",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (participants.size > 1) {
                                                participants.removeAt(participants.lastIndex)
                                                distributeEvenly(participants, targetCount)
                                            }
                                        },
                                        enabled = participants.size > 1,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = "Decrease group size",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = "${participants.size}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    )
                                    IconButton(
                                        onClick = {
                                            val maxMembers = if (isAmal) 100 else 30
                                            if (participants.size < maxMembers) {
                                                val nextNum = participants.size + 1
                                                participants.add(
                                                    ParticipantAllocation(
                                                        name = "Member $nextNum",
                                                        count = 1,
                                                        displayOrder = nextNum
                                                    )
                                                )
                                                distributeEvenly(participants, targetCount)
                                            }
                                        },
                                        enabled = participants.size < (if (isAmal) 100 else 30),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Increase group size",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Each button in its own designated clean space
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            SecondaryEditorialButton(
                                text = "Balance Equally",
                                icon = Icons.Default.Balance,
                                onClick = {
                                    if (participants.isNotEmpty() && targetCount > 0) {
                                        distributeEvenly(participants, targetCount)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            PrimarySanctuaryButton(
                                text = "Add Member",
                                icon = Icons.Default.PersonAdd,
                                onClick = {
                                    val defaultCount = if (isAmal) {
                                        val remaining = targetCount - totalAllocated
                                        if (remaining > 0) remaining else 1
                                    } else {
                                        val remaining = 30 - totalAllocated
                                        if (remaining > 0) remaining else 1
                                    }
                                    participants.add(
                                        ParticipantAllocation(
                                            name = "Member ${participants.size + 1}",
                                            count = defaultCount,
                                            displayOrder = participants.size + 1
                                        )
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 3. Members List Section Header
                item(key = "members_header") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Text(
                            text = "Members (${participants.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(
                            thickness = 0.8.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
                        )
                    }
                }

                // 4. Individual Member Cards with generous breathing room
                itemsIndexed(
                    items = participants,
                    key = { _, item -> item.uid }
                ) { index, participant ->
                    val rangePreview = previewRanges.getOrNull(index)
                    ParticipantRowItem(
                        orderNumber = index + 1,
                        participant = participant,
                        isAmal = isAmal,
                        contiguousRangePreview = rangePreview,
                        onNameChange = { newName ->
                            participants[index] = participant.copy(name = newName)
                        },
                        onCountChange = { newCount ->
                            val maxCount = if (isAmal) maxOf(targetCount, 10000) else 30
                            val clamped = newCount.coerceIn(1, maxCount)
                            participants[index] = participant.copy(count = clamped)
                        },
                        onMoveUp = if (index > 0) {
                            {
                                val item = participants.removeAt(index)
                                participants.add(index - 1, item)
                            }
                        } else null,
                        onMoveDown = if (index < participants.size - 1) {
                            {
                                val item = participants.removeAt(index)
                                participants.add(index + 1, item)
                            }
                        } else null,
                        onRemove = {
                            if (participants.size > 1) {
                                participants.removeAt(index)
                            }
                        },
                        canDelete = participants.size > 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeTogglePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier.height(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun distributeEvenly(
    participants: SnapshotStateList<ParticipantAllocation>,
    targetTotal: Int
) {
    val count = participants.size
    if (count == 0 || targetTotal <= 0) return
    val base = targetTotal / count
    val remainder = targetTotal % count

    for (i in 0 until count) {
        val allocated = base + if (i < remainder) 1 else 0
        participants[i] = participants[i].copy(count = maxOf(allocated, 1))
    }
}

@Composable
private fun ParticipantRowItem(
    orderNumber: Int,
    participant: ParticipantAllocation,
    isAmal: Boolean,
    contiguousRangePreview: IntRange?,
    onNameChange: (String) -> Unit,
    onCountChange: (Int) -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onRemove: () -> Unit,
    canDelete: Boolean,
    modifier: Modifier = Modifier
) {
    var showManualInputDialog by remember { mutableStateOf(false) }

    if (showManualInputDialog) {
        var typedValue by remember { mutableStateOf(participant.count.toString()) }
        Dialog(onDismissRequest = { showManualInputDialog = false }) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = participant.name.ifBlank { "Member $orderNumber" },
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isAmal) "Enter target recitations for this member:"
                        else "Enter contiguous Para count (1–30):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = typedValue,
                        onValueChange = { input ->
                            typedValue = input.filter { it.isDigit() }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SecondaryEditorialButton(
                            text = "Cancel",
                            onClick = { showManualInputDialog = false },
                            modifier = Modifier.weight(1f)
                        )
                        PrimarySanctuaryButton(
                            text = "Apply",
                            onClick = {
                                val entered = typedValue.toIntOrNull()
                                if (entered != null && entered > 0) {
                                    onCountChange(entered)
                                }
                                showManualInputDialog = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp)
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
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "#$orderNumber",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    if (!isAmal && contiguousRangePreview != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "Para ${contiguousRangePreview.toList().joinToString(", ")}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { onMoveUp?.invoke() },
                        enabled = onMoveUp != null,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Move Up",
                            tint = if (onMoveUp != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { onMoveDown?.invoke() },
                        enabled = onMoveDown != null,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Move Down",
                            tint = if (onMoveDown != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (canDelete) {
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove participant",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = participant.name,
                    onValueChange = onNameChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Member Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background
                    )
                )

                Spacer(modifier = Modifier.width(12.dp))

                val stepDelta = if (isAmal && participant.count >= 20) 5 else 1
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        IconButton(
                            onClick = { onCountChange(participant.count - stepDelta) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrease",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Surface(
                            onClick = { showManualInputDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = if (isAmal) "${participant.count}" else "${participant.count} ${if (participant.count == 1) "Para" else "Paras"}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }

                        IconButton(
                            onClick = { onCountChange(participant.count + stepDelta) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
