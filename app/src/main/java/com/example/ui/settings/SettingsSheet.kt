package com.example.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.quran.ArabicFontStyle
import com.example.ui.components.AyahEndMedallionSymbol
import com.example.ui.components.ManuscriptOrnamentDivider
import com.example.ui.components.PrimarySanctuaryButton
import com.example.ui.components.SecondaryEditorialButton
import com.example.ui.components.ResizableSectionContainer
import com.example.ui.components.SanctuaryGeometricGlyph
import com.example.ui.viewmodel.DistributionViewModel
import com.example.ui.viewmodel.QuranViewModel
import com.example.util.KhatmNotificationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    quranViewModel: QuranViewModel? = null,
    distributionViewModel: DistributionViewModel? = null
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val fontSizeSp by quranViewModel?.fontSizeSp?.collectAsState()
        ?: remember { androidx.compose.runtime.mutableIntStateOf(24) }
    val fontStyle by quranViewModel?.arabicFontStyle?.collectAsState()
        ?: remember { mutableStateOf(ArabicFontStyle.UTHMANI) }

    val savedProfileName by distributionViewModel?.userProfileName?.collectAsState()
        ?: remember { mutableStateOf("") }
    val notificationsEnabled by distributionViewModel?.notificationsEnabled?.collectAsState()
        ?: remember { mutableStateOf(true) }
    val firebaseStatus by distributionViewModel?.firebaseConnectionStatus?.collectAsState()
        ?: remember { mutableStateOf(com.example.data.firebase.FirebaseConnectionStatus.UNINITIALIZED) }
    val firebaseProfile by distributionViewModel?.firebaseUserProfile?.collectAsState()
        ?: remember { mutableStateOf(null) }

    var localNameDraft by remember(savedProfileName) { mutableStateOf(savedProfileName) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var sliderFontSize by remember(fontSizeSp) { mutableStateOf(fontSizeSp.toFloat()) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    ModalBottomSheet(
        onDismissRequest = {
            if (localNameDraft.trim() != savedProfileName) {
                distributionViewModel?.setUserProfileName(localNameDraft.trim())
            }
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Sheet Title Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SanctuaryGeometricGlyph(
                        isQuranGlyph = true,
                        size = 44.dp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Profile, rooms & Mushaf typography",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "v4.0",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // 1. Profile & Online Room Notifications
            if (distributionViewModel != null) {
                ResizableSectionContainer(
                    title = "Room Profile & Reminders",
                    icon = Icons.Default.Person,
                    contentPadding = PaddingValues(22.dp)
                ) {
                    Text(
                        text = "Your Default Name in Online Rooms",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = localNameDraft,
                        onValueChange = {
                            localNameDraft = it
                            distributionViewModel.setUserProfileName(it)
                        },
                        placeholder = { Text("e.g. Hamza") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_profile_name_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val cloudStatusText = when (firebaseStatus) {
                        com.example.data.firebase.FirebaseConnectionStatus.READY -> {
                            val shortUid = firebaseProfile?.uidHash ?: "ACTIVE"
                            "Firebase Auth & Firestore Secured • ID: $shortUid"
                        }
                        com.example.data.firebase.FirebaseConnectionStatus.MISSING_CONFIGURATION -> {
                            "Local-Ready Mode (Configure Firebase keys in Secrets Panel for live cloud sync)"
                        }
                        else -> "Initializing Firebase Services..."
                    }
                    Text(
                        text = cloudStatusText,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = if (firebaseStatus == com.example.data.firebase.FirebaseConnectionStatus.READY) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (firebaseStatus == com.example.data.firebase.FirebaseConnectionStatus.READY ||
                        localNameDraft.isNotBlank()
                    ) {
                        Surface(
                            onClick = {
                                localNameDraft = ""
                                distributionViewModel.deleteCloudProfileAndResetId()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("delete_cloud_profile_button")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Delete Cloud Profile & Reset Anonymous ID",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
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
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Room & Monthly Reminders",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Notify when members complete Paras or a new month begins",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { enabled ->
                                    distributionViewModel.setNotificationsEnabled(enabled)
                                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        !KhatmNotificationHelper.hasNotificationPermission(context)
                                    ) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 2. Theme Selection Section
            ResizableSectionContainer(
                title = "Appearance",
                contentPadding = PaddingValues(22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ThemeOptionTile(
                        title = "Warm Parchment",
                        subtitle = "Light mode",
                        icon = Icons.Default.LightMode,
                        selected = !isDarkTheme,
                        previewBg = Color(0xFFFBF8F1),
                        previewAccent = Color(0xFF144538),
                        onClick = { onToggleDarkTheme(false) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_light_button")
                    )
                    ThemeOptionTile(
                        title = "Midnight Slate",
                        subtitle = "Dark mode",
                        icon = Icons.Default.DarkMode,
                        selected = isDarkTheme,
                        previewBg = Color(0xFF12151E),
                        previewAccent = Color(0xFFE5C178),
                        onClick = { onToggleDarkTheme(true) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_dark_button")
                    )
                }
            }

            // 3. Mushaf Typography & Script Container
            if (quranViewModel != null) {
                ResizableSectionContainer(
                    title = "Mushaf Script & Size",
                    icon = Icons.Default.FormatSize,
                    contentPadding = PaddingValues(22.dp),
                    headerAction = {
                        Surface(
                            onClick = {
                                quranViewModel.setFontSize(24)
                                quranViewModel.setArabicFontStyle(ArabicFontStyle.UTHMANI)
                                sliderFontSize = 24f
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset typography",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Reset",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ScriptChoicePill(
                            title = "Uthmani",
                            selected = fontStyle == ArabicFontStyle.UTHMANI,
                            onClick = { quranViewModel.setArabicFontStyle(ArabicFontStyle.UTHMANI) },
                            modifier = Modifier.weight(1f)
                        )
                        ScriptChoicePill(
                            title = "Indo-Pak",
                            selected = fontStyle == ArabicFontStyle.INDO_PAK,
                            onClick = { quranViewModel.setArabicFontStyle(ArabicFontStyle.INDO_PAK) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    val currentDisplaySp = sliderFontSize.toInt().coerceIn(18, 42)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Arabic Text Size",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${currentDisplaySp}sp",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Slider(
                        value = sliderFontSize,
                        onValueChange = { newSize ->
                            sliderFontSize = newSize
                            quranViewModel.setFontSize(newSize.toInt())
                        },
                        valueRange = 18f..42f,
                        steps = 11,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dynamic Verse Preview Box reflecting exact chosen Script & Size in authentic RTL layout
                    val previewArabicText = when (fontStyle) {
                        ArabicFontStyle.UTHMANI -> "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
                        ArabicFontStyle.INDO_PAK -> "بِسْمِ اللهِ الرَّحْمٰنِ الرَّحِيْمِ"
                    }

                    val previewAnnotatedText: AnnotatedString = remember(previewArabicText) {
                        buildAnnotatedString {
                            append(previewArabicText)
                            append(" ")
                            appendInlineContent(id = "ayah_end", alternateText = "١")
                        }
                    }

                    val previewMarkerColor = MaterialTheme.colorScheme.secondary
                    val previewInlineContentMap = remember(currentDisplaySp, previewMarkerColor) {
                        val markerSizeSp = (currentDisplaySp * 1.34f).sp
                        mapOf(
                            "ayah_end" to InlineTextContent(
                                placeholder = Placeholder(
                                    width = markerSizeSp,
                                    height = markerSizeSp,
                                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                                )
                            ) { digits ->
                                AyahEndMedallionSymbol(
                                    arabicDigits = digits,
                                    markerColor = previewMarkerColor,
                                    fontSizeSp = currentDisplaySp
                                )
                            }
                        )
                    }

                    val previewFontFamily = when (fontStyle) {
                        ArabicFontStyle.UTHMANI -> FontFamily.Serif
                        ArabicFontStyle.INDO_PAK -> FontFamily.Default
                    }
                    val previewFontWeight = if (fontStyle == ArabicFontStyle.INDO_PAK) FontWeight.Medium else FontWeight.Normal
                    val previewLineHeight = if (fontStyle == ArabicFontStyle.INDO_PAK) (currentDisplaySp * 2.05).sp else (currentDisplaySp * 1.9).sp
                    val previewLetterSpacing = if (fontStyle == ArabicFontStyle.INDO_PAK) 0.4.sp else 0.sp

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Surah Al-Fatihah • Ayah 1",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "• ${if (fontStyle == ArabicFontStyle.UTHMANI) "Uthmani" else "Indo-Pak"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = previewAnnotatedText,
                                inlineContent = previewInlineContentMap,
                                style = TextStyle(
                                    fontSize = currentDisplaySp.sp,
                                    lineHeight = previewLineHeight,
                                    letterSpacing = previewLetterSpacing,
                                    fontFamily = previewFontFamily,
                                    fontWeight = previewFontWeight,
                                    textAlign = TextAlign.Center,
                                    textDirection = TextDirection.Rtl
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = "In the name of Allah, the Entirely Merciful, the Especially Merciful.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontStyle = FontStyle.Italic,
                                    lineHeight = 18.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // 4. Privacy, Security & Data Safety (Play Store Compliance)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Privacy & Data Safety",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "100% Offline-first local groups. Zero tracking on Qur'an recitation. Voluntary permissionary ads only for online rooms.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SecondaryEditorialButton(
                        text = "View Data Safety & Privacy Policy",
                        onClick = { showPrivacyDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 5. Clean Hambasy Studios Credit Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Quran Baanto • Version 4.0",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Created by Hambasy Studios",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            ManuscriptOrnamentDivider()

            PrimarySanctuaryButton(
                text = "Done",
                onClick = {
                    if (localNameDraft.trim() != savedProfileName) {
                        distributionViewModel?.setUserProfileName(localNameDraft.trim())
                    }
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("close_settings_button")
            )
        }
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Privacy & Data Safety",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Our Guiding Principle: Sanctuary & Peace",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "• 100% Offline by Default: Local Khatm and Amal distributions, participants, and bookmarks are stored exclusively on your device in an encrypted SQLite database.\n\n" +
                                "• Zero Recitation Tracking: Your Qur'an reading habits, ayah progress, and bookmarks are never logged or transmitted.\n\n" +
                                "• Online Rooms: Only synced when you explicitly create or join an online room. Synced securely via Google Cloud Firestore with TLS 1.3 encryption.\n\n" +
                                "• Voluntary Ad Policy: Zero banner ads anywhere in the app. Zero ads on the Qur'an reader or Home screen. A voluntary 30-second sponsored message is only suggested after creating or joining an online room to help cover cloud servers. You may decline with zero penalty.",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun ThemeOptionTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    previewBg: Color,
    previewAccent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(
            width = if (selected) 1.8.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(previewBg)
                        .border(1.dp, previewAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = previewAccent,
                        modifier = Modifier.size(17.dp)
                    )
                }

                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ScriptChoicePill(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier.height(44.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
