package com.daklok.biblelockscreen

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.annotation.RequiresApi
import kotlin.math.max

object WallpaperUtils {

    @RequiresApi(Build.VERSION_CODES.P)
    fun createBitmapWithText(
        context: Context,
        imageUri: Uri,
        verse: String,
        ref: String,
        textSizeMultiplier: Float = 1.0f,
        textWidthMultiplier: Float = 1.0f,
        verticalOffset: Float = 0.0f,
        textColorInt: Int = Color.WHITE,
        textAlpha: Float = 1.0f,
        isBold: Boolean = true,
        useShadow: Boolean = true,
        fontFamilyStr: String = "sans-serif",
        bgBlurRadius: Float = 0f,
        bgDarkness: Float = 0.23f
    ): Bitmap? {
        try {
            // 1. Získanie rozmerov obrazovky
            val metrics = context.resources.displayMetrics
            val screenW = metrics.widthPixels
            val screenH = metrics.heightPixels

            val source = ImageDecoder.createSource(context.contentResolver, imageUri)
            val original = ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }

            // 2. Vytvoríme si dočasnú bitmapu pre pozadie
            var workingBitmap = Bitmap.createBitmap(screenW, screenH, Bitmap.Config.ARGB_8888)
            val bgCanvas = Canvas(workingBitmap)

            // 3. Center-Crop obrázku
            val scale = max(screenW.toFloat() / original.width, screenH.toFloat() / original.height)
            val scaledW = original.width * scale
            val scaledH = original.height * scale
            val dx = (screenW - scaledW) / 2f
            val dy = (screenH - scaledH) / 2f

            val matrix = Matrix().apply {
                postScale(scale, scale)
                postTranslate(dx, dy)
            }
            bgCanvas.drawBitmap(original, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
            original.recycle()

            // 4. Aplikácia Blur efektu na pozadie - teraz s ohľadom na hustotu pixelov (DP -> PX)
            if (bgBlurRadius > 0f) {
                val density = metrics.density
                val blurred = blurBitmap(context, workingBitmap, bgBlurRadius * density)
                workingBitmap.recycle()
                workingBitmap = blurred
            }

            // Vytvoríme už finálny canvas na upravenom (alebo neupravenom) pozadí
            val canvas = Canvas(workingBitmap)

            // 5. Vignette
            val paintScrim = Paint().apply {
                color = Color.BLACK
                alpha = (bgDarkness * 255).toInt().coerceIn(0, 255)
            }
            canvas.drawRect(0f, 0f, screenW.toFloat(), screenH.toFloat(), paintScrim)

            // 6. Text Settings
            val finalTextColor = applyAlpha(textColorInt, textAlpha)
            val textPaint = TextPaint().apply {
                color = finalTextColor
                isAntiAlias = true
                typeface = Typeface.create(fontFamilyStr, if (isBold) Typeface.BOLD else Typeface.NORMAL)
                if (useShadow) {
                    setShadowLayer(12f, 0f, 0f, Color.BLACK)
                }
            }

            // KONZISTENTNÁ LOGIKA VEĽKOSTI A ŠÍRKY
            val boxWidth = screenW * 0.80f * textWidthMultiplier
            val paddingPx = screenW * 0.025f
            val textLayoutWidth = max(1f, boxWidth - (2 * paddingPx)).toInt()
            
            var baseSize = screenW * 0.055f
            if (verse.length > 150) baseSize = screenW * 0.045f
            textPaint.textSize = baseSize * textSizeMultiplier

            var staticLayout = createLayout(verse, textPaint, textLayoutWidth)

            // Zmenšovanie ak sa nezmestí (safety check)
            while (staticLayout.height > screenH * 0.6 && textPaint.textSize > 20f) {
                textPaint.textSize -= 2f
                staticLayout = createLayout(verse, textPaint, textLayoutWidth)
            }

            // Ref Settings
            val cleanRef = ref.replace(" Katolícky preklad", "")
                .replace(" SSV", "")
                .trim()

            val refPaint = TextPaint().apply {
                color = applyAlpha(if (textColorInt == Color.BLACK) Color.DKGRAY else Color.LTGRAY, textAlpha)
                textSize = textPaint.textSize * 0.75f
                isAntiAlias = true
                typeface = Typeface.create(fontFamilyStr, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
                if (useShadow) {
                    setShadowLayer(8f, 0f, 0f, Color.BLACK)
                }
            }

            // Výpočet výšky celého bloku
            val gap = textPaint.textSize * 0.5f
            val fontMetrics = refPaint.fontMetrics
            val refHeight = fontMetrics.descent - fontMetrics.ascent
            val totalBlockHeight = staticLayout.height + gap + refHeight

            // 7. Pozícia
            val centerX = screenW / 2f
            val centerY = (screenH / 2f) - (totalBlockHeight / 2f)
            
            val defaultOffset = screenH * 0.05f
            val userOffsetPixels = (screenH * 0.35f) * verticalOffset
            val blockTopY = centerY + userOffsetPixels + defaultOffset

            // 8. Kreslenie verša (vycentrovaný StaticLayout)
            canvas.save()
            val xPos = (screenW - textLayoutWidth) / 2f
            canvas.translate(xPos, blockTopY)
            staticLayout.draw(canvas)
            canvas.restore()

            // 9. Kreslenie referencie
            val refBaselineY = blockTopY + staticLayout.height + gap - fontMetrics.ascent
            canvas.drawText(cleanRef, centerX, refBaselineY, refPaint)

            return workingBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    @Suppress("DEPRECATION")
    private fun blurBitmap(context: Context, bitmap: Bitmap, radius: Float): Bitmap {
        if (radius <= 0.1f) return bitmap

        // 1. Calculate downscale factor.
        // We use a fixed factor if radius is high to keep performance consistent
        // across preview and final render.
        val downscale = if (radius > 25f) radius / 25f else 1f
        val internalRadius = (radius / downscale).coerceIn(1f, 25f)

        val width = max(1, (bitmap.width / downscale).toInt())
        val height = max(1, (bitmap.height / downscale).toInt())

        // Create the smaller bitmap for blurring
        val workingBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
        val outputBitmap = Bitmap.createBitmap(workingBitmap.width, workingBitmap.height, Bitmap.Config.ARGB_8888)

        var rs: android.renderscript.RenderScript? = null
        try {
            rs = android.renderscript.RenderScript.create(context)
            val input = android.renderscript.Allocation.createFromBitmap(rs, workingBitmap)
            val output = android.renderscript.Allocation.createTyped(rs, input.type)
            val script = android.renderscript.ScriptIntrinsicBlur.create(rs, android.renderscript.Element.U8_4(rs))

            script.setRadius(internalRadius)
            script.setInput(input)
            script.forEach(output)
            output.copyTo(outputBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: If RS fails, the scaled-down workingBitmap already provides a "cheap" blur
            return Bitmap.createScaledBitmap(workingBitmap, bitmap.width, bitmap.height, true)
        } finally {
            if (workingBitmap != bitmap) workingBitmap.recycle()
            rs?.destroy()
        }

        // Scale back to original size with bilinear filtering (true)
        val finalBitmap = Bitmap.createScaledBitmap(outputBitmap, bitmap.width, bitmap.height, true)
        outputBitmap.recycle()
        return finalBitmap
    }

    private fun createLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1.25f)
            .build()
    }

    private fun applyAlpha(color: Int, alphaFactor: Float): Int {
        val originalAlpha = Color.alpha(color)
        val newAlpha = (originalAlpha * alphaFactor).toInt().coerceIn(0, 255)
        return Color.argb(newAlpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}
