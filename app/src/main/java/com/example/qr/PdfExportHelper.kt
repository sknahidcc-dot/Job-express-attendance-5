package com.example.qr

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.QrConfig
import java.io.File
import java.io.FileOutputStream

object PdfExportHelper {

    fun generateAndShareA4Pdf(
        context: Context,
        config: QrConfig,
        qrBitmap: Bitmap?
    ): File? {
        try {
            val doc = PdfDocument()
            // Standard A4 page size in PostScript points: 595 x 842
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            // 1. Background
            canvas.drawColor(Color.WHITE)

            // 2. Top Banner (Red)
            paint.color = Color.parseColor("#D32F2F")
            paint.style = Paint.Style.FILL
            canvas.drawRect(0f, 0f, 595f, 130f, paint)

            // Accent Gold/Yellow bar
            paint.color = Color.parseColor("#FFD54F")
            canvas.drawRect(0f, 130f, 595f, 136f, paint)

            // Top Banner Text
            paint.color = Color.WHITE
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 28f
            canvas.drawText("JOB EXPRESS", 595f / 2, 52f, paint)

            paint.textSize = 18f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("LIBRARY ATTENDANCE SCANNER", 595f / 2, 82f, paint)

            paint.textSize = 13f
            paint.color = Color.parseColor("#FFCDD2")
            canvas.drawText("স্মার্ট ডিজিটাল হাজিরা ও লাইব্রেরি ব্যবস্থাপনা", 595f / 2, 108f, paint)

            // 3. Notice Badge
            paint.color = Color.parseColor("#FBE9E7")
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(40f, 155f, 555f, 205f, 12f, 12f, paint)

            paint.color = Color.parseColor("#D84315")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f
            canvas.drawRoundRect(40f, 155f, 555f, 205f, 12f, 12f, paint)

            paint.style = Paint.Style.FILL
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 13f
            canvas.drawText("লাইব্রেরিতে প্রবেশের সময় এবং বের হওয়ার সময় স্ক্যান করুন", 595f / 2, 185f, paint)

            // 4. Center QR Code Frame
            val qrBoxLeft = 147f
            val qrBoxTop = 225f
            val qrBoxSize = 300f

            paint.color = Color.parseColor("#F8F9FA")
            canvas.drawRoundRect(qrBoxLeft - 10, qrBoxTop - 10, qrBoxLeft + qrBoxSize + 10, qrBoxTop + qrBoxSize + 10, 16f, 16f, paint)

            paint.color = Color.parseColor("#B71C1C")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            canvas.drawRoundRect(qrBoxLeft - 10, qrBoxTop - 10, qrBoxLeft + qrBoxSize + 10, qrBoxTop + qrBoxSize + 10, 16f, 16f, paint)

            // Draw QR Bitmap
            if (qrBitmap != null) {
                val destRect = Rect(qrBoxLeft.toInt(), qrBoxTop.toInt(), (qrBoxLeft + qrBoxSize).toInt(), (qrBoxTop + qrBoxSize).toInt())
                canvas.drawBitmap(qrBitmap, null, destRect, null)
            }

            // 5. GPS Verification Chip below QR
            val chipTop = qrBoxTop + qrBoxSize + 22f
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#E8F5E9")
            canvas.drawRoundRect(80f, chipTop, 515f, chipTop + 40f, 8f, 8f, paint)

            paint.color = Color.parseColor("#2E7D32")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRoundRect(80f, chipTop, 515f, chipTop + 40f, 8f, 8f, paint)

            paint.style = Paint.Style.FILL
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("GPS Geofence: 23.5462131° N, 89.1720178° E (Radius: 300m)", 595f / 2, chipTop + 24f, paint)

            // 6. User Instructions
            val stepStartY = chipTop + 65f
            paint.textAlign = Paint.Align.LEFT
            paint.color = Color.parseColor("#1E293B")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 14f
            canvas.drawText("হাজিরা দেওয়ার সহজ নিয়মাবলি:", 50f, stepStartY, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 11.5f
            paint.color = Color.parseColor("#334155")

            val steps = listOf(
                "১. আপনার ফোনে 'Job Express Attendance' অ্যাপটি ইনস্টল করে ওপেন করুন।",
                "২. প্রথমবার অ্যাপ ওপেন করার সময় আপনার লাইব্রেরি আইডি (LID) ভেরিফাই করুন।",
                "৩. লাইব্রেরিতে প্রবেশের সময় বড় 'কিউআর কোড স্ক্যান করুন' বাটনে চাপ দিয়ে দেয়ালের কোডটি স্ক্যান করুন।",
                "৪. লাইব্রেরি থেকে বের হওয়ার সময় পুনরায় একবার স্ক্যান করুন (বের হওয়ার সময় স্বয়ংক্রিয় রেকর্ড হবে)।",
                "৫. জিপিএস সিকিউরিটি থাকার কারণে লাইব্রেরি প্রাঙ্গণের বাহিরে থেকে হাজিরা কাজ করবে না।"
            )

            var curY = stepStartY + 24f
            for (step in steps) {
                canvas.drawText(step, 50f, curY, paint)
                curY += 20f
            }

            // 7. Footer Banner
            paint.color = Color.parseColor("#F1F5F9")
            paint.style = Paint.Style.FILL
            canvas.drawRect(0f, 780f, 595f, 842f, paint)

            paint.color = Color.parseColor("#E2E8F0")
            paint.strokeWidth = 1f
            canvas.drawLine(0f, 780f, 595f, 780f, paint)

            paint.color = Color.parseColor("#475569")
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Job Express • University of Barishal • হেল্পলাইন: 01351835060", 595f / 2, 804f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 9.5f
            paint.color = Color.parseColor("#64748B")
            canvas.drawText("কারিগরি সহায়তায়: Nahid Hasan (sknahid.study@gmail.com)", 595f / 2, 824f, paint)

            doc.finishPage(page)

            // Save PDF file
            val pdfDir = File(context.cacheDir, "pdf")
            if (!pdfDir.exists()) pdfDir.mkdirs()
            val pdfFile = File(pdfDir, "Job_Express_Attendance_QR_A4.pdf")

            val fos = FileOutputStream(pdfFile)
            doc.writeTo(fos)
            fos.close()
            doc.close()

            // Open or share
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Job Express Library Attendance QR Code (A4)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "A4 PDF ডাউনলোড বা প্রিন্ট করুন").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })

            Toast.makeText(context, "A4 সাইজ PDF সফলভাবে তৈরি হয়েছে!", Toast.LENGTH_LONG).show()
            return pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "PDF তৈরিতে ত্রুটি: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            return null
        }
    }
}
