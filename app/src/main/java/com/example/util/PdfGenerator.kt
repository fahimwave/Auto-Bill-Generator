package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.DutyLog
import com.example.data.UserProfile
import com.example.util.DayDutyGroup
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    /**
     * Generates a strictly Black and White A4 PDF replicating the exact LaTeX layout
     * specified for the IUB Academic Success Center (ASC) Student-On-Duty Bill.
     * Supports smart multi-page pagination if duty rows exceed 1 page, allowing
     * maximum bottom margin tolerance (down to 0.5 inches / 36 pt) before breaking.
     */
    fun generateAscBillPdf(
        context: Context,
        profile: UserProfile,
        logs: List<DutyLog>,
        billingTitleOverride: String? = null
    ): File {
        val pdfDocument = PdfDocument()

        // Standard A4 dimensions in PostScript points (72 points/inch)
        val pageWidth = 595
        val pageHeight = 842

        val dayGroups = TimeUtil.groupLogsByDate(logs)
        val grandTotalHours = dayGroups.sumOf { it.totalHours }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.SERIF
        }

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 0.6f
        }

        val leftMargin = 55f
        val rightMargin = pageWidth - 55f
        val tableWidth = rightMargin - leftMargin

        val colDateWidth = tableWidth * 0.23f
        val colDayWidth = tableWidth * 0.27f
        val colFromWidth = tableWidth * 0.165f
        val colToWidth = tableWidth * 0.165f
        val colHoursWidth = tableWidth * 0.17f

        val colX0 = leftMargin
        val colX1 = colX0 + colDateWidth
        val colX2 = colX1 + colDayWidth
        val colX3 = colX2 + colFromWidth
        val colX4 = colX3 + colToWidth
        val colX5 = rightMargin

        val baseRowHeight = 22f

        // Calculate total shift row count across all groups
        val totalShiftRows = dayGroups.sumOf { it.shifts.size.coerceAtLeast(1) }

        // Determine if we can comfortably or tightly fit in 1 page:
        // Header height: ~195 pt
        // Total row: 22 pt, signature gap: ~25 pt, signature block: ~45 pt => ~92 pt for footer
        // 842 - 36 (0.5 inch bottom margin) = 806 pt usable limit
        val usableSinglePageHeight = 806f
        val neededSinglePageHeight = 195f + (totalShiftRows * baseRowHeight) + 92f

        // Check if compression allows 1-page fit down to 0.5 inch margin:
        // Even if baseRowHeight makes it 830, shrinking rowHeight slightly to 19f can fit up to 26-28 shifts!
        val canFitSinglePage = neededSinglePageHeight <= usableSinglePageHeight ||
                (190f + (totalShiftRows * 19.5f) + 85f) <= usableSinglePageHeight

        val rowHeight = if (canFitSinglePage && neededSinglePageHeight > 780f) {
            19.5f
        } else {
            baseRowHeight
        }

        val billingTitle = billingTitleOverride ?: TimeUtil.getBillingMonthTitle(logs)

        if (canFitSinglePage) {
            // === SINGLE PAGE RENDERING ===
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            val headerStartY = if (neededSinglePageHeight > 780f) 38f else 48f
            var currentY = drawHeaderAndDetails(canvas, pageWidth, leftMargin, textPaint, profile, billingTitle, headerStartY)

            currentY = drawTableHeader(canvas, colX0, colX1, colX2, colX3, colX4, colX5, currentY, rowHeight, linePaint, textPaint)

            for (group in dayGroups) {
                currentY = drawDateGroupRows(canvas, group, colX0, colX1, colX2, colX3, colX4, colX5, currentY, rowHeight, linePaint, textPaint)
            }

            currentY = drawTotalHoursRow(canvas, colX0, colX4, colX5, currentY, rowHeight, grandTotalHours, linePaint, textPaint)

            val sigGap = if (neededSinglePageHeight > 780f) 28f else 40f
            drawSignatures(canvas, profile, leftMargin, rightMargin, currentY + sigGap, textPaint)

            pdfDocument.finishPage(page)
        } else {
            // === MULTI-PAGE RENDERING ===
            // Page 1: Header + As many date groups as can fit up to bottom margin 0.5 inch (806 pt)
            val page1MaxY = 806f
            var page1Index = 1
            val pageInfo1 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, page1Index).create()
            val page1 = pdfDocument.startPage(pageInfo1)
            val canvas1 = page1.canvas
            canvas1.drawColor(Color.WHITE)

            var currentY1 = drawHeaderAndDetails(canvas1, pageWidth, leftMargin, textPaint, profile, billingTitle, 48f)
            currentY1 = drawTableHeader(canvas1, colX0, colX1, colX2, colX3, colX4, colX5, currentY1, baseRowHeight, linePaint, textPaint)

            var splitGroupIndex = 0
            while (splitGroupIndex < dayGroups.size) {
                val group = dayGroups[splitGroupIndex]
                val groupHeight = group.shifts.size.coerceAtLeast(1) * baseRowHeight
                if (currentY1 + groupHeight > page1MaxY) {
                    break
                }
                currentY1 = drawDateGroupRows(canvas1, group, colX0, colX1, colX2, colX3, colX4, colX5, currentY1, baseRowHeight, linePaint, textPaint)
                splitGroupIndex++
            }

            pdfDocument.finishPage(page1)

            // Page 2: Continuation Table Header + Remaining date groups + Total Hours + Signatures
            val pageInfo2 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create()
            val page2 = pdfDocument.startPage(pageInfo2)
            val canvas2 = page2.canvas
            canvas2.drawColor(Color.WHITE)

            var currentY2 = 50f
            // Continuation header
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize = 11f
            textPaint.isFakeBoldText = true
            canvas2.drawText("Academic Success Center (ASC) - Student-On-Duty Bill (Continuation)", (pageWidth / 2).toFloat(), currentY2, textPaint)
            currentY2 += 18f

            currentY2 = drawTableHeader(canvas2, colX0, colX1, colX2, colX3, colX4, colX5, currentY2, baseRowHeight, linePaint, textPaint)

            for (i in splitGroupIndex until dayGroups.size) {
                val group = dayGroups[i]
                currentY2 = drawDateGroupRows(canvas2, group, colX0, colX1, colX2, colX3, colX4, colX5, currentY2, baseRowHeight, linePaint, textPaint)
            }

            currentY2 = drawTotalHoursRow(canvas2, colX0, colX4, colX5, currentY2, baseRowHeight, grandTotalHours, linePaint, textPaint)

            drawSignatures(canvas2, profile, leftMargin, rightMargin, currentY2 + 45f, textPaint)

            pdfDocument.finishPage(page2)
        }

        // Format filename per requirement: "ASC Bill মাসের নাম, id, user এর নাম"
        val monthString = try {
            if (billingTitleOverride != null && billingTitleOverride.startsWith("Bill of ")) {
                billingTitleOverride.removePrefix("Bill of ").trim().replace(" ", "_")
            } else if (logs.isNotEmpty()) {
                val latest = logs.maxByOrNull { it.date }
                val date = latest?.let { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(it.date) } ?: Date()
                SimpleDateFormat("MMMM_yyyy", Locale.US).format(date)
            } else {
                SimpleDateFormat("MMMM_yyyy", Locale.US).format(Date())
            }
        } catch (e: Exception) {
            SimpleDateFormat("MMMM_yyyy", Locale.US).format(Date())
        }

        val cleanId = profile.studentId.trim().replace(Regex("[^a-zA-Z0-9]"), "_").ifBlank { "2310866" }
        val cleanName = profile.name.trim().replace(Regex("[^a-zA-Z0-9]"), "_").ifBlank { "Student" }

        val fileName = "ASC_Bill_${monthString}_${cleanId}_${cleanName}.pdf"
        val outputFile = File(context.cacheDir, fileName)
        FileOutputStream(outputFile).use { outStream ->
            pdfDocument.writeTo(outStream)
        }
        pdfDocument.close()

        return outputFile
    }

    private fun drawHeaderAndDetails(
        canvas: Canvas,
        pageWidth: Int,
        leftMargin: Float,
        textPaint: Paint,
        profile: UserProfile,
        billingTitle: String,
        startY: Float
    ): Float {
        var currentY = startY

        // 2. Centered Academic Header
        textPaint.textSize = 15f
        textPaint.isFakeBoldText = true
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("Independent University, Bangladesh", (pageWidth / 2).toFloat(), currentY, textPaint)

        currentY += 18f
        textPaint.textSize = 12f
        canvas.drawText("Academic Success Center (ASC)", (pageWidth / 2).toFloat(), currentY, textPaint)

        currentY += 16f
        textPaint.textSize = 11f
        canvas.drawText("Student-On-Duty Bill", (pageWidth / 2).toFloat(), currentY, textPaint)

        currentY += 24f

        // 3. Student Details (Left Aligned)
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = 10f

        val details = listOf(
            Pair("Name:", profile.name.trim()),
            Pair("ID:", profile.studentId.trim()),
            Pair("Account no:", profile.accountNo.trim()),
            Pair("Bank Name:", profile.bankName.ifBlank { "MTB" }.trim())
        )

        for ((label, value) in details) {
            textPaint.isFakeBoldText = true
            val labelWidth = textPaint.measureText("$label ")
            canvas.drawText(label, leftMargin, currentY, textPaint)

            textPaint.isFakeBoldText = false
            canvas.drawText(value, leftMargin + labelWidth, currentY, textPaint)
            currentY += 14.5f
        }

        currentY += 12f

        // 4. Billing Month Header (Centered Bold)
        textPaint.isFakeBoldText = true
        textPaint.textSize = 11.5f
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(billingTitle, (pageWidth / 2).toFloat(), currentY, textPaint)

        currentY += 15f
        return currentY
    }

    private fun drawTableHeader(
        canvas: Canvas,
        colX0: Float,
        colX1: Float,
        colX2: Float,
        colX3: Float,
        colX4: Float,
        colX5: Float,
        currentY: Float,
        rowHeight: Float,
        linePaint: Paint,
        textPaint: Paint
    ): Float {
        val headerYTop = currentY
        val headerYBottom = headerYTop + rowHeight

        // Top line
        canvas.drawLine(colX0, headerYTop, colX5, headerYTop, linePaint)
        // Bottom line of header
        canvas.drawLine(colX0, headerYBottom, colX5, headerYBottom, linePaint)

        // Vertical lines for header
        canvas.drawLine(colX0, headerYTop, colX0, headerYBottom, linePaint)
        canvas.drawLine(colX1, headerYTop, colX1, headerYBottom, linePaint)
        canvas.drawLine(colX2, headerYTop, colX2, headerYBottom, linePaint)
        canvas.drawLine(colX3, headerYTop, colX3, headerYBottom, linePaint)
        canvas.drawLine(colX4, headerYTop, colX4, headerYBottom, linePaint)
        canvas.drawLine(colX5, headerYTop, colX5, headerYBottom, linePaint)

        textPaint.textSize = 10f
        textPaint.isFakeBoldText = true
        textPaint.textAlign = Paint.Align.CENTER

        val headerTextY = headerYTop + (rowHeight / 2f) + 3.5f
        canvas.drawText("Date", (colX0 + colX1) / 2f, headerTextY, textPaint)
        canvas.drawText("Day", (colX1 + colX2) / 2f, headerTextY, textPaint)
        canvas.drawText("From", (colX2 + colX3) / 2f, headerTextY, textPaint)
        canvas.drawText("To", (colX3 + colX4) / 2f, headerTextY, textPaint)
        canvas.drawText("Hours", (colX4 + colX5) / 2f, headerTextY, textPaint)

        return headerYBottom
    }

    private fun drawDateGroupRows(
        canvas: Canvas,
        group: DayDutyGroup,
        colX0: Float,
        colX1: Float,
        colX2: Float,
        colX3: Float,
        colX4: Float,
        colX5: Float,
        startY: Float,
        rowHeight: Float,
        linePaint: Paint,
        textPaint: Paint
    ): Float {
        val shiftCount = group.shifts.size.coerceAtLeast(1)
        val groupTotalHeight = shiftCount * rowHeight
        val groupTopY = startY
        val groupBottomY = groupTopY + groupTotalHeight

        // Outer vertical borders for the entire date group
        canvas.drawLine(colX0, groupTopY, colX0, groupBottomY, linePaint)
        canvas.drawLine(colX1, groupTopY, colX1, groupBottomY, linePaint)
        canvas.drawLine(colX2, groupTopY, colX2, groupBottomY, linePaint)
        canvas.drawLine(colX4, groupTopY, colX4, groupBottomY, linePaint)
        canvas.drawLine(colX5, groupTopY, colX5, groupBottomY, linePaint)

        // Date (Vertically centered across all shifts of this date)
        val verticalCenterTextY = groupTopY + (groupTotalHeight / 2f) + 3.5f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.isFakeBoldText = false
        textPaint.textSize = 9.5f
        canvas.drawText(group.dateDisplay, (colX0 + colX1) / 2f, verticalCenterTextY, textPaint)

        // Day (Vertically centered)
        canvas.drawText(group.dayName, (colX1 + colX2) / 2f, verticalCenterTextY, textPaint)

        // Hours (Vertically centered for the day's combined duration)
        canvas.drawText(TimeUtil.formatDurationString(group.totalHours), (colX4 + colX5) / 2f, verticalCenterTextY, textPaint)

        // Draw individual shift rows inside the group
        for (i in group.shifts.indices) {
            val shift = group.shifts[i]
            val shiftRowTop = groupTopY + (i * rowHeight)
            val shiftRowBottom = shiftRowTop + rowHeight
            val shiftTextY = shiftRowTop + (rowHeight / 2f) + 3.5f

            // Vertical line between From and To
            canvas.drawLine(colX3, shiftRowTop, colX3, shiftRowBottom, linePaint)

            // From time
            canvas.drawText(shift.startTime, (colX2 + colX3) / 2f, shiftTextY, textPaint)
            // To time
            canvas.drawText(shift.endTime, (colX3 + colX4) / 2f, shiftTextY, textPaint)

            // Horizontal line between shifts (only within From and To columns)
            if (i < group.shifts.size - 1) {
                canvas.drawLine(colX2, shiftRowBottom, colX4, shiftRowBottom, linePaint)
            }
        }

        // Bottom border for the entire date group
        canvas.drawLine(colX0, groupBottomY, colX5, groupBottomY, linePaint)
        return groupBottomY
    }

    private fun drawTotalHoursRow(
        canvas: Canvas,
        colX0: Float,
        colX4: Float,
        colX5: Float,
        startY: Float,
        rowHeight: Float,
        grandTotalHours: Double,
        linePaint: Paint,
        textPaint: Paint
    ): Float {
        val totalRowTop = startY
        val totalRowBottom = totalRowTop + rowHeight

        // Vertical lines
        canvas.drawLine(colX0, totalRowTop, colX0, totalRowBottom, linePaint)
        canvas.drawLine(colX4, totalRowTop, colX4, totalRowBottom, linePaint)
        canvas.drawLine(colX5, totalRowTop, colX5, totalRowBottom, linePaint)

        // Bottom line
        canvas.drawLine(colX0, totalRowBottom, colX5, totalRowBottom, linePaint)

        textPaint.isFakeBoldText = true
        textPaint.textSize = 10f
        val totalTextY = totalRowTop + (rowHeight / 2f) + 3.5f

        // "Total Hours:" right-aligned before colX4
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Total Hours:", colX4 - 10f, totalTextY, textPaint)

        // Grand Total Hours centered in Hours col
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(TimeUtil.formatDurationString(grandTotalHours), (colX4 + colX5) / 2f, totalTextY, textPaint)

        return totalRowBottom
    }

    private fun drawSignatures(
        canvas: Canvas,
        profile: UserProfile,
        leftMargin: Float,
        rightMargin: Float,
        startY: Float,
        textPaint: Paint
    ) {
        val sigLineWidth = 150f
        val leftSigCenterX = leftMargin + (sigLineWidth / 2f) + 15f
        val rightSigCenterX = rightMargin - (sigLineWidth / 2f) - 15f

        val studentSigLineY = startY + 30f
        val authoritySigLineY = studentSigLineY

        // Draw Student Signature Image if available
        if (profile.signaturePath.isNotBlank()) {
            val sigFile = File(profile.signaturePath)
            if (sigFile.exists() && sigFile.length() > 0) {
                try {
                    val bitmap = BitmapFactory.decodeFile(sigFile.absolutePath)
                    if (bitmap != null) {
                        val maxSigW = 110f
                        val maxSigH = 34f
                        val aspect = bitmap.width.toFloat() / bitmap.height.toFloat()
                        val targetW = if (aspect >= (maxSigW / maxSigH)) maxSigW else maxSigH * aspect
                        val targetH = targetW / aspect

                        val left = leftSigCenterX - (targetW / 2f)
                        val top = studentSigLineY - targetH - 3f
                        val destRect = RectF(left, top, left + targetW, top + targetH)

                        canvas.drawBitmap(bitmap, null, destRect, null)
                    }
                } catch (e: Exception) {
                    // Ignore bitmap failure gracefully
                }
            }
        }

        // Student Signature Line: \rule{5cm}{0.4pt}
        val sigLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }

        val leftLineStart = leftSigCenterX - (sigLineWidth / 2f)
        val leftLineEnd = leftSigCenterX + (sigLineWidth / 2f)
        canvas.drawLine(leftLineStart, studentSigLineY, leftLineEnd, studentSigLineY, sigLinePaint)

        textPaint.isFakeBoldText = true
        textPaint.textSize = 9.5f
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("Signature of the Student", leftSigCenterX, studentSigLineY + 14f, textPaint)

        // Authority Signature Line
        val rightLineStart = rightSigCenterX - (sigLineWidth / 2f)
        val rightLineEnd = rightSigCenterX + (sigLineWidth / 2f)
        canvas.drawLine(rightLineStart, authoritySigLineY, rightLineEnd, authoritySigLineY, sigLinePaint)

        canvas.drawText("Signature of the person in charge", rightSigCenterX, authoritySigLineY + 14f, textPaint)
    }

    fun openPdf(context: Context, pdfFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooser = Intent.createChooser(intent, "Open ASC Bill PDF")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun sharePdf(context: Context, pdfFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Share ASC Bill PDF")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
