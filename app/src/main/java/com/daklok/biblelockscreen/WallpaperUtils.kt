package com.daklok.biblelockscreen

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint

object WallpaperUtils {

    fun createBitmapWithText(context: Context, imageUri: Uri, verse: String, ref: String): Bitmap? {
        try {
            val source = ImageDecoder.createSource(context.contentResolver, imageUri)
            val original = ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }

            val bitmap = original.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(bitmap)
            val width = canvas.width
            val height = canvas.height

            // 1. Filter (mierne silnejší pre istotu)
            canvas.drawColor(Color.argb(30, 0, 0, 0))

            // 2. Nastavenie pera - ODSTRÁNENÉ Align.CENTER (to robilo chaos pri translate)
            val textPaint = TextPaint().apply {
                color = Color.WHITE
                isAntiAlias = true
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                // Necháme predvolený Align.LEFT, centrovanie vyrieši StaticLayout
            }

            // Dynamická veľkosť
            var optimalSize = width * 0.06f
            if (verse.length > 150) optimalSize = width * 0.05f
            textPaint.textSize = optimalSize

            val textWidth = (width * 0.80).toInt() // 80% šírky pre text
            var staticLayout = createLayout(verse, textPaint, textWidth)

            // Zmenšenie ak je príliš vysoký
            while (staticLayout.height > height * 0.6 && textPaint.textSize > 30f) {
                textPaint.textSize -= 2f
                staticLayout = createLayout(verse, textPaint, textWidth)
            }

            // 3. VÝPOČET POZÍCIE - Matematicky presne na stred
            // xPos musí byť začiatok ľavého okraja textového bloku
            val xPos = (width - textWidth) / 2f
            val yPos = (height / 2f) - (staticLayout.height / 2f) -100

            // 4. Vykreslenie verša
            canvas.save()
            canvas.translate(xPos, yPos)
            staticLayout.draw(canvas)
            canvas.restore()

            // 5. Vykreslenie referencie (Súradnice)
            val cleanRef = ref.replace(" Katolícky preklad", "")
                .replace(" SSV", "")
                .trim()

            val refPaint = TextPaint().apply {
                color = Color.parseColor("#CCCCCC")
                textSize = textPaint.textSize * 0.7f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER // Referenciu kreslíme priamo na stred (xPos + textWidth/2)
            }

            // Referenciu vykreslíme presne pod stred textového bloku
            canvas.drawText(cleanRef, width / 2f, yPos + staticLayout.height + (height * 0.05f), refPaint)

            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun createLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER) // Toto vycentruje riadky vnútri bloku
            .setLineSpacing(0f, 1.2f)
            .build()
    }
}