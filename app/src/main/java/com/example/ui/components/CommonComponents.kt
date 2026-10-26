package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Clean, responsive top header that respects status-bar insets without extra dead space.
 */
@Composable
fun SanctuaryTopHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    leadingLogo: Boolean = false,
    leadingStaticGlyph: Boolean = false,
    isQuranGlyph: Boolean = true,
    badgeText: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (onBack != null) {
                        Surface(
                            onClick = onBack,
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Navigate back",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    } else if (leadingLogo) {
                        Image(
                            painter = painterResource(id = R.drawable.quran_baanto_logo_circle),
                            contentDescription = "Quran Baanto Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f),
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    } else if (leadingStaticGlyph) {
                        SanctuaryGeometricGlyph(
                            isQuranGlyph = isQuranGlyph,
                            size = 38.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (badgeText != null) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    border = BorderStroke(
                                        0.6.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Text(
                                        text = badgeText,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        if (!subtitle.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    content = actions
                )
            }

            HorizontalDivider(
                thickness = 0.7.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Clean, lightweight container card without layout-thrashing animations so lists scroll at 60fps.
 */
@Composable
fun ResizableSectionContainer(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    badgeText: String? = null,
    badgeColor: Color = MaterialTheme.colorScheme.primaryContainer,
    badgeContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    icon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    onClick: (() -> Unit)? = null,
    headerAction: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val borderStroke = BorderStroke(1.dp, borderColor.copy(alpha = 0.75f))

    val innerContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding)
        ) {
            if (title != null || badgeText != null || headerAction != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (icon != null) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            if (title != null) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (!subtitle.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (headerAction != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        headerAction()
                    } else if (badgeText != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = badgeColor
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = badgeContentColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            content()
        }
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = shape,
            color = containerColor,
            border = borderStroke,
            modifier = modifier.fillMaxWidth()
        ) {
            innerContent()
        }
    } else {
        Surface(
            shape = shape,
            color = containerColor,
            border = borderStroke,
            modifier = modifier.fillMaxWidth()
        ) {
            innerContent()
        }
    }
}

/**
 * Clean static vector motion-graphic style icon for Qur'an (Rub el Hizb Open Book)
 * and Khatm/Amal Circles (Interlocking Rotation Nodes). Replaces bitmap images.
 */
@Composable
fun SanctuaryGeometricGlyph(
    isQuranGlyph: Boolean,
    size: Dp = 48.dp,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    accentColor: Color = MaterialTheme.colorScheme.secondary,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .border(
                width = 1.dp,
                color = primaryColor.copy(alpha = 0.25f),
                shape = RoundedCornerShape(14.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.56f)) {
            if (isQuranGlyph) {
                drawQuranVectorGlyph(primaryColor, accentColor)
            } else {
                drawKhatmCycleVectorGlyph(primaryColor, accentColor)
            }
        }
    }
}

private fun DrawScope.drawQuranVectorGlyph(
    primaryColor: Color,
    accentColor: Color
) {
    val w = size.width
    val h = size.height
    val stroke = Stroke(width = w * 0.085f, cap = StrokeCap.Round)

    // Outer rotated geometric frame
    val sqSize = w * 0.72f
    val topLeft = Offset((w - sqSize) / 2f, (h - sqSize) / 2f)
    rotate(degrees = 45f, pivot = center) {
        drawRoundRect(
            color = accentColor.copy(alpha = 0.6f),
            topLeft = topLeft,
            size = Size(sqSize, sqSize),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
            style = Stroke(width = w * 0.065f)
        )
    }

    // Upright square
    drawRoundRect(
        color = primaryColor,
        topLeft = topLeft,
        size = Size(sqSize, sqSize),
        cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
        style = stroke
    )

    // Center minimalist open-book / focal point
    drawCircle(
        color = accentColor,
        radius = w * 0.11f,
        center = center
    )
}

private fun DrawScope.drawKhatmCycleVectorGlyph(
    primaryColor: Color,
    accentColor: Color
) {
    val w = size.width
    val r = w * 0.38f
    val strokeWidth = w * 0.09f

    // Segmented orbital ring representing contiguous cycle
    drawArc(
        color = primaryColor,
        startAngle = -75f,
        sweepAngle = 150f,
        useCenter = false,
        topLeft = Offset(center.x - r, center.y - r),
        size = Size(r * 2, r * 2),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
    drawArc(
        color = accentColor,
        startAngle = 105f,
        sweepAngle = 150f,
        useCenter = false,
        topLeft = Offset(center.x - r, center.y - r),
        size = Size(r * 2, r * 2),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )

    // Inner diamond core
    val innerSize = w * 0.28f
    val innerTopLeft = Offset(center.x - innerSize / 2f, center.y - innerSize / 2f)
    rotate(degrees = 45f, pivot = center) {
        drawRoundRect(
            color = primaryColor,
            topLeft = innerTopLeft,
            size = Size(innerSize, innerSize),
            cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
        )
    }
}

/**
 * Fast, zero-path-allocation octagonal number badge for 60fps scrolling in Para & Surah lists.
 */
@Composable
fun OctagonalNumberMedallion(
    number: Int,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val s = this.size.minDimension
            val boxSize = s * 0.68f
            val offset = (s - boxSize) / 2f
            val topLeft = Offset(offset, offset)
            val rectSize = Size(boxSize, boxSize)
            val corner = CornerRadius(s * 0.08f, s * 0.08f)

            // Soft fill
            rotate(degrees = 45f, pivot = center) {
                drawRoundRect(
                    color = containerColor.copy(alpha = 0.65f),
                    topLeft = topLeft,
                    size = rectSize,
                    cornerRadius = corner
                )
                drawRoundRect(
                    color = secondaryColor.copy(alpha = 0.55f),
                    topLeft = topLeft,
                    size = rectSize,
                    cornerRadius = corner,
                    style = Stroke(width = 1.1.dp.toPx())
                )
            }

            drawRoundRect(
                color = containerColor.copy(alpha = 0.85f),
                topLeft = topLeft,
                size = rectSize,
                cornerRadius = corner
            )
            drawRoundRect(
                color = primaryColor.copy(alpha = 0.7f),
                topLeft = topLeft,
                size = rectSize,
                cornerRadius = corner,
                style = Stroke(width = 1.2.dp.toPx())
            )
        }

        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.31f).sp
            ),
            color = primaryColor,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Subtle horizontal divider with a centered diamond accent.
 */
@Composable
fun ManuscriptOrnamentDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 0.7.dp,
            color = color.copy(alpha = 0.3f)
        )
        Canvas(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .size(8.dp)
        ) {
            rotate(degrees = 45f, pivot = center) {
                drawRect(color = color)
            }
        }
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 0.7.dp,
            color = color.copy(alpha = 0.3f)
        )
    }
}

