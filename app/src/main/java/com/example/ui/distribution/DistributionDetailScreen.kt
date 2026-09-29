package com.example.ui.distribution

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.db.DistributionType
import com.example.data.db.ParticipantEntity
import com.example.domain.ParaAssignment
import com.example.ui.components.ContiguousParaBlockBar
import com.example.ui.components.OctagonalNumberMedallion
import com.example.ui.components.PrimarySanctuaryButton
import com.example.ui.components.ResizableSectionContainer
import com.example.ui.components.SanctuaryGeometricGlyph
import com.example.ui.components.SanctuaryLoadingState
import com.example.ui.components.SanctuaryTopHeader
import com.example.ui.components.SecondaryEditorialButton
import com.example.ui.components.rememberAllocationPalette
import com.example.ui.viewmodel.DistributionViewModel
import com.example.util.MonthDateInfo
import java.util.Calendar

@Composable
fun DistributionDetailScreen(
    distributionId: Long,
    distributionViewModel: DistributionViewModel,
    onEditDistribution: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeState by distributionViewModel.activeDistributionState.collectAsState()

    var showShareDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(distributionId) {
        distributionViewModel.selectDistribution(distributionId)
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
    val currentCal = remember { Calendar.getInstance() }
    val isViewingCurrentMonth = activeState.selectedYear == currentCal.get(Calendar.YEAR) &&
        activeState.selectedMonth == (currentCal.get(Calendar.MONTH) + 1)

    // Strictly ordered 1 to 30, then by participant name
    val orderedAssignments = remember(activeState.assignments) {
        activeState.assignments.sortedWith(
            compareBy<ParaAssignment> { it.startPara }
                .thenBy { it.endPara }
                .thenBy { it.participantName.lowercase() }
        )
    }

    val allocationBlockSizes = remember(isAmal, activeState.participants, orderedAssignments) {
        if (isAmal) {
            activeState.participants.map { it.allocatedCount }
        } else {
            orderedAssignments.map { it.allocatedCount }
        }
    }

    val palette = rememberAllocationPalette()

    Scaffold(
        topBar = {
            SanctuaryTopHeader(
                title = dist.name,
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
                    top = 24.dp,
                    bottom = 48.dp,
                    start = horizontalPad,
                    end = horizontalPad
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Spacious Overview / Month Navigation Card + Multi-Color Allocation Line
                item(key = "overview_section") {
                    if (isAmal) {
                        AmalSummarySpecimenCard(
                            totalTarget = dist.totalTarget,
                            participantCount = activeState.participants.size,
                            blockSizes = allocationBlockSizes
                        )
                    } else {
                        MonthCycleNavigatorCard(
                            dateInfo = activeState.dateInfo,
                            isCurrentMonth = isViewingCurrentMonth,
                            blockSizes = allocationBlockSizes,
                            onPreviousMonth = { distributionViewModel.navigateMonth(-1) },
                            onNextMonth = { distributionViewModel.navigateMonth(1) },
                            onResetCurrent = { distributionViewModel.resetToCurrentMonth() }
                        )
                    }
                }

                // 2. Dedicated Primary Action Buttons Section (Share & Copy in their own clean space)
                item(key = "primary_actions_section") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        PrimarySanctuaryButton(
                            text = "Share Schedule",
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
                }

                // 3. Clean Schedule Section Header
                item(key = "schedule_header") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text(
                            text = if (isAmal) "Member Allocations" else "Monthly Schedule",
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

                // 4. Spacious Individual Member Allocation Cards
                if (isAmal) {
                    itemsIndexed(
                        items = activeState.participants,
                        key = { _, item -> item.id }
                    ) { index, participant ->
                        AmalParticipantCard(
                            index = index + 1,
                            participant = participant,
                            accentColor = palette[index % palette.size]
                        )
                    }
                } else {
                    itemsIndexed(
                        items = orderedAssignments,
                        key = { _, item -> "${item.startPara}_${item.participantId}" }
                    ) { index, assignment ->
                        KhatmAssignmentSpecimenCard(
                            assignment = assignment,
                            accentColor = palette[index % palette.size]
                        )
                    }
                }

                // 5. Dedicated Session Management Section (Edit & Delete in their own clean space)
                item(key = "session_management_section") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        HorizontalDivider(
                            thickness = 0.8.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            SecondaryEditorialButton(
                                text = "Edit Session",
                                icon = Icons.Default.Edit,
                                onClick = { onEditDistribution(distributionId) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("edit_distribution_button")
                            )

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

    if (showShareDialog) {
        ShareDistributionDialog(
            shareText = activeState.shareText,
            onDismiss = { showShareDialog = false }
        )
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
    totalTarget: Int,
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
                Text(
                    text = "$totalTarget Total Recitations",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$participantCount Members",
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
    dateInfo: MonthDateInfo,
    isCurrentMonth: Boolean,
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

@Composable
private fun AmalParticipantCard(
    index: Int,
    participant: ParticipantEntity,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                OctagonalNumberMedallion(
                    number = index,
                    size = 42.dp,
                    primaryColor = accentColor
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = accentColor,
                                fontWeight = FontWeight.ExtraBold
                            )
                        ) {
                            append("${participant.allocatedCount}: ")
                        }
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append(participant.name)
                        }
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        lineHeight = 22.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "${participant.allocatedCount} ${if (participant.allocatedCount == 1) "Recitation" else "Recitations"}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
    }
}

/**
 * Displays a single member's contiguous Para allocation ordered strictly 1 -> 30,
 * leading with the Para numbers first followed by the participant's name:
 * e.g. "1, 2: XYZ" or "3, 4, 5: ABC".
 */
@Composable
private fun KhatmAssignmentSpecimenCard(
    assignment: ParaAssignment,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("khatm_assignment_card_${assignment.startPara}"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OctagonalNumberMedallion(
                number = assignment.startPara,
                size = 42.dp,
                primaryColor = accentColor
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = accentColor,
                            fontWeight = FontWeight.ExtraBold
                        )
                    ) {
                        append("${assignment.commaSeparatedParas}: ")
                    }
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(assignment.participantName)
                    }
                },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    lineHeight = 23.sp
                ),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "${assignment.allocatedCount} ${if (assignment.allocatedCount == 1) "Para" else "Paras"}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
    }
}
