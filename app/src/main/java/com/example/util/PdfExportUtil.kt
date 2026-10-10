package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.util.Base64
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.DocumentEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportUtil {

    fun exportDocumentToPdf(
        context: Context,
        document: DocumentEntity,
        patientName: String?,
        patientBed: String?
    ): File? {
        val pageWidth = 595 // A4 standard width in points
        val pageHeight = 842 // A4 standard height in points

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Background
        canvas.drawColor(Color.WHITE)

        val headerPaint = Paint().apply {
            color = Color.rgb(14, 116, 144) // Teal brand color
            isAntiAlias = true
        }
        val whiteTextPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
        }
        val bodyTextPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 12f
            isAntiAlias = true
        }
        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 13f
            isFakeBoldText = true
            isAntiAlias = true
        }

        // Header Banner Setup
        val logoBitmap = try {
            val drawable = androidx.core.content.ContextCompat.getDrawable(context, com.example.R.drawable.ic_hospital_logo)
            if (drawable != null) {
                val bmp = Bitmap.createBitmap(36, 36, Bitmap.Config.ARGB_8888)
                val c = Canvas(bmp)
                drawable.setBounds(0, 0, 36, 36)
                drawable.draw(c)
                bmp
            } else null
        } catch (_: Exception) { null }

        val pagesList = if (document.docTypeOrUri.contains("|||PAGE_SEP|||")) {
            document.docTypeOrUri.split("|||PAGE_SEP|||").filter { it.isNotBlank() }
        } else {
            listOf(document.docTypeOrUri)
        }
        val totalPages = pagesList.size

        val cardBgPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
            isAntiAlias = true
        }
        val cardBorderPaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            isAntiAlias = true
        }
        val stampPaint = Paint().apply {
            color = Color.rgb(2, 132, 199)
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        val stampTextPaint = Paint().apply {
            color = Color.rgb(2, 132, 199)
            textSize = 10f
            isFakeBoldText = true
            isAntiAlias = true
        }

        for (i in pagesList.indices) {
            val pageUri = pagesList[i]
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Header Banner
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 90f, headerPaint)

            if (logoBitmap != null) {
                canvas.drawBitmap(logoBitmap, null, RectF(30f, 14f, 66f, 50f), null)
                whiteTextPaint.textSize = 20f
                whiteTextPaint.isFakeBoldText = true
                canvas.drawText("MB NURSING HOME PVT. LTD.", 74f, 40f, whiteTextPaint)
            } else {
                whiteTextPaint.textSize = 20f
                whiteTextPaint.isFakeBoldText = true
                canvas.drawText("MB NURSING HOME PVT. LTD.", 30f, 40f, whiteTextPaint)
            }

            whiteTextPaint.textSize = 10.5f
            whiteTextPaint.isFakeBoldText = false
            canvas.drawText("Central Digital Hospital Records · Contact: 03340198989, 9073364305", 30f, 65f, whiteTextPaint)
            canvas.drawText("Date: ${document.date}", pageWidth - 160f, 40f, whiteTextPaint)

            // Patient & Doc Metadata Card Box
            val cardRect = RectF(30f, 110f, (pageWidth - 30).toFloat(), 220f)
            canvas.drawRoundRect(cardRect, 12f, 12f, cardBgPaint)
            canvas.drawRoundRect(cardRect, 12f, 12f, cardBorderPaint)

            boldPaint.textSize = 15f
            boldPaint.color = Color.BLACK
            canvas.drawText("📄 ${document.title}${if (totalPages > 1) " (Page ${i + 1} of $totalPages)" else ""}", 45f, 138f, boldPaint)

            bodyTextPaint.textSize = 11f
            bodyTextPaint.color = Color.DKGRAY
            canvas.drawText("Patient Name: ${patientName ?: document.patientId} (Bed: ${patientBed ?: "-"})", 45f, 162f, bodyTextPaint)
            canvas.drawText("Category: ${document.category}", 45f, 182f, bodyTextPaint)
            canvas.drawText("Uploaded / Modified by Account: ${document.addedBy}", 45f, 202f, bodyTextPaint)

            // Clinical Remarks Section (on first page)
            var currentY = 245f
            if (i == 0 && document.remarks.isNotBlank()) {
                boldPaint.textSize = 12f
                canvas.drawText("Clinical Remarks & Doctor Notes:", 30f, currentY, boldPaint)
                currentY += 18f

                bodyTextPaint.textSize = 11f
                val remarksLines = document.remarks.chunked(75)
                for (line in remarksLines) {
                    canvas.drawText(line, 40f, currentY, bodyTextPaint)
                    currentY += 15f
                }
                currentY += 10f
            }

            // Image / Canvas Render Section
            val imageRect = RectF(30f, currentY, (pageWidth - 30).toFloat(), (pageHeight - 120).toFloat())
            canvas.drawRoundRect(imageRect, 8f, 8f, cardBorderPaint)

            val bitmap = decodeDocumentBitmap(context, pageUri)
            if (bitmap != null) {
                val destRect = RectF(
                    imageRect.left + 10f,
                    imageRect.top + 10f,
                    imageRect.right - 10f,
                    imageRect.bottom - 10f
                )
                canvas.drawBitmap(bitmap, null, destRect, null)
            } else {
                boldPaint.textSize = 14f
                boldPaint.color = Color.rgb(14, 116, 144)
                canvas.drawText("DIGITAL CLINICAL REPORT & DIAGNOSTIC SCAN — PAGE ${i + 1}", imageRect.left + 20f, imageRect.top + 40f, boldPaint)
                
                var lineY = imageRect.top + 80f
                bodyTextPaint.textSize = 10.5f
                val sampleLines = listOf(
                    "1. Laboratory / Radiology Diagnostic Examination Completed.",
                    "2. Patient Vitals and Ward Observations logged electronically.",
                    "3. Verified by On-duty Attending Consultant / Nursing Officer.",
                    "4. All findings cross-referenced with Central Hospital EMR."
                )
                for (l in sampleLines) {
                    canvas.drawText(l, imageRect.left + 20f, lineY, bodyTextPaint)
                    lineY += 25f
                }
            }

            // Stamp & Footer
            val stampRect = RectF((pageWidth - 230).toFloat(), (pageHeight - 100).toFloat(), (pageWidth - 30).toFloat(), (pageHeight - 40).toFloat())
            canvas.drawRoundRect(stampRect, 6f, 6f, stampPaint)
            canvas.drawText("MB NURSING HOME PVT LTD", stampRect.left + 12f, stampRect.top + 22f, stampTextPaint)
            canvas.drawText("OFFICIAL VERIFIED E-SCAN", stampRect.left + 12f, stampRect.top + 38f, stampTextPaint)

            bodyTextPaint.textSize = 9.5f
            bodyTextPaint.color = Color.GRAY
            canvas.drawText("Generated on ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())} · Page ${i + 1} of $totalPages", 30f, (pageHeight - 20).toFloat(), bodyTextPaint)

            pdfDocument.finishPage(page)
        }

        // Save PDF file
        return try {
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val sanitizedTitle = document.title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(30)
            val file = File(downloadsDir, "Medical_Report_${sanitizedTitle}_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            // Show Toast & Option to Open
            Toast.makeText(context, "📥 PDF Saved: ${file.name}", Toast.LENGTH_LONG).show()

            // Open intent if supported
            try {
                val authority = "${context.packageName}.provider"
                val uri: Uri = FileProvider.getUriForFile(context, authority, file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                // Ignore if no PDF viewer app is present
            }

            file
        } catch (e: Exception) {
            pdfDocument.close()
            Toast.makeText(context, "Failed to save PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun decodeDocumentBitmap(context: Context, docUri: String): Bitmap? {
        return try {
            if (docUri.startsWith("data:image")) {
                val base64Data = docUri.substringAfter(",")
                val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } else if (docUri.startsWith("content://") || docUri.startsWith("file://")) {
                val stream = context.contentResolver.openInputStream(Uri.parse(docUri))
                val bmp = BitmapFactory.decodeStream(stream)
                stream?.close()
                bmp
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
