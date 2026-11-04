package com.example.ui.streak

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.data.FreezeShieldAdResult
import com.example.data.db.DailyLogEntity
import com.example.data.db.PlanType
import com.example.data.db.ReadingPlanEntity
import com.example.data.db.StreakCircleEntity
import com.example.data.db.StreakEntity
import com.example.ui.components.ChiseledSanctuaryButton
import com.example.ui.components.PrimarySanctuaryButton
import com.example.ui.theme.SanctuaryEmeraldDark
import com.example.ui.theme.SanctuaryEmeraldMedium
import com.example.ui.theme.SanctuaryGoldAmber
import com.example.ui.theme.SanctuaryGoldBright
import com.example.ui.theme.SanctuaryGoldHalo
import com.example.ui.theme.SanctuaryGoldWarm
import com.example.ui.viewmodel.StreakViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyScreen(
    streakViewModel: StreakViewModel,
    onContinueReading: (paraNum: Int, surahNum: Int, ayahNum: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val streak by streakViewModel.streakState.collectAsState()
    val todayLog by streakViewModel.todayLog.collectAsState()
    val activePlans by streakViewModel.activePlansState.collectAsState()
    val streakCircles by streakViewModel.streakCirclesState.collectAsState()
    val recentLogs by streakViewModel.recentLogsState.collectAsState()
    val savedProfileName by streakViewModel.userProfileName.collectAsState()

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val activity = context as? Activity

    var showFreezeDialog by remember { mutableStateOf(false) }
    var showCreatePlanSheet by remember { mutableStateOf(false) }
    var showCreateCircleDialog by remember { mutableStateOf(false) }
    var showJoinCircleDialog by remember { mutableStateOf(false) }
    var showMilestonesStaircaseDialog by remember { mutableStateOf(false) }
    var selectedCircleForDetail by remember { mutableStateOf<StreakCircleEntity?>(null) }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Streaks & Plans",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.3.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Your daily Qur'an recitation companion",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (streak?.isOptedIn == true) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.clickable { showFreezeDialog = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = "Freeze Pod",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if ((streak?.availableFreezeShields ?: 0) > 0) "1 Pod" else "Freeze",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (streak?.isOptedIn != true) {
            // Opt-in / Introductory Sanctuary Screen
            OptInWelcomeView(
                onOptIn = { targetAyahs ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    streakViewModel.optInToStreak(targetAyahs)
                    Toast.makeText(context, "Welcome to your Daily Qur'an Journey! ✨", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            // Active Journey Dashboard
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                // 1. Top Hero Card: Streak Ember & Today's Target (Tap to open Duolingo Staircase)
                item {
                    CelestialStreakHero(
                        streak = streak!!,
                        todayLog = todayLog,
                        onContinueReading = {
                            val p = streak!!.lastReadPara ?: 1
                            val s = streak!!.lastReadSurah ?: 1
                            val a = streak!!.lastReadAyah ?: 1
                            onContinueReading(p, s, a)
                        },
                        onOpenFreezeDialog = { showFreezeDialog = true },
                        onOpenMilestonesStaircase = { showMilestonesStaircaseDialog = true }
                    )
                }

                // 2. My Plans: Horizontal Swipe Deck of Rounded Rectangular Cards
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "My Reading Plans",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Swipe to track your goals",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            ChiseledSanctuaryButton(
                                text = "New Plan",
                                icon = Icons.Default.Add,
                                onClick = { showCreatePlanSheet = true },
                                modifier = Modifier.testTag("create_plan_button")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (activePlans.isEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                ),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(22.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No active reading plans yet",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Set a daily Ayah target or Juz goal to pace your recitation.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(activePlans, key = { it.id }) { plan ->
                                    ReadingPlanDeckCard(
                                        plan = plan,
                                        onRead = {
                                            val p = streak!!.lastReadPara ?: 1
                                            val s = streak!!.lastReadSurah ?: 1
                                            val a = streak!!.lastReadAyah ?: 1
                                            onContinueReading(p, s, a)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Recitation Circles: Horizontal Swipe Deck of Circular "Blazes"
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Recitation Circles",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Circular blazes with your family & companions",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    onClick = { showJoinCircleDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.2.dp, SanctuaryGoldWarm.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .height(42.dp)
                                        .testTag("join_circle_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
                                        Text(
                                            "Join",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = SanctuaryGoldAmber
                                        )
                                    }
                                }

                                ChiseledSanctuaryButton(
                                    text = "Create",
                                    icon = Icons.Default.Add,
                                    onClick = { showCreateCircleDialog = true },
                                    modifier = Modifier.testTag("create_circle_button")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (streakCircles.isEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                ),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(22.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Groups,
                                        contentDescription = null,
                                        tint = Color(0xFFFF9800),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No recitation circles yet",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Create a circle and share the passcode with companions to motivate each other daily.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(streakCircles, key = { it.id }) { circle ->
                                    CircularBlazeCard(
                                        circle = circle,
                                        onClick = { selectedCircleForDetail = circle }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Freeze Pod Shield Dialog
    if (showFreezeDialog && streak != null) {
        FreezePodDialog(
            streak = streak!!,
            streakViewModel = streakViewModel,
            activity = activity,
            onDismiss = { showFreezeDialog = false }
        )
    }

    // Milestones Staircase Duolingo-style Dialog
    if (showMilestonesStaircaseDialog && streak != null) {
        MilestonesStaircaseDialog(
            streak = streak!!,
            todayLog = todayLog,
            onDismiss = { showMilestonesStaircaseDialog = false }
        )
    }

    // Streak Circle Detail Dialog
    if (selectedCircleForDetail != null) {
        StreakCircleDetailDialog(
            circle = selectedCircleForDetail!!,
            streakViewModel = streakViewModel,
            onShare = {
                streakViewModel.formatShareCircle(selectedCircleForDetail!!) { text ->
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, text)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Streak Circle"))
                }
            },
            onDismiss = { selectedCircleForDetail = null }
        )
    }

    // Create Plan Bottom Sheet
    if (showCreatePlanSheet) {
        CreatePlanBottomSheet(
            onDismiss = { showCreatePlanSheet = false },
            onCreate = { title, isDailyGoal, count, durationDays ->
                if (isDailyGoal) {
                    streakViewModel.createDailyAyahPlan(title, count, durationDays)
                } else {
                    streakViewModel.createParaRangePlan(title, 1, count.coerceIn(1, 30), durationDays)
                }
                showCreatePlanSheet = false
                Toast.makeText(context, "New plan '$title' started!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Create Circle Dialog
    if (showCreateCircleDialog) {
        CreateCircleDialog(
            initialUserName = savedProfileName,
            onDismiss = { showCreateCircleDialog = false },
            onCreate = { name, creator ->
                streakViewModel.createStreakCircle(name, creator) {
                    showCreateCircleDialog = false
                    Toast.makeText(context, "Circle '${it.name}' created! Passcode: ${it.roomPasscode}", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    // Join Circle Dialog
    if (showJoinCircleDialog) {
        JoinCircleDialog(
            initialUserName = savedProfileName,
            onDismiss = { showJoinCircleDialog = false },
            onJoin = { passcode, name ->
                streakViewModel.joinStreakCircle(passcode, name) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    if (success) showJoinCircleDialog = false
                }
            }
        )
    }
}

/**
 * Peaceful, non-intrusive opt-in introductory screen.
 */
@Composable
private fun OptInWelcomeView(
    onOptIn: (targetAyahs: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTarget by remember { mutableIntStateOf(10) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // Visual: Illuminated 3-Step Milestone Stairwell Graphic
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Step 1: Base
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = SanctuaryEmeraldMedium.copy(alpha = 0.35f),
                            border = BorderStroke(1.2.dp, SanctuaryGoldWarm.copy(alpha = 0.7f)),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("5", fontWeight = FontWeight.Bold, color = SanctuaryGoldAmber)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Start",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SanctuaryEmeraldDark,
                            border = BorderStroke(1.dp, SanctuaryGoldWarm.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .width(62.dp)
                                .height(32.dp)
                        ) {}
                    }

                    // Step 2: Intermediate
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = SanctuaryGoldWarm.copy(alpha = 0.35f),
                            border = BorderStroke(1.5.dp, SanctuaryGoldBright),
                            modifier = Modifier.size(50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = SanctuaryGoldBright,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Grow",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = SanctuaryGoldAmber
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SanctuaryEmeraldDark,
                            border = BorderStroke(1.dp, SanctuaryGoldBright.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .width(66.dp)
                                .height(64.dp)
                        ) {}
                    }

                    // Step 3: Pinnacle / Radiant Star
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = SanctuaryGoldBright,
                            border = BorderStroke(2.dp, SanctuaryGoldAmber),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = SanctuaryEmeraldDark,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Noor",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = SanctuaryGoldBright
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SanctuaryEmeraldDark,
                            border = BorderStroke(1.2.dp, SanctuaryGoldBright),
                            modifier = Modifier
                                .width(70.dp)
                                .height(96.dp)
                        ) {}
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Embark on Measured Recitation",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "“And recite the Qur'an with measured recitation.”\n— Surah Al-Muzzammil (73:4)",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Choose Your Daily Goal",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Goal selection chips with generous touch target
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(5, 10, 20).forEach { count ->
                    val isSelected = selectedTarget == count
                    Surface(
                        onClick = { selectedTarget = count },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .testTag("target_chip_$count")
                    ) {
                        Text(
                            text = "$count Ayahs",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }

        // Dedicated space for the primary action button (never wrapped or cramped!)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            color = Color.Transparent
        ) {
            PrimarySanctuaryButton(
                text = "Begin Daily Journey",
                icon = Icons.Default.PlayArrow,
                onClick = { onOptIn(selectedTarget) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("begin_journey_button")
            )
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Top Celestial Hero displaying the user's active streak flame, daily progress bar, and checkpoint.
 */
@Composable
private fun CelestialStreakHero(
    streak: StreakEntity,
    todayLog: DailyLogEntity?,
    onContinueReading: () -> Unit,
    onOpenFreezeDialog: () -> Unit,
    onOpenMilestonesStaircase: () -> Unit
) {
    val ayahsRead = todayLog?.ayahsReadCount ?: 0
    val targetAyahs = (todayLog?.targetAyahsCount ?: 10).coerceAtLeast(1)
    val progress = (ayahsRead.toFloat() / targetAyahs).coerceIn(0f, 1f)
    val isGoalMet = todayLog?.hasMetGoal == true || ayahsRead >= targetAyahs

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Streak flame counter (clickable to open staircase)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onOpenMilestonesStaircase() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFFFF9800).copy(alpha = 0.25f),
                                        Color(0xFFE65100).copy(alpha = 0.08f)
                                    )
                                )
                            )
                            .border(1.5.dp, Color(0xFFFF9800).copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak Flame",
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${streak.currentStreakDays}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = " Days",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 3.dp, start = 2.dp)
                            )
                        }
                        Text(
                            text = "Best: ${streak.longestStreakDays} days • Missed: ${streak.consecutiveMissedDays}/2",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Freeze Pod badge button
                IconButton(
                    onClick = onOpenFreezeDialog,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (streak.availableFreezeShields > 0) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        )
                        .border(
                            1.dp,
                            if (streak.availableFreezeShields > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(
                        imageVector = if (streak.availableFreezeShields > 0) Icons.Default.Shield else Icons.Outlined.Shield,
                        contentDescription = "Freeze Pod Status",
                        tint = if (streak.availableFreezeShields > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar toward today's goal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isGoalMet) "Today's Goal Completed! ✨" else "Today's Recitation Goal",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (isGoalMet) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isGoalMet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$ayahsRead / $targetAyahs Ayahs",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = if (isGoalMet) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Resume reading checkpoint button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val lastSurah = streak.lastReadSurahName ?: "Surah Al-Fatihah"
                val lastAyah = streak.lastReadAyah ?: 1
                val lastPara = streak.lastReadPara ?: 1

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Last Stop",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$lastSurah : Ayah $lastAyah (Para $lastPara)",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onContinueReading,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Continue", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                onClick = onOpenMilestonesStaircase,
                shape = RoundedCornerShape(12.dp),
                color = SanctuaryGoldWarm.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, SanctuaryGoldWarm.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = SanctuaryGoldAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Ascend Stairwell • Tap to view all milestones",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = SanctuaryGoldAmber
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = SanctuaryGoldAmber,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Visual Celestial Milestone Path with Breathing Noor Orb and Serpentine Constellation Nodes.
 */
@Composable
private fun CelestialMilestonePathView(
    streak: StreakEntity,
    todayLog: DailyLogEntity?,
    recentLogs: List<DailyLogEntity>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val currentStreak = streak.currentStreakDays
    val ayahsRead = todayLog?.ayahsReadCount ?: 0
    val targetAyahs = (todayLog?.targetAyahsCount ?: 10).coerceAtLeast(1)

    // Milestone landmarks
    val milestones = listOf(
        MilestoneItem(1, "First Light", "Start of the blessed journey", Icons.Default.Star),
        MilestoneItem(3, "Steadfast Heart", "3 consecutive days of recitation", Icons.Default.Star),
        MilestoneItem(7, "Lantern of Noor", "1 week of devotion completed", Icons.Default.LocalFireDepartment, isMajor = true),
        MilestoneItem(14, "Radiant Fortnight", "2 weeks of illuminated recitation", Icons.Default.Star),
        MilestoneItem(21, "Habit of Light", "21 days: ingrained in your soul", Icons.Default.Star),
        MilestoneItem(30, "Celestial Khatm", "1 whole month of daily Qur'an", Icons.Default.LocalFireDepartment, isMajor = true),
        MilestoneItem(60, "Emerald Beacon", "60 days of continuous recitation", Icons.Default.Star, isMajor = true),
        MilestoneItem(100, "Crown of Light", "100 days of blessed Quranic remembrance", Icons.Default.Star, isMajor = true)
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Interactive Breathing Noor Ember Orb
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        Toast.makeText(
                            context,
                            "Today: $ayahsRead of $targetAyahs Ayahs recited. Your Noor is glowing! ✨",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.SanctuaryEmeraldDark),
                border = BorderStroke(1.4.dp, com.example.ui.theme.SanctuaryGoldWarm.copy(alpha = 0.8f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Central breathing orb
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        com.example.ui.theme.SanctuaryGoldBright.copy(alpha = 0.35f),
                                        com.example.ui.theme.SanctuaryEmeraldMedium
                                    )
                                )
                            )
                            .border(2.dp, com.example.ui.theme.SanctuaryGoldBright, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Noor Ember",
                                tint = com.example.ui.theme.SanctuaryGoldBright,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "$currentStreak",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Breathing Noor Ember",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.SanctuaryGoldBright
                        )
                    )

                    Text(
                        text = "Tap to view today's aura • $ayahsRead of $targetAyahs Ayahs recited",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }
        }

        item {
            Text(
                text = "Celestial Serpentine Milestones",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Follow the illuminated path of continuous recitation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        itemsIndexed(milestones) { index, m ->
            val isUnlocked = currentStreak >= m.day
            val isNext = !isUnlocked && (currentStreak == m.day - 1 || (currentStreak < m.day && milestones.firstOrNull { currentStreak < it.day } == m))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("milestone_path_node_${m.day}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Celestial Spine Column with connecting glowing beam
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(54.dp)
                ) {
                    // Top beam connector
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(16.dp)
                                .background(
                                    if (isUnlocked) SanctuaryGoldBright
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                )
                        )
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Luminous Constellation Medallion
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isUnlocked -> Brush.radialGradient(
                                        listOf(SanctuaryGoldBright, SanctuaryEmeraldDark)
                                    )
                                    isNext -> Brush.radialGradient(
                                        listOf(SanctuaryGoldAmber, SanctuaryGoldWarm.copy(alpha = 0.4f))
                                    )
                                    else -> Brush.radialGradient(
                                        listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface)
                                    )
                                }
                            )
                            .border(
                                width = if (isUnlocked || isNext || m.isMajor) 1.8.dp else 1.dp,
                                color = when {
                                    isUnlocked -> SanctuaryGoldBright
                                    isNext -> SanctuaryGoldAmber
                                    else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isUnlocked -> {
                                Icon(
                                    imageVector = if (m.isMajor) Icons.Default.Star else Icons.Default.Check,
                                    contentDescription = "Completed",
                                    tint = if (m.isMajor) SanctuaryGoldBright else Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            isNext -> {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = "In Progress",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Bottom beam connector
                    if (index < milestones.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(16.dp)
                                .background(
                                    if (currentStreak >= milestones[index + 1].day) SanctuaryGoldBright
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                )
                        )
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Milestone Sanctuary Card
                MilestoneCard(
                    milestone = m,
                    currentStreak = currentStreak,
                    isUnlocked = isUnlocked,
                    isNext = isNext,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val status = if (isUnlocked) "Unlocked! ✨" else "${m.day - currentStreak} days remaining to reach this milestone."
                        Toast.makeText(context, "${m.title}: $status", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private data class MilestoneItem(
    val day: Int,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isMajor: Boolean = false
)

@Composable
private fun MilestoneCard(
    milestone: MilestoneItem,
    currentStreak: Int,
    isUnlocked: Boolean,
    isNext: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = when {
        isUnlocked -> SanctuaryEmeraldDark
        isNext -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = when {
        isUnlocked -> SanctuaryGoldWarm.copy(alpha = 0.85f)
        isNext -> SanctuaryGoldAmber
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(if (isNext || milestone.isMajor || isUnlocked) 1.4.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isUnlocked) SanctuaryGoldWarm.copy(alpha = 0.22f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    border = BorderStroke(0.6.dp, if (isUnlocked) SanctuaryGoldBright.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "DAY ${milestone.day}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 9.sp,
                            letterSpacing = 0.6.sp
                        ),
                        color = if (isUnlocked) SanctuaryGoldBright else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (isUnlocked) {
                    Text(
                        text = "Achieved ✨",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = SanctuaryGoldBright
                    )
                } else if (isNext) {
                    Text(
                        text = "Next Target 🔥",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = SanctuaryGoldAmber
                    )
                } else {
                    Text(
                        text = "${milestone.day - currentStreak}d left",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = milestone.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = if (isUnlocked) Color.White else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = milestone.description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = if (isUnlocked) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Reading Plans list view with active goals and "Create Plan" button.
 */
@Composable
private fun ReadingPlansListView(
    plans: List<ReadingPlanEntity>,
    onCreatePlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Custom Reading Plans",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Set structured targets to finish Paras or Surahs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ChiseledSanctuaryButton(
                    text = "New Plan",
                    icon = Icons.Default.Add,
                    onClick = onCreatePlan,
                    modifier = Modifier.testTag("create_plan_button")
                )
            }
        }

        if (plans.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No active reading plans yet",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Create a goal like '10 Ayahs a Day' or 'Complete 5 Paras' to keep yourself inspired.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(plans) { plan ->
                PlanCard(plan = plan)
            }
        }
    }
}

@Composable
private fun PlanCard(plan: ReadingPlanEntity) {
    val progress = if (plan.totalDaysCount > 0) {
        (plan.completedDaysCount.toFloat() / plan.totalDaysCount).coerceIn(0f, 1f)
    } else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = plan.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = when (plan.planType) {
                            PlanType.DAILY_AYAH_GOAL -> "Daily Goal"
                            PlanType.PARA_RANGE_TARGET -> "Para Range"
                            PlanType.CUSTOM_SURAH_TARGET -> "Surah Goal"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${plan.completedDaysCount} / ${plan.totalDaysCount} days completed • ${plan.dailyTargetAyahs} Ayahs/day",
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
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

/**
 * Streak Circles (Group Streak Rooms) list view.
 */
@Composable
private fun StreakCirclesListView(
    circles: List<StreakCircleEntity>,
    onCreateCircle: () -> Unit,
    onJoinCircle: () -> Unit,
    onShareCircle: (StreakCircleEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Streak Circles",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Recite together with friends and family",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = onJoinCircle,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.2.dp, SanctuaryGoldWarm.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("join_circle_button")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
                            Text(
                                "Join",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = SanctuaryGoldAmber
                            )
                        }
                    }

                    ChiseledSanctuaryButton(
                        text = "Create",
                        icon = Icons.Default.Add,
                        onClick = onCreateCircle,
                        modifier = Modifier.testTag("create_circle_button")
                    )
                }
            }
        }

        if (circles.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No streak circles yet",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Create a circle and share the passcode with your family or friends to motivate one another daily.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(circles) { circle ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = circle.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Room Passcode: ${circle.roomPasscode}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "${circle.collectiveStreakDays}d",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Passcode chip & Share button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Passcode", circle.roomPasscode))
                                    Toast.makeText(context, "Passcode '${circle.roomPasscode}' copied!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Code: ${circle.roomPasscode}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Passcode",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = { onShareCircle(circle) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Invite", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Freeze Pod Shield Dialog explaining the strict 1-pod per 7-day rule,
 * 2-consecutive-day death rule, and rewarded ads server-support rationale.
 */
@Composable
private fun FreezePodDialog(
    streak: StreakEntity,
    streakViewModel: StreakViewModel,
    activity: Activity?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val adStatus by streakViewModel.adWatchStatus.collectAsState()

    val canUseCooldown = streakViewModel.streakRepository.canUseFreezeShieldCooldown(streak.lastFreezeShieldUsedDate)
    val hasPod = streak.availableFreezeShields > 0
    val adsWatched = streak.pendingAdViewsForNextShield
    val adsNeeded = 3

    AlertDialog(
        onDismissRequest = {
            streakViewModel.clearAdStatus()
            onDismiss()
        },
        icon = {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Noor Freeze Pod Shield",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Rule 1: Missed day safeguard
                Text(
                    text = "A Freeze Pod automatically protects your streak if life gets in the way and you miss 1 day.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Strict rules card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "• Max 1 Freeze Pod earned per 7 days.",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "• If 2 consecutive days are missed, streaks permanently reset.",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // Server support explanation
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Support Our Quran Servers",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Watching 3 short sponsor messages helps fund cloud servers and keep this app 100% free for the Ummah while unlocking your weekly shield.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Current pod status
                if (hasPod) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF2E7D32).copy(alpha = 0.15f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "You have 1 Freeze Pod active and ready!",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                } else if (!canUseCooldown) {
                    Text(
                        text = "Cooldown Active: Freeze Pod was used recently. Available again in 7 days.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    // Ads progress
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Sponsor Videos Progress",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$adsWatched / $adsNeeded",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { adsWatched.toFloat() / adsNeeded },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (adStatus != null) {
                    Text(
                        text = adStatus!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            if (!hasPod && canUseCooldown) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        streakViewModel.watchRewardedAdForFreeze(
                            activity = activity,
                            onSuccess = { res ->
                                when (res) {
                                    is FreezeShieldAdResult.ShieldEarned -> {
                                        Toast.makeText(context, "Freeze Pod Activated! 🛡️", Toast.LENGTH_LONG).show()
                                    }
                                    is FreezeShieldAdResult.Progress -> {
                                        Toast.makeText(context, "Progress: ${res.adsWatched}/3 videos", Toast.LENGTH_SHORT).show()
                                    }
                                    is FreezeShieldAdResult.CooldownActive -> {
                                        Toast.makeText(context, "Cooldown: ${res.daysRemaining} days remaining", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onAdNotReady = {
                                Toast.makeText(context, "Sponsor message is currently buffering. Please try again in a moment.", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                ) {
                    Text("Watch Sponsor Video (${adsWatched}/3)")
                }
            } else {
                TextButton(onClick = {
                    streakViewModel.clearAdStatus()
                    onDismiss()
                }) {
                    Text("Close")
                }
            }
        },
        dismissButton = {
            if (!hasPod && canUseCooldown) {
                TextButton(onClick = {
                    streakViewModel.clearAdStatus()
                    onDismiss()
                }) {
                    Text("Cancel")
                }
            }
        }
    )
}

/**
 * Bottom sheet to configure a custom reading plan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePlanBottomSheet(
    onDismiss: () -> Unit,
    onCreate: (title: String, isDailyGoal: Boolean, count: Int, durationDays: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var isDailyGoal by remember { mutableStateOf(true) }
    var count by remember { mutableIntStateOf(10) }
    var durationDays by remember { mutableIntStateOf(30) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Create New Reading Plan",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Plan Title") },
                placeholder = { Text("e.g. Ramadan Khatm, Morning Noor") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = isDailyGoal, onClick = { isDailyGoal = true })
                Text("Daily Ayahs Goal", modifier = Modifier.clickable { isDailyGoal = true })
                Spacer(modifier = Modifier.width(16.dp))
                RadioButton(selected = !isDailyGoal, onClick = { isDailyGoal = false })
                Text("Para Range Goal", modifier = Modifier.clickable { isDailyGoal = false })
            }

            if (isDailyGoal) {
                Text(
                    text = "Daily Target: $count Ayahs",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Slider(
                    value = count.toFloat(),
                    onValueChange = { count = it.toInt() },
                    valueRange = 5f..100f,
                    steps = 18
                )
            } else {
                Text(
                    text = "Total Paras: $count Paras",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Slider(
                    value = count.toFloat(),
                    onValueChange = { count = it.toInt() },
                    valueRange = 1f..30f,
                    steps = 28
                )
            }

            Text(
                text = "Duration: $durationDays Days",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7, 14, 30, 60).forEach { days ->
                    FilterChip(
                        selected = durationDays == days,
                        onClick = { durationDays = days },
                        label = { Text("$days Days") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            PrimarySanctuaryButton(
                text = "Start Plan",
                onClick = {
                    val finalTitle = if (title.isNotBlank()) title else if (isDailyGoal) "$count Ayahs Daily Plan" else "Finish $count Paras"
                    onCreate(finalTitle, isDailyGoal, count, durationDays)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Dialog to create a new Streak Circle.
 */
@Composable
private fun CreateCircleDialog(
    initialUserName: String = "",
    onDismiss: () -> Unit,
    onCreate: (name: String, creatorName: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var creator by remember(initialUserName) { mutableStateOf(initialUserName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Recitation Circle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Circle Name") },
                    placeholder = { Text("e.g. Family Qur'an Circle") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = creator,
                    onValueChange = { creator = it },
                    label = { Text("Your Display Name") },
                    placeholder = { Text("e.g. Ahmad") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name.trim(), if (creator.isNotBlank()) creator.trim() else "Member")
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Dialog to join a Streak Circle via Passcode.
 * Validates that the passcode is not a Khatm room passcode.
 */
@Composable
private fun JoinCircleDialog(
    initialUserName: String = "",
    onDismiss: () -> Unit,
    onJoin: (passcode: String, memberName: String) -> Unit
) {
    var passcode by remember { mutableStateOf("") }
    var memberName by remember(initialUserName) { mutableStateOf(initialUserName) }

    val isKhatmPasscode = passcode.trim().uppercase().startsWith("QBO-") ||
            passcode.trim().uppercase().startsWith("KHTM-") ||
            passcode.trim().uppercase().startsWith("DIST-")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join Recitation Circle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = passcode,
                    onValueChange = { if (it.length <= 14) passcode = it.uppercase() },
                    label = { Text("Circle Passcode") },
                    placeholder = { Text("e.g. CRCL-3819") },
                    singleLine = true,
                    isError = isKhatmPasscode,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isKhatmPasscode) {
                    Text(
                        text = "This passcode belongs to a Qur'an Khatm session. Please go to the Distributions tab to join.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                OutlinedTextField(
                    value = memberName,
                    onValueChange = { memberName = it },
                    label = { Text("Your Display Name") },
                    placeholder = { Text("e.g. Fatima") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (passcode.isNotBlank() && !isKhatmPasscode) {
                        onJoin(passcode.trim(), if (memberName.isNotBlank()) memberName.trim() else "Member")
                    }
                },
                enabled = passcode.isNotBlank() && !isKhatmPasscode
            ) {
                Text("Join Circle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ReadingPlanDeckCard(
    plan: ReadingPlanEntity,
    onRead: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (plan.totalDaysCount > 0) {
        (plan.completedDaysCount.toFloat() / plan.totalDaysCount.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val percent = (progress * 100).toInt()

    Card(
        modifier = modifier
            .width(280.dp)
            .clickable { onRead() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = if (plan.planType == PlanType.DAILY_AYAH_GOAL) "Daily Habit" else "Paras Plan",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = plan.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${plan.dailyTargetAyahs} Ayahs / Day • ${plan.completedDaysCount} of ${plan.totalDaysCount} days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    onClick = onRead,
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Recite",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CircularBlazeCard(
    circle: StreakCircleEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(2.2.dp, Color(0xFFFFB74D)),
        modifier = modifier.size(148.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF9800),
                            Color(0xFFE65100),
                            Color(0xFF2E1300)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color(0xFFFFF3E0),
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = circle.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        lineHeight = 16.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.4f)
                ) {
                    Text(
                        text = "🔥 ${circle.collectiveStreakDays}d streak",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = Color(0xFFFFE0B2),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MilestonesStaircaseDialog(
    streak: StreakEntity,
    todayLog: DailyLogEntity?,
    onDismiss: () -> Unit
) {
    val currentStreak = streak.currentStreakDays
    val milestones = remember {
        listOf(
            MilestoneItem(1, "First Light", "Start of the blessed journey", Icons.Default.Star),
            MilestoneItem(3, "Steadfast Heart", "3 consecutive days of recitation", Icons.Default.Star),
            MilestoneItem(7, "Lantern of Noor", "1 week of devotion completed", Icons.Default.LocalFireDepartment, isMajor = true),
            MilestoneItem(14, "Radiant Fortnight", "2 weeks of illuminated recitation", Icons.Default.Star),
            MilestoneItem(21, "Habit of Light", "21 days: ingrained in your soul", Icons.Default.Star),
            MilestoneItem(30, "Celestial Khatm", "1 whole month of daily Qur'an", Icons.Default.LocalFireDepartment, isMajor = true),
            MilestoneItem(60, "Emerald Beacon", "60 days of continuous recitation", Icons.Default.Star, isMajor = true),
            MilestoneItem(100, "Crown of Light", "100 days of blessed Quranic remembrance", Icons.Default.Star, isMajor = true)
        )
    }

    val nextMilestone = milestones.firstOrNull { it.day > currentStreak }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, SanctuaryGoldWarm.copy(alpha = 0.8f)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SanctuaryEmeraldDark)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Quranic Milestones Stairwell",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Current: Day $currentStreak • Next Target: Day ${nextMilestone?.day ?: 100}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SanctuaryGoldBright
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Staircase winding down/up
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    itemsIndexed(milestones) { index, m ->
                        val isUnlocked = currentStreak >= m.day
                        val isCurrentTarget = !isUnlocked && (nextMilestone == m)
                        val xOffset = when (index % 4) {
                            0 -> 0.dp
                            1 -> (-50).dp
                            2 -> 0.dp
                            else -> 50.dp
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(x = xOffset),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (index > 0) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(18.dp)
                                        .background(
                                            if (isUnlocked) SanctuaryGoldBright.copy(alpha = 0.8f)
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                            RoundedCornerShape(2.dp)
                                        )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            // Stair step node
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isUnlocked -> Brush.radialGradient(
                                                listOf(SanctuaryGoldBright, SanctuaryGoldWarm)
                                            )
                                            isCurrentTarget -> Brush.radialGradient(
                                                listOf(Color(0xFFFF9800), Color(0xFFE65100))
                                            )
                                            else -> Brush.radialGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.surfaceVariant,
                                                    MaterialTheme.colorScheme.surface
                                                )
                                            )
                                        }
                                    )
                                    .border(
                                        width = if (isCurrentTarget) 3.dp else 2.dp,
                                        color = when {
                                            isUnlocked -> SanctuaryGoldAmber
                                            isCurrentTarget -> SanctuaryGoldBright
                                            else -> MaterialTheme.colorScheme.outlineVariant
                                        },
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isUnlocked) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Completed",
                                        tint = SanctuaryEmeraldDark,
                                        modifier = Modifier.size(32.dp)
                                    )
                                } else if (isCurrentTarget) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = "Next Target",
                                        tint = Color.White,
                                        modifier = Modifier.size(34.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Day Badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    isUnlocked -> SanctuaryGoldWarm.copy(alpha = 0.2f)
                                    isCurrentTarget -> Color(0xFFFF9800).copy(alpha = 0.2f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Text(
                                    text = if (isCurrentTarget) "Day ${m.day} • Next Target" else "Day ${m.day}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = when {
                                        isUnlocked -> SanctuaryGoldAmber
                                        isCurrentTarget -> Color(0xFFE65100)
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }

                            Text(
                                text = m.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isCurrentTarget || isUnlocked) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isUnlocked || isCurrentTarget) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = m.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreakCircleDetailDialog(
    circle: StreakCircleEntity,
    streakViewModel: StreakViewModel,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val members by streakViewModel.getCircleMembers(circle.id).collectAsState(initial = emptyList())

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, Color(0xFFFFB74D)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.78f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(26.dp)
                        )
                        Text(
                            text = circle.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Passcode banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFF9800).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Circle Passcode",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = circle.roomPasscode,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color(0xFFE65100)
                            )
                        }

                        IconButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Circle Passcode", circle.roomPasscode))
                                Toast.makeText(context, "Copied passcode ${circle.roomPasscode}", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Passcode",
                                tint = Color(0xFFE65100)
                            )
                        }
                    }
                }

                Text(
                    text = "Members (${members.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Members list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(members, key = { it.id }) { member ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = member.memberName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (member.isHost) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "Host",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "🔥 ${member.currentStreakDays}d",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFFE65100)
                                    )
                                    if (member.hasCompletedToday) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Completed Today",
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Share button in dedicated space
                PrimarySanctuaryButton(
                    text = "Share Circle Passcode",
                    icon = Icons.Default.Share,
                    onClick = onShare,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
