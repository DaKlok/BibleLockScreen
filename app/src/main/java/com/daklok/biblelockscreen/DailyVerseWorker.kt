package com.daklok.biblelockscreen

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class DailyVerseWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    @RequiresApi(Build.VERSION_CODES.P)
    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("bible_app_prefs", Context.MODE_PRIVATE)

        val localFile = java.io.File(applicationContext.filesDir, "user_wallpaper.jpg")
        val uriString = if (localFile.exists()) {
            Uri.fromFile(localFile).toString()
        } else {
            prefs.getString("bg_uri", null)
        }

        if (uriString == null) return Result.failure()

        // Načítanie všetkých nastavení
        val bgBlurRadius = prefs.getFloat("bg_blur", 0f)
        val textSizeMult = prefs.getFloat("text_size_mult", 1.0f)
        val textWidthMult = prefs.getFloat("text_width_mult", 1.0f)
        val verticalOffset = prefs.getFloat("vertical_offset", 0.0f)
        val textColor = prefs.getInt("text_color", Color.WHITE)
        val textAlpha = prefs.getFloat("text_alpha", 1.0f)
        val isBold = prefs.getBoolean("is_bold", true)
        val useShadow = prefs.getBoolean("use_shadow", true)
        val fontFamilyStr = prefs.getString("font_family", "sans-serif") ?: "sans-serif"
        
        // Use YouVersionFetcher.getDefaultLanguage() if no language is saved in prefs
        //val defaultLang = YouVersionFetcher.getDefaultLanguage()
        val defaultLang = LocalBibleProvider.getDefaultLanguage()
        val verseLang = prefs.getString("verse_lang", defaultLang) ?: defaultLang

        //val verseData = YouVersionFetcher.getVerseOfTheDay(verseLang) ?: return Result.retry()
        val verseData = LocalBibleProvider.getVerse(applicationContext, verseLang)

        val finalBitmap = WallpaperUtils.createBitmapWithText(
            context = applicationContext,
            imageUri = Uri.parse(uriString),
            verse = verseData.first,
            ref = verseData.second,
            textSizeMultiplier = textSizeMult,
            textWidthMultiplier = textWidthMult,
            verticalOffset = verticalOffset,
            textColorInt = textColor,
            textAlpha = textAlpha,
            isBold = isBold,
            useShadow = useShadow,
            fontFamilyStr = fontFamilyStr,
            bgBlurRadius = bgBlurRadius
        )

        if (finalBitmap != null) {
            val wallpaperManager = WallpaperManager.getInstance(applicationContext)
            try {
                wallpaperManager.setBitmap(
                    finalBitmap,
                    null,
                    true,
                    WallpaperManager.FLAG_LOCK
                )
                return Result.success()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return Result.failure()
    }
}