package com.daklok.biblelockscreen

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint

object WallpaperUtils {

    fun createBitmapWithText(
        context: Context,
        imageUri: Uri,
        verse: String,
        ref: String,
        textSizeMultiplier: Float = 1.0f,
        verticalOffset: Float = 0.0f,
        textColorInt: Int = Color.WHITE,
        textAlpha: Float = 1.0f, // 0.0 - 1.0
        isBold: Boolean = true,
        useShadow: Boolean = true
    ): Bitmap? {
        try {
            val source = ImageDecoder.createSource(context.contentResolver, imageUri)
            val original = ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }

            // Kópia bitmapy
            val bitmap = original.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(bitmap)
            val width = canvas.width
            val height = canvas.height

            // 1. Vignette (stmavovací filter) - prispôsobíme podľa priehľadnosti textu
            // Ak je text priehľadnejší, chceme tmavšie pozadie pre kontrast
            val paintScrim = Paint().apply {
                color = Color.BLACK
                alpha = (60 + (1.0f - textAlpha) * 40).toInt().coerceIn(0, 150)
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintScrim)

            // 2. Nastavenie farby a priehľadnosti
            val finalTextColor = applyAlpha(textColorInt, textAlpha)

            val textPaint = TextPaint().apply {
                color = finalTextColor
                isAntiAlias = true
                typeface = Typeface.create(Typeface.SANS_SERIF, if (isBold) Typeface.BOLD else Typeface.NORMAL)
                if (useShadow) {
                    setShadowLayer(12f, 0f, 0f, Color.BLACK)
                }
            }

            // Výpočet veľkosti
            var baseSize = width * 0.055f
            if (verse.length > 150) baseSize = width * 0.045f
            textPaint.textSize = baseSize * textSizeMultiplier

            val textWidth = (width * 0.85).toInt()
            var staticLayout = createLayout(verse, textPaint, textWidth)

            // Zmenšovanie ak sa nezmestí
            while (staticLayout.height > height * 0.6 && textPaint.textSize > 20f) {
                textPaint.textSize -= 2f
                staticLayout = createLayout(verse, textPaint, textWidth)
            }

            // 3. Pozícia
            val xPos = (width - textWidth) / 2f
            val centerY = (height / 2f) - (staticLayout.height / 2f)
            // Pixel 6 má hodiny vysoko, posuňme default trochu nižšie
            val defaultOffset = height * 0.05f
            val userOffsetPixels = (height * 0.35f) * verticalOffset
            val yPos = centerY + userOffsetPixels + defaultOffset

            // 4. Kreslenie verša
            canvas.save()
            canvas.translate(xPos, yPos)
            staticLayout.draw(canvas)
            canvas.restore()

            // 5. Kreslenie referencie
            val cleanRef = ref.replace(" Katolícky preklad", "")
                .replace(" SSV", "")
                .trim()

            val refPaint = TextPaint().apply {
                color = applyAlpha(if (textColorInt == Color.BLACK) Color.DKGRAY else Color.LTGRAY, textAlpha) // Jemne iná farba
                textSize = textPaint.textSize * 0.75f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
                if (useShadow) {
                    setShadowLayer(8f, 0f, 0f, Color.BLACK)
                }
            }

            canvas.drawText(
                cleanRef,
                width / 2f,
                yPos + staticLayout.height + (textPaint.textSize * 1.5f),
                refPaint
            )

            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun createLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1.25f) // Trochu vzdušnejšie riadkovanie
            .build()
    }

    private fun applyAlpha(color: Int, alphaFactor: Float): Int {
        val originalAlpha = Color.alpha(color)
        val newAlpha = (originalAlpha * alphaFactor).toInt().coerceIn(0, 255)
        return Color.argb(newAlpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}