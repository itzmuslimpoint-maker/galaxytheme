package com.example.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.example.data.WidgetConfig
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object WidgetDrawer {

    fun drawAestheticWidget(context: Context, config: WidgetConfig?, width: Int = 512, height: Int = 300): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // If config is null, use defaults
        val styleType = config?.styleType ?: "digital"
        val bgColorHex = config?.backgroundColor ?: "#1A1B2F"
        val textColorHex = config?.textColor ?: "#FFFFFF"
        val accentColorHex = config?.accentColor ?: "#FF4081"
        val opacity = config?.opacity ?: 0.9f
        val customText = config?.customText ?: "Design is intelligence made visible."
        val fontStyle = config?.fontStyle ?: "sans"
        val showBattery = config?.showBattery ?: true
        val showDate = config?.showDate ?: true

        // Draw background with rounded corners and opacity
        val bgPaint = Paint().apply {
            isAntiAlias = true
            color = parseColorSafely(bgColorHex)
            alpha = (opacity * 255).toInt().coerceIn(0, 255)
            style = Paint.Style.FILL
        }
        val rectF = RectF(0f, 0f, width.toFloat(), height.toFloat())
        val rx = 24.dpToPx(context)
        val ry = 24.dpToPx(context)
        canvas.drawRoundRect(rectF, rx, ry, bgPaint)

        // Draw a subtle primary border
        val borderPaint = Paint().apply {
            isAntiAlias = true
            color = parseColorSafely(accentColorHex)
            alpha = 40 // very subtle
            style = Paint.Style.STROKE
            strokeWidth = 2.dpToPx(context)
        }
        canvas.drawRoundRect(rectF, rx, ry, borderPaint)

        // Set up Font Typeface
        val typeface = when (fontStyle) {
            "serif" -> Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            "monospace" -> Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            else -> Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val boldTypeface = when (fontStyle) {
            "serif" -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
            "monospace" -> Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            else -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        // Common Paints
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = parseColorSafely(textColorHex)
            setTypeface(typeface)
        }

        val accentPaint = Paint().apply {
            isAntiAlias = true
            color = parseColorSafely(accentColorHex)
            setTypeface(boldTypeface)
        }

        val calendar = Calendar.getInstance()

        // 1. STYLE: DIGITAL CLOCK
        if (styleType == "digital") {
            // Draw current time
            val timeSdf = SimpleDateFormat("hh:mm", Locale.getDefault())
            val amPmSdf = SimpleDateFormat("a", Locale.getDefault())
            val timeStr = timeSdf.format(calendar.time)
            val amPmStr = amPmSdf.format(calendar.time)

            textPaint.apply {
                textSize = 64f.dpToPx(context)
                setTypeface(boldTypeface)
                textAlign = Paint.Align.CENTER
            }
            val timeY = height / 2f + 10f.dpToPx(context)
            canvas.drawText(timeStr, width / 2f - 15f.dpToPx(context), timeY, textPaint)

            accentPaint.apply {
                textSize = 18f.dpToPx(context)
                setTypeface(boldTypeface)
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText(amPmStr, width / 2f + 85f.dpToPx(context), timeY - 25f.dpToPx(context), accentPaint)

            // Draw date under time
            if (showDate) {
                val dateSdf = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault())
                val dateStr = dateSdf.format(calendar.time)
                textPaint.apply {
                    textSize = 14f.dpToPx(context)
                    setTypeface(typeface)
                    textAlign = Paint.Align.CENTER
                    alpha = 180
                }
                canvas.drawText(dateStr, width / 2f, timeY + 30f.dpToPx(context), textPaint)
            }

            // Draw battery or minor details in corners
            if (showBattery) {
                drawBatteryIndicator(canvas, context, width, height, textColorHex)
            }
            
            // Subtle custom subtitle
            if (customText.isNotEmpty()) {
                textPaint.apply {
                    textSize = 11f.dpToPx(context)
                    setTypeface(typeface)
                    textAlign = Paint.Align.CENTER
                    alpha = 110
                }
                val displayText = if (customText.length > 35) customText.substring(0, 32) + "..." else customText
                canvas.drawText(displayText, width / 2f, height - 16f.dpToPx(context), textPaint)
            }
        }
        // 2. STYLE: ANALOG CLOCK
        else if (styleType == "analog") {
            val centerX = width / 3f
            val centerY = height / 2f
            val radius = (height.coerceAtMost(width) / 2.3f)

            // Draw clock face background outer rim
            accentPaint.style = Paint.Style.STROKE
            accentPaint.strokeWidth = 3f.dpToPx(context)
            canvas.drawCircle(centerX, centerY, radius, accentPaint)

            // Draw hour ticks
            val tickPaint = Paint().apply {
                isAntiAlias = true
                color = parseColorSafely(textColorHex)
                strokeWidth = 2f.dpToPx(context)
                style = Paint.Style.STROKE
            }
            for (i in 0 until 12) {
                val angle = i * 30.0
                val rad = Math.toRadians(angle)
                val startX = (centerX + (radius - 8f.dpToPx(context)) * Math.sin(rad)).toFloat()
                val startY = (centerY - (radius - 8f.dpToPx(context)) * Math.cos(rad)).toFloat()
                val endX = (centerX + radius * Math.sin(rad)).toFloat()
                val endY = (centerY - radius * Math.cos(rad)).toFloat()
                canvas.drawLine(startX, startY, endX, endY, tickPaint)
            }

            // Calculate clock hand angles
            val hour = calendar.get(Calendar.HOUR)
            val minute = calendar.get(Calendar.MINUTE)
            val second = calendar.get(Calendar.SECOND)

            val hourAngle = (hour + minute / 60.0) * 30.0
            val minAngle = (minute + second / 60.0) * 6.0

            // Draw hour hand (thicker, shorter)
            val hourPaint = Paint().apply {
                isAntiAlias = true
                color = parseColorSafely(textColorHex)
                strokeWidth = 4f.dpToPx(context)
                strokeCap = Paint.Cap.ROUND
            }
            val hourRad = Math.toRadians(hourAngle)
            val hourLen = radius * 0.5f
            canvas.drawLine(
                centerX, centerY,
                (centerX + hourLen * Math.sin(hourRad)).toFloat(),
                (centerY - hourLen * Math.cos(hourRad)).toFloat(),
                hourPaint
            )

            // Draw minute hand (thinner, longer)
            val minPaint = Paint().apply {
                isAntiAlias = true
                color = parseColorSafely(accentColorHex)
                strokeWidth = 2.5f.dpToPx(context)
                strokeCap = Paint.Cap.ROUND
            }
            val minRad = Math.toRadians(minAngle)
            val minLen = radius * 0.8f
            canvas.drawLine(
                centerX, centerY,
                (centerX + minLen * Math.sin(minRad)).toFloat(),
                (centerY - minLen * Math.cos(minRad)).toFloat(),
                minPaint
            )

            // Pivot center circle
            val pivotPaint = Paint().apply {
                isAntiAlias = true
                color = parseColorSafely(accentColorHex)
                style = Paint.Style.FILL
            }
            canvas.drawCircle(centerX, centerY, 5f.dpToPx(context), pivotPaint)

            // Right column info
            val rightColX = width * 0.70f
            
            // Digital short time
            val shortTimeSdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            textPaint.apply {
                textSize = 20f.dpToPx(context)
                setTypeface(boldTypeface)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(shortTimeSdf.format(calendar.time), rightColX, centerY - 24f.dpToPx(context), textPaint)

            // Date
            if (showDate) {
                val dateSdf = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
                textPaint.apply {
                    textSize = 13f.dpToPx(context)
                    setTypeface(typeface)
                    textAlign = Paint.Align.CENTER
                    alpha = 180
                }
                canvas.drawText(dateSdf.format(calendar.time), rightColX, centerY + 2f.dpToPx(context), textPaint)
            }

            // Custom Text / Quote (small)
            textPaint.apply {
                textSize = 10f.dpToPx(context)
                setTypeface(typeface)
                textAlign = Paint.Align.CENTER
                alpha = 130
            }
            val quoteStr = if (customText.length > 20) customText.substring(0, 17) + "..." else customText
            canvas.drawText("\"$quoteStr\"", rightColX, centerY + 26f.dpToPx(context), textPaint)

            if (showBattery) {
                drawBatteryIndicator(canvas, context, width, height, textColorHex)
            }
        }
        // 3. STYLE: QUOTE CARD / MOTIVATIONAL
        else if (styleType == "quote") {
            // Quote mark symbol top-left
            accentPaint.apply {
                textSize = 60f.dpToPx(context)
                setTypeface(boldTypeface)
                alpha = 60
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText("“", 24f.dpToPx(context), 70f.dpToPx(context), accentPaint)

            // Quote text
            textPaint.apply {
                textSize = 16f.dpToPx(context)
                setTypeface(typeface)
                textAlign = Paint.Align.LEFT
                alpha = 255
            }
            
            val lines = wrapText(customText, width - 80.dpToPx(context).toInt(), textPaint)
            var startY = 90f.dpToPx(context)
            for (line in lines.take(3)) {
                canvas.drawText(line, 40f.dpToPx(context), startY, textPaint)
                startY += 24f.dpToPx(context)
            }

            // Draw author / spacer
            accentPaint.apply {
                textSize = 11f.dpToPx(context)
                setTypeface(boldTypeface)
                textAlign = Paint.Align.RIGHT
                alpha = 180
            }
            canvas.drawText("— Aesthetic Vibe", width - 40f.dpToPx(context), startY + 8f.dpToPx(context), accentPaint)

            // Date & Battery tiny footprint along bottom
            if (showDate) {
                val dateSdf = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault())
                textPaint.apply {
                    textSize = 11f.dpToPx(context)
                    setTypeface(typeface)
                    textAlign = Paint.Align.LEFT
                    alpha = 120
                }
                canvas.drawText(dateSdf.format(calendar.time), 40f.dpToPx(context), height - 20f.dpToPx(context), textPaint)
            }

            if (showBattery) {
                drawBatteryIndicator(canvas, context, width, height, textColorHex)
            }
        }
        // 4. STYLE: CALENDAR CARD
        else if (styleType == "calendar") {
            val leftColX = width * 0.28f
            val dividerX = width * 0.44f
            val rightColX = width * 0.48f

            // Left side: Large Date Representation
            val daySdf = SimpleDateFormat("dd", Locale.getDefault())
            val monthSdf = SimpleDateFormat("MMM", Locale.getDefault())
            val dayOfWeekSdf = SimpleDateFormat("EEEE", Locale.getDefault())

            accentPaint.apply {
                textSize = 52f.dpToPx(context)
                setTypeface(boldTypeface)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(daySdf.format(calendar.time), leftColX, height / 2f + 4f.dpToPx(context), accentPaint)

            textPaint.apply {
                textSize = 16f.dpToPx(context)
                setTypeface(boldTypeface)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(monthSdf.format(calendar.time).uppercase(), leftColX, height / 2f - 40f.dpToPx(context), textPaint)

            textPaint.apply {
                textSize = 12f.dpToPx(context)
                setTypeface(typeface)
                textAlign = Paint.Align.CENTER
                alpha = 160
            }
            canvas.drawText(dayOfWeekSdf.format(calendar.time), leftColX, height / 2f + 24f.dpToPx(context), textPaint)

            // Draw divider line
            val divPaint = Paint().apply {
                isAntiAlias = true
                color = parseColorSafely(textColorHex)
                strokeWidth = 1f.dpToPx(context)
                alpha = 50
            }
            canvas.drawLine(dividerX, 40f.dpToPx(context), dividerX, height - 40f.dpToPx(context), divPaint)

            // Right side: Mini Calendar Representation or Month Grid
            textPaint.apply {
                textSize = 12f.dpToPx(context)
                setTypeface(boldTypeface)
                textAlign = Paint.Align.LEFT
                alpha = 200
            }
            canvas.drawText("MAY 2026", rightColX, 55f.dpToPx(context), textPaint)

            // Dynamic short custom quote
            if (customText.isNotEmpty()) {
                textPaint.apply {
                    textSize = 11f.dpToPx(context)
                    setTypeface(typeface)
                    textAlign = Paint.Align.LEFT
                    alpha = 140
                }
                val calendarLine = if (customText.length > 25) customText.substring(0, 22) + "..." else customText
                canvas.drawText("“$calendarLine”", rightColX, height / 2f + 40f.dpToPx(context), textPaint)
            }

            // Draw mock mini monthly grid lines/dots (looks incredibly cute)
            val dotPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
            }
            val startGridX = rightColX
            val startGridY = 75f.dpToPx(context)
            val cellW = 20f.dpToPx(context)
            val cellH = 15f.dpToPx(context)

            // 5 weeks, 7 days
            for (row in 0 until 4) {
                for (col in 0 until 7) {
                    val isTodayIndex = (row == 2 && col == 3) // simulate active day
                    dotPaint.color = if (isTodayIndex) parseColorSafely(accentColorHex) else parseColorSafely(textColorHex)
                    dotPaint.alpha = if (isTodayIndex) 255 else 60

                    if (isTodayIndex) {
                        canvas.drawCircle(startGridX + col * cellW + 3f.dpToPx(context), startGridY + row * cellH, 4f.dpToPx(context), dotPaint)
                    } else {
                        canvas.drawCircle(startGridX + col * cellW + 3f.dpToPx(context), startGridY + row * cellH, 2.5f.dpToPx(context), dotPaint)
                    }
                }
            }

            if (showBattery) {
                drawBatteryIndicator(canvas, context, width, height, textColorHex)
            }
        }

        return bitmap
    }

    private fun drawBatteryIndicator(canvas: Canvas, context: Context, width: Int, height: Int, textColorHex: String) {
        val paint = Paint().apply {
            isAntiAlias = true
            color = parseColorSafely(textColorHex)
            textSize = 10f.dpToPx(context)
            alpha = 120
            textAlign = Paint.Align.RIGHT
        }
        val x = width - 16f.dpToPx(context)
        val y = 24f.dpToPx(context)

        // Simulate or read battery (88% is aesthetic default)
        canvas.drawText("⚡ 88%", x, y, paint)
    }

    private fun parseColorSafely(hex: String): Int {
        return try {
            Color.parseColor(hex)
        } catch (e: Exception) {
            Color.WHITE
        }
    }

    private fun Float.dpToPx(context: Context): Float {
        return this * context.resources.displayMetrics.density
    }

    private fun Int.dpToPx(context: Context): Float {
        return this.toFloat().dpToPx(context)
    }

    private fun wrapText(text: String, width: Int, paint: Paint): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val textWidth = paint.measureText(testLine)
            if (textWidth <= width) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                }
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        return lines
    }
}