/**
 * Primary action button with crisp geometry.
 */
@Composable
fun PrimarySanctuaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 13.dp)
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        ),
        contentPadding = contentPadding,
        modifier = modifier.height(48.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Secondary outlined button.
 */
@Composable
fun SecondaryEditorialButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 13.dp)
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) accentColor.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = accentColor
        ),
        contentPadding = contentPadding,
        modifier = modifier.height(48.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Harmonious color palette for visually distinguishing member segments in the 30-Para allocation bar.
 */
@Composable
fun rememberAllocationPalette(): List<Color> {
    return listOf(
        MaterialTheme.colorScheme.primary,
        Color(0xFFC5943A),
        Color(0xFF2E7D6B),
        Color(0xFF4A6B82),
        Color(0xFF8B5E3C),
        Color(0xFF5A7D3E),
        Color(0xFF7B527A),
        Color(0xFF3B7A8C)
    )
}

/**
 * Pure visual multi-color segmented allocation bar showing how the 30 Paras (or Amal target)
 * are divided among members, with no extra text or explanations.
 */
@Composable
fun ContiguousParaBlockBar(
    blockSizes: List<Int>,
    totalTarget: Int = 30,
    modifier: Modifier = Modifier
) {
    val palette = rememberAllocationPalette()
    val sum = blockSizes.sum()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .border(0.7.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(999.dp))
            .padding(1.5.dp),
        horizontalArrangement = Arrangement.spacedBy(2.5.dp)
    ) {
        if (sum <= 0) {
            Box(modifier = Modifier.fillMaxSize())
        } else {
            blockSizes.filter { it > 0 }.forEachIndexed { idx, count ->
                val weight = (count.toFloat() / maxOf(sum, totalTarget).toFloat()).coerceAtLeast(0.02f)
                Box(
                    modifier = Modifier
                        .weight(weight)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .background(palette[idx % palette.size])
                )
            }
            if (sum < totalTarget) {
                val remainingWeight = ((totalTarget - sum).toFloat() / totalTarget.toFloat()).coerceAtLeast(0.02f)
                Box(
                    modifier = Modifier
                        .weight(remainingWeight)
                        .fillMaxHeight()
                        .background(Color.Transparent)
                )
            }
        }
    }
}

/**
 * Calm, editorial loading state card matching the Sanctuary design language.
 */
@Composable
fun SanctuaryLoadingState(
    message: String = "Loading sessions...",
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 42.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            androidx.compose.material3.CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(34.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Clean empty state card.
 */
@Composable
fun PeacefulEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (actionText != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(22.dp))
                PrimarySanctuaryButton(
                    text = actionText,
                    onClick = onActionClick
                )
            }
        }
    }
}

data class ParsedAyah(
    val arabicText: String,
    val ayahNumber: Int,
    val arabicDigits: String
)

private val ARABIC_DIGITS = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

fun Int.toArabicNumerals(): String {
    if (this <= 0) return "٠"
    val str = this.toString()
    val sb = StringBuilder(str.length)
    for (ch in str) {
        if (ch in '0'..'9') {
            sb.append(ARABIC_DIGITS[ch - '0'])
        } else {
            sb.append(ch)
        }
    }
    return sb.toString()
}

