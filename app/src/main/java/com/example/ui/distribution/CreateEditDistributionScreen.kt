package com.example.ui.distribution

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.DistributionType
import com.example.domain.ParticipantAllocation
import com.example.ui.viewmodel.DistributionViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

    // Load existing distribution data if editing
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
                distributionName = "Surah Yaseen"
                totalTargetText = "110"
                participants.add(ParticipantAllocation(name = "Dad", count = 20, displayOrder = 1))
                participants.add(ParticipantAllocation(name = "Mom", count = 20, displayOrder = 2))
                participants.add(ParticipantAllocation(name = "Hamza", count = 20, displayOrder = 3))
                participants.add(ParticipantAllocation(name = "Sara", count = 20, displayOrder = 4))
                participants.add(ParticipantAllocation(name = "Tariq", count = 30, displayOrder = 5))
            } else {
                distributionName = "Family Khatm"
                totalTargetText = "30"
                participants.add(ParticipantAllocation(name = "Dad", count = 6, displayOrder = 1))
                participants.add(ParticipantAllocation(name = "Mom", count = 6, displayOrder = 2))
                participants.add(ParticipantAllocation(name = "Hamza", count = 6, displayOrder = 3))
                participants.add(ParticipantAllocation(name = "Sara", count = 6, displayOrder = 4))
                participants.add(ParticipantAllocation(name = "Tariq", count = 6, displayOrder = 5))
            }
        }
    }

    val isAmal = distributionType == DistributionType.AMAL
    val targetCount = if (isAmal) (totalTargetText.toIntOrNull() ?: 0) else 30
    val totalAllocated = participants.sumOf { it.count }
    val isExactMatch = targetCount > 0 && totalAllocated == targetCount && participants.isNotEmpty()
    val canSave = distributionName.isNotBlank() && isExactMatch && participants.all { it.name.isNotBlank() && it.count > 0 }

    val screenTitle = if (existingDistributionId == null) {
        if (isAmal) "New Amal" else "New Khatm"
    } else {
        if (isAmal) "Edit Amal" else "Edit Khatm"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = screenTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    if (!isExactMatch) {
                        val unitLabel = if (isAmal) "recitation" else "Para"
                        val unitsLabel = if (isAmal) "recitations" else "Paras"
                        val diff = targetCount - totalAllocated
                        Text(
                            text = if (diff > 0) {
                                "⚠️ Assign $diff more ${if (diff == 1) unitLabel else unitsLabel} to match total ($targetCount)."
                            } else {
                                "⚠️ Over-allocated by ${-diff} ${if (-diff == 1) unitLabel else unitsLabel}. Total must equal $targetCount."
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Button(
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
                        enabled = canSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = if (existingDistributionId == null) {
                                if (isAmal) "Create Amal ($targetCount Recitations)" else "Create Khatm (30 Paras)"
                            } else "Save Changes",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp,
                start = 20.dp,
                end = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Distribution Name Field
            item {
                OutlinedTextField(
                    value = distributionName,
                    onValueChange = { distributionName = it },
                    label = { Text(if (isAmal) "Amal Name" else "Group / Khatm Name") },
                    placeholder = {
                        Text(if (isAmal) "e.g. Surah Yaseen, Ayat al-Kursi" else "e.g. Family Khatm, Ramadan Circle")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            // For Amal: Total Recitations Input & Preset Chips
            if (isAmal) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 0.8.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Total Recitations Target",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = totalTargetText,
                                onValueChange = { input ->
                                    val filtered = input.filter { it.isDigit() }
                                    totalTargetText = filtered
                                },
                                label = { Text("Total Recitations") },
                                placeholder = { Text("e.g. 110, 40, 100") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    focusedContainerColor = MaterialTheme.colorScheme.background,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.background
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Common Targets",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(40, 70, 100, 110, 300, 1000).forEach { preset ->
                                    FilterChip(
                                        selected = targetCount == preset,
                                        onClick = { totalTargetText = preset.toString() },
                                        label = { Text("$preset") },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            labelColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Allocation Live Progress Banner
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isExactMatch) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 0.8.dp,
                        color = if (isExactMatch) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ALLOCATION STATUS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    fontSize = 10.sp
                                ),
                                color = if (isExactMatch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val unitStr = if (isAmal) "recitations" else "Paras"
                                Text(
                                    text = "$totalAllocated / $targetCount $unitStr assigned",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (isExactMatch) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Matched",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Auto divide evenly button
                        if (participants.isNotEmpty() && targetCount > 0) {
                            OutlinedButton(
                                onClick = {
                                    distributeEvenly(participants, targetCount)
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Auto Divide", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Quick Preset Templates for Khatm (if starting new Khatm)
            if (!isAmal && existingDistributionId == null) {
                item {
                    Column {
                        Text(
                            text = "Quick Presets",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PresetChip("5 people (6 each)") {
                                applyPreset(participants, 5, 6)
                            }
                            PresetChip("6 people (5 each)") {
                                applyPreset(participants, 6, 5)
                            }
                            PresetChip("10 people (3 each)") {
                                applyPreset(participants, 10, 3)
                            }
                            PresetChip("30 people (1 each)") {
                                applyPreset(participants, 30, 1)
                            }
                        }
                    }
                }
            }

            // Participant Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Participants (${participants.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Button(
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
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Person", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Participant list items
            itemsIndexed(
                items = participants,
                key = { index, item -> item.id.toString() + "_" + index }
            ) { index, participant ->
                ParticipantRowItem(
                    orderNumber = index + 1,
                    participant = participant,
                    isAmal = isAmal,
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

@Composable
private fun PresetChip(
    label: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        shape = RoundedCornerShape(8.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            labelColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

private fun applyPreset(
    participants: SnapshotStateList<ParticipantAllocation>,
    count: Int,
    countPerPerson: Int
) {
    participants.clear()
    for (i in 1..count) {
        participants.add(
            ParticipantAllocation(
                name = "Member $i",
                count = countPerPerson,
                displayOrder = i
            )
        )
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
    onNameChange: (String) -> Unit,
    onCountChange: (Int) -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onRemove: () -> Unit,
    canDelete: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = 0.8.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Order number badge & re-order controls
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = { onMoveUp?.invoke() },
                    enabled = onMoveUp != null,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Move Up",
                        tint = if (onMoveUp != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(14.dp)
                    )
                }

                Text(
                    text = "#$orderNumber",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(
                    onClick = { onMoveDown?.invoke() },
                    enabled = onMoveDown != null,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Move Down",
                        tint = if (onMoveDown != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Name Field
            OutlinedTextField(
                value = participant.name,
                onNameChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Name") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Count Stepper & Input
            val stepDelta = if (isAmal && participant.count >= 20) 5 else 1
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(horizontal = 2.dp, vertical = 2.dp)
            ) {
                IconButton(
                    onClick = { onCountChange(participant.count - stepDelta) },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease",
                        modifier = Modifier.size(12.dp)
                    )
                }

                Text(
                    text = if (isAmal) "${participant.count}" else "${participant.count} p",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.padding(horizontal = 2.dp)
                )

                IconButton(
                    onClick = { onCountChange(participant.count + stepDelta) },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase",
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            if (canDelete) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove participant",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
