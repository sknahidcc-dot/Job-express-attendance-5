package com.example.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.example.data.QrConfig
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import org.json.JSONObject
import java.util.EnumMap

object QrCodeGenerator {

    fun createQrDataJson(config: QrConfig): String {
        return JSONObject().apply {
            put("app", "JobExpress")
            put("url", config.scriptUrl)
            put("lat", config.latitude)
            put("lng", config.longitude)
            put("radius", config.radiusMeters)
        }.toString()
    }

    fun parseQrData(rawText: String): QrConfig? {
        val trimmed = rawText.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return try {
                val json = JSONObject(trimmed)
                val url = json.optString("url", "")
                val lat = json.optDouble("lat", 23.5462131)
                val lng = json.optDouble("lng", 89.1720178)
                val radius = json.optDouble("radius", 300.0)
                if (url.isNotBlank()) {
                    QrConfig(scriptUrl = url, latitude = lat, longitude = lng, radiusMeters = radius)
                } else null
            } catch (e: Exception) {
                null
            }
        }
        // Fallback: If it's a direct Google Apps Script URL
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return QrConfig(
                scriptUrl = trimmed,
                latitude = 23.5462131,
                longitude = 89.1720178,
                radiusMeters = 300.0
            )
        }
        return null
    }

    fun generateQrBitmap(
        content: String,
        width: Int = 600,
        height: Int = 600,
        fgColor: Int = Color.BLACK,
        bgColor: Int = Color.WHITE
    ): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
                put(EncodeHintType.MARGIN, 1)
            }
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) fgColor else bgColor)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
