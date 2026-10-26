package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream

object RoomQrCodeHelper {

    private const val QR_EMERALD_DARK = 0xFF0A3D31.toInt()
    private const val QR_PARCHMENT_LIGHT = 0xFFFDFBF7.toInt()
    private const val QR_GOLD_ACCENT = 0xFFC59B27.toInt()

    /**
     * Generates a high-contrast, scannable QR code Bitmap for the given [passcode] and [roomName].
     * Encodes the clean passcode/invite payload so scanning or pasting into "Join Room" works immediately.
     */
    fun generateQrCodeBitmap(
        passcode: String,
        roomName: String = "",
        sizePx: Int = 640
    ): Bitmap? {
        if (passcode.isBlank()) return null
        return try {
            val payload = if (roomName.isNotBlank()) {
                "$roomName\nRoom Passcode: ${passcode.trim().uppercase()}"
            } else {
                passcode.trim().uppercase()
            }

            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 2
            )

            val bitMatrix = QRCodeWriter().encode(
                payload,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx,
                hints
            )

            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)

            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) QR_EMERALD_DARK else QR_PARCHMENT_LIGHT
                }
            }

            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Creates a styled share card image with the Room Name, QR code, and Passcode,
     * writes it to the restricted `shared_qr/` cache path, and launches the system share sheet.
     */
    fun shareRoomQrCodeImage(
        context: Context,
        roomName: String,
        passcode: String,
        typeSubtitle: String
    ) {
        try {
            val qrBitmap = generateQrCodeBitmap(passcode = passcode, roomName = roomName, sizePx = 680) ?: return
            val cardWidth = 920
            val cardHeight = 1140
            val cardBitmap = Bitmap.createBitmap(cardWidth, cardHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(cardBitmap)

            // Warm parchment background
            canvas.drawColor(QR_PARCHMENT_LIGHT)

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = QR_GOLD_ACCENT
                strokeWidth = 6f
            }
            canvas.drawRoundRect(
                RectF(28f, 28f, cardWidth - 28f, cardHeight - 28f),
                42f,
                42f,
                borderPaint
            )

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = QR_EMERALD_DARK
                textSize = 44f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val safeTitle = if (roomName.length > 28) roomName.take(26) + "…" else roomName
            canvas.drawText(safeTitle, cardWidth / 2f, 128f, titlePaint)

            val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = QR_GOLD_ACCENT
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(typeSubtitle.uppercase(), cardWidth / 2f, 174f, subtitlePaint)

            // Draw QR code centered
            val qrLeft = (cardWidth - qrBitmap.width) / 2f
            val qrTop = 220f
            canvas.drawBitmap(qrBitmap, qrLeft, qrTop, null)

            // Draw passcode pill at bottom
            val pillRect = RectF(140f, 940f, cardWidth - 140f, 1050f)
            val pillFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = QR_EMERALD_DARK
            }
            canvas.drawRoundRect(pillRect, 28f, 28f, pillFillPaint)

            val codePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 42f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(passcode.uppercase(), cardWidth / 2f, 1010f, codePaint)

            val sharedDir = File(context.cacheDir, "shared_qr").apply { mkdirs() }
            val qrFile = File(sharedDir, "quran_baanto_room_qr.png")
            FileOutputStream(qrFile).use { out ->
                cardBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                qrFile
            )

            val caption = "$roomName\nOnline Room Passcode: $passcode\nScan QR or enter passcode in Quran Baanto to join."
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, caption)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Room QR Code"))
        } catch (_: Exception) {
            // Fallback to text share if image URI fails
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "$roomName\nRoom Passcode: $passcode")
            }
            context.startActivity(Intent.createChooser(fallbackIntent, "Share Room Passcode"))
        }
    }
}
