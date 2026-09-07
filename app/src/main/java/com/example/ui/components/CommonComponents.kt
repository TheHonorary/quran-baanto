package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

private val ARABIC_DIGITS_CHARS = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
private val AYAH_REGEX = Regex("""^(.*?)\s*(\d+)$""")
private val PARSED_AYAH_CACHE = java.util.concurrent.ConcurrentHashMap<String, ParsedAyah>()

private val ARABIC_NUMERALS_CACHE: Array<String> = Array(350) { num ->
    if (num == 0) "٠"
    else {
        val str = num.toString()
        val sb = StringBuilder(str.length)
        for (ch in str) {
            if (ch in '0'..'9') sb.append(ARABIC_DIGITS_CHARS[ch - '0']) else sb.append(ch)
        }
        sb.toString()
    }
}

/**
 * Converts Western ASCII digits (0-9) to standard Arabic-Indic digits (٠-٩).
 * Uses zero-allocation cached array for standard Ayah numbers (0..349).
 */
fun toArabicIndicDigits(number: Int): String {
    if (number in 0 until 350) {
        return ARABIC_NUMERALS_CACHE[number]
    }
    val str = number.toString()
    val sb = StringBuilder(str.length)
    for (ch in str) {
        if (ch in '0'..'9') {
            sb.append(ARABIC_DIGITS_CHARS[ch - '0'])
        } else {
            sb.append(ch)
        }
    }
    return sb.toString()
}

/**
 * Parses an Ayah string that may end with a symbol or number like " ۝12" or " 12"
 * and returns the clean verse text along with the integer Ayah number.
 * Thread-safe cached result prevents redundant string and regex parsing.
 */
data class ParsedAyah(
    val arabicText: String,
    val ayahNumber: Int
)

fun parseAyah(rawVerse: String, fallbackNumber: Int): ParsedAyah {
    return PARSED_AYAH_CACHE.getOrPut(rawVerse) {
        val trimmed = rawVerse.trim()
        val symbolIndex = trimmed.lastIndexOf('۝')
        if (symbolIndex >= 0) {
            val text = trimmed.substring(0, symbolIndex).trim()
            val numStr = trimmed.substring(symbolIndex + 1).trim()
            val num = numStr.toIntOrNull() ?: fallbackNumber
            ParsedAyah(arabicText = text, ayahNumber = num)
        } else {
            val match = AYAH_REGEX.find(trimmed)
            if (match != null) {
                val text = match.groupValues[1].trim()
                val num = match.groupValues[2].toIntOrNull() ?: fallbackNumber
                ParsedAyah(arabicText = text, ayahNumber = num)
            } else {
                ParsedAyah(arabicText = trimmed, ayahNumber = fallbackNumber)
            }
        }
    }
}

/**
 * Beautiful, authentic Mushaf Ayah End Marker with an ornate rosette/circular frame
 * and centered Arabic-Indic numerals.
 */
@Composable
fun AyahEndMarker(
    ayahNumber: Int,
    fontSizeSp: Int,
    markerColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val sizeDp = (fontSizeSp * 1.35f).dp
    val arabicDigits = toArabicIndicDigits(ayahNumber)

    Box(
        modifier = modifier
            .size(sizeDp)
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f * 0.90f

            // Outer decorative ring
            drawCircle(
                color = markerColor.copy(alpha = 0.85f),
                radius = radius,
                center = center,
                style = Stroke(width = 1.3.dp.toPx())
            )
            // Inner delicate concentric ring
            drawCircle(
                color = markerColor.copy(alpha = 0.35f),
                radius = radius * 0.76f,
                center = center,
                style = Stroke(width = 0.8.dp.toPx())
            )
            // 4 cardinal ornamental pips
            val pipRadius = 1.2.dp.toPx()
            for (i in 0 until 4) {
                val angle = Math.toRadians((i * 90.0) + 45.0)
                val x = center.x + (radius * 0.88f * Math.cos(angle)).toFloat()
                val y = center.y + (radius * 0.88f * Math.sin(angle)).toFloat()
                drawCircle(
                    color = markerColor.copy(alpha = 0.7f),
                    radius = pipRadius,
                    center = Offset(x, y)
                )
            }
        }

        // The Ayah number in Arabic-Indic numerals centered inside
        Text(
            text = arabicDigits,
            style = TextStyle(
                fontSize = (fontSizeSp * 0.44f).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default,
                textAlign = TextAlign.Center,
                color = textColor
            ),
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun ParaNumberBadge(
    number: Int,
    modifier: Modifier = Modifier
) {
    val formattedNumber = String.format("%02d", number)
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = formattedNumber,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                letterSpacing = 0.2.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.1).sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        if (!subtitle.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Clean, branded Quran Baanto official logo emblem with rounded corners and subtle border.
 */
@Composable
fun QuranBaantoLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(14.dp)
) {
    Image(
        painter = painterResource(id = R.drawable.quran_baanto_logo),
        contentDescription = "Quran Baanto Logo",
        modifier = modifier
            .size(size)
            .clip(shape)
    )
}

@Composable
fun EditorialDivider(
    modifier: Modifier = Modifier
) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        thickness = 0.6.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    )
}

@Composable
fun PeacefulEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
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
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(22.dp))
            Button(
                onClick = onActionClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = actionText)
            }
        }
    }
}
