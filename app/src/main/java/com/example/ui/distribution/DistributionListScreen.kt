package com.example.ui.distribution

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diversity1
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.db.DistributionEntity
import com.example.data.db.DistributionType
import com.example.ui.components.ManuscriptOrnamentDivider
import com.example.ui.components.PeacefulEmptyState
import com.example.ui.components.PrimarySanctuaryButton
import com.example.ui.components.ResizableSectionContainer
import com.example.ui.components.SanctuaryGeometricGlyph
import com.example.ui.components.SanctuaryLoadingState
import com.example.ui.components.SanctuaryTopHeader
import com.example.ui.components.SecondaryEditorialButton
import com.example.ui.viewmodel.DistributionViewModel
import java.util.Calendar

@Composable
fun DistributionListScreen(
    distributionViewModel: DistributionViewModel,
    onCreateNewDistribution: (DistributionType) -> Unit,
    onOpenDistributionDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val distributions by distributionViewModel.distributions.collectAsState()
    val isLoading by distributionViewModel.isLoadingDistributions.collectAsState()
    val participantsByDistribution by distributionViewModel.participantsByDistribution.collectAsState()

    var showTypePicker by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf<DistributionType?>(null) } // null = All
    var activeShareSummaryText by remember { mutableStateOf<String?>(null) }
    var distributionPendingDelete by remember { mutableStateOf<DistributionEntity?>(null) }

    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val currentMonth = remember { Calendar.getInstance().get(Calendar.MONTH) + 1 }

    val filteredDistributions = remember(distributions, selectedFilter) {
        if (selectedFilter == null) distributions
        else distributions.filter { it.type == selectedFilter }
    }

    val khatmCount = remember(distributions) { distributions.count { it.type == DistributionType.KHATM } }
    val amalCount = remember(distributions) { distributions.count { it.type == DistributionType.AMAL } }

    Scaffold(
        topBar = {
            SanctuaryTopHeader(
                title = "Distributions",
                leadingStaticGlyph = true,
                isQuranGlyph = false,
                actions = {
                    Surface(
                        onClick = { showTypePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("top_new_distribution_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add New Session",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "New",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (distributions.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showTypePicker = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    text = {
                        Text(
                            text = "New Session",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    },
                    modifier = Modifier.testTag("fab_new_distribution")
                )
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
                                selected = selectedFilter == null,
                                onClick = { selectedFilter = null },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("filter_all")
                            )
                            DistributionTabSegment(
                                title = "Qur'an ($khatmCount)",
                                selected = selectedFilter == DistributionType.KHATM,
                                onClick = { selectedFilter = DistributionType.KHATM },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("filter_khatm")
                            )
                            DistributionTabSegment(
                                title = "Amal ($amalCount)",
                                selected = selectedFilter == DistributionType.AMAL,
                                onClick = { selectedFilter = DistributionType.AMAL },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("filter_amal")
                            )
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
                            description = "Create a monthly 30-Para Qur'an Khatm cycle or a custom Amal recitation session for your group.",
                            actionText = "Create Session",
                            onActionClick = { showTypePicker = true }
                        )
                    }
                } else if (filteredDistributions.isEmpty()) {
                    item(key = "empty_filtered_distributions_state") {
                        val isAmalFilter = selectedFilter == DistributionType.AMAL
                        PeacefulEmptyState(
                            icon = Icons.Default.Diversity1,
                            title = if (isAmalFilter) "No Custom Amal Sessions" else "No Qur'an Khatm Sessions",
                            description = if (isAmalFilter) {
                                "Start a custom target recitation circle for Surah Yaseen, Salawat, or any group Amal."
                            } else {
                                "Start a monthly rotating 30-Para Qur'an Khatm schedule for your family or group."
                            },
                            actionText = if (isAmalFilter) "New Amal Session" else "New Khatm Session",
                            onActionClick = {
                                onCreateNewDistribution(
                                    selectedFilter ?: DistributionType.KHATM
                                )
                            }
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
                                        DistributionType.KHATM -> "Qur'an Khatm Sessions"
                                        DistributionType.AMAL -> "Custom Amal Sessions"
                                        null -> "Active Sessions"
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

                    items(
                        items = filteredDistributions,
                        key = { it.id }
                    ) { dist ->
                        val distParticipants = participantsByDistribution[dist.id].orEmpty()

                        DistributionSpecimenCard(
                            distribution = dist,
                            onOpenClick = { onOpenDistributionDetail(dist.id) },
                            onDeleteClick = { distributionPendingDelete = dist },
                            onShareClick = {
                                val summary = distributionViewModel.generateShareSummaryForDistribution(
                                    distribution = dist,
                                    participants = distParticipants,
                                    targetYear = currentYear,
                                    targetMonth = currentMonth
                                )
                                if (summary.isNotBlank()) {
                                    activeShareSummaryText = summary
                                }
                            }
                        )
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
            onSelectType = { type ->
                showTypePicker = false
                onCreateNewDistribution(type)
            },
            onDismiss = { showTypePicker = false }
        )
    }

    activeShareSummaryText?.let { summaryText ->
        ShareDistributionDialog(
            shareText = summaryText,
            onDismiss = { activeShareSummaryText = null }
        )
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
 * Clean modal dialog for selecting between Qur'an Khatm and Custom Amal.
 */
@Composable
fun DistributionModePickerDialog(
    onSelectType: (DistributionType) -> Unit,
    onDismiss: () -> Unit
) {
    var highlightedType by remember { mutableStateOf(DistributionType.KHATM) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 6.dp,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp)
                .widthIn(max = 520.dp)
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Select Distribution Type",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Choose Qur'an Khatm or Custom Amal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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

                ModeSpecimenOptionCard(
                    title = "Qur'an Khatm",
                    description = "Divides 30 Paras into contiguous blocks that rotate sequentially every month.",
                    isQuranGlyph = true,
                    isSelected = highlightedType == DistributionType.KHATM,
                    accentColor = MaterialTheme.colorScheme.primary,
                    onSelectCard = { highlightedType = DistributionType.KHATM }
                )

                ModeSpecimenOptionCard(
                    title = "Custom Amal",
                    description = "Set a custom target count for Surah Yaseen, Salawat, or any group recitation.",
                    isQuranGlyph = false,
                    isSelected = highlightedType == DistributionType.AMAL,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    onSelectCard = { highlightedType = DistributionType.AMAL }
                )

                ManuscriptOrnamentDivider(modifier = Modifier.padding(vertical = 2.dp))

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
                        text = "Continue",
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        onClick = { onSelectType(highlightedType) },
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }
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
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 1.8.dp else 1.dp,
            color = if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
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
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }

            if (isSelected) {
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * Clean session card showing the session Name and three action buttons:
 * Open, Delete, and Share — each in its own designated space without member para details on the list view.
 */
@Composable
private fun DistributionSpecimenCard(
    distribution: DistributionEntity,
    onOpenClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAmal = distribution.type == DistributionType.AMAL
    val compactButtonPadding = PaddingValues(horizontal = 10.dp, vertical = 11.dp)

    ResizableSectionContainer(
        modifier = modifier.testTag("distribution_card_${distribution.id}"),
        onClick = onOpenClick,
        contentPadding = PaddingValues(22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SanctuaryGeometricGlyph(
                isQuranGlyph = !isAmal,
                size = 46.dp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = distribution.name,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

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