/**
 * Draws an authentic Mushaf circular Ayah end medallion with the Arabic verse number
 * strictly centered inside the circle across all Android devices and fonts.
 */
@Composable
fun AyahEndMedallionSymbol(
    arabicDigits: String,
    markerColor: Color,
    fontSizeSp: Int,
    modifier: Modifier = Modifier
) {
    val digitFontSize = when {
        arabicDigits.length >= 3 -> (fontSizeSp * 0.40f).sp
        arabicDigits.length == 2 -> (fontSizeSp * 0.47f).sp
        else -> (fontSizeSp * 0.54f).sp
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val diameter = size.minDimension
            val outerRadius = (diameter / 2f) * 0.86f
            val innerRadius = outerRadius * 0.80f
            val strokeOuter = (diameter * 0.055f).coerceAtLeast(1.1.dp.toPx())
            val strokeInner = (diameter * 0.028f).coerceAtLeast(0.6.dp.toPx())

            // Subtle inner fill so digits have crisp contrast
            drawCircle(
                color = markerColor.copy(alpha = 0.10f),
                radius = outerRadius,
                center = center
            )

            // Outer circle ring
            drawCircle(
                color = markerColor,
                radius = outerRadius,
                center = center,
                style = Stroke(width = strokeOuter)
            )

            // Inner concentric manuscript ring
            drawCircle(
                color = markerColor.copy(alpha = 0.5f),
                radius = innerRadius,
                center = center,
                style = Stroke(width = strokeInner)
            )

            // Four cardinal rosette accents on the outer rim
            val dotRadius = (diameter * 0.045f).coerceAtLeast(1.dp.toPx())
            drawCircle(
                color = markerColor,
                radius = dotRadius,
                center = Offset(center.x, center.y - outerRadius)
            )
            drawCircle(
                color = markerColor,
                radius = dotRadius,
                center = Offset(center.x, center.y + outerRadius)
            )
            drawCircle(
                color = markerColor,
                radius = dotRadius,
                center = Offset(center.x - outerRadius, center.y)
            )
            drawCircle(
                color = markerColor,
                radius = dotRadius,
                center = Offset(center.x + outerRadius, center.y)
            )
        }

        Text(
            text = arabicDigits,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = digitFontSize,
                lineHeight = digitFontSize
            ),
            color = markerColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * Fast, zero-regex Ayah parser for 120 FPS Quran reader list flattening.
 * Strips trailing '۝<digits>' or '﴿<digits>﴾' markers so each Ayah displays
 * its true verse number once inside the proper Arabic ayah medallion.
 */
fun parseAyah(rawVerse: String, fallbackNumber: Int): ParsedAyah {
    val trimmed = rawVerse.trim()

    // 1. Check for Arabic End of Ayah symbol '۝' (U+06DD) used in paras.json & surahs.json
    val ayahSymbolIdx = trimmed.indexOf('۝')
    if (ayahSymbolIdx != -1) {
        val textPart = trimmed.substring(0, ayahSymbolIdx).trim()
        val digitsPart = trimmed.substring(ayahSymbolIdx + 1).trim()
        var parsedNum = 0
        for (ch in digitsPart) {
            val digitVal = when (ch) {
                in '٠'..'٩' -> ch - '٠'
                in '0'..'9' -> ch - '0'
                else -> -1
            }
            if (digitVal >= 0) {
                parsedNum = parsedNum * 10 + digitVal
            }
        }
        val finalNum = if (parsedNum > 0) parsedNum else fallbackNumber
        return ParsedAyah(
            arabicText = textPart,
            ayahNumber = finalNum,
            arabicDigits = finalNum.toArabicNumerals()
        )
    }

    // 2. Check for ornate parentheses '﴿...﴾' if present
    val openIdx = trimmed.lastIndexOf('﴿')
    val closeIdx = trimmed.lastIndexOf('﴾')
    if (openIdx != -1 && closeIdx > openIdx) {
        val textPart = trimmed.substring(0, openIdx).trim()
        val digitsPart = trimmed.substring(openIdx + 1, closeIdx).trim()
        var parsedNum = 0
        for (ch in digitsPart) {
            val digitVal = when (ch) {
                in '٠'..'٩' -> ch - '٠'
                in '0'..'9' -> ch - '0'
                else -> -1
            }
            if (digitVal >= 0) {
                parsedNum = parsedNum * 10 + digitVal
            }
        }
        val finalNum = if (parsedNum > 0) parsedNum else fallbackNumber
        return ParsedAyah(
            arabicText = textPart,
            ayahNumber = finalNum,
            arabicDigits = finalNum.toArabicNumerals()
        )
    }

    return ParsedAyah(
        arabicText = trimmed,
        ayahNumber = fallbackNumber,
        arabicDigits = fallbackNumber.toArabicNumerals()
    )
}

