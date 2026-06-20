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
        val bgDarkness = prefs.getFloat("bg_darkness", 0.23f)

        // Skontrolujeme, či užívateľ nemá nastavený vlastný text
        val useCustomVerse = prefs.getBoolean("use_custom_verse", false)
        val customVerseText = prefs.getString("custom_verse_text", null)
        val customVerseRef = prefs.getString("custom_verse_ref", null)

        val verseData = if (useCustomVerse && !customVerseText.isNullOrEmpty()) {
            Pair(customVerseText, customVerseRef ?: "")
        } else {
            val defaultLang = LocalBibleProvider.getDefaultLanguage()
            val verseLang = prefs.getString("verse_lang", defaultLang) ?: defaultLang
            val verseLangSource = prefs.getString("verse_lang_source", LocalBibleProvider.SOURCE_BUILTIN)
                ?: LocalBibleProvider.SOURCE_BUILTIN
            val changeOnScreenOff = prefs.getBoolean("change_on_screen_off", false)
            if (changeOnScreenOff) {
                LocalBibleProvider.getVerseForScreenOff(applicationContext, verseLang, verseLangSource)
            } else {
                val intervalHours = prefs.getInt("auto_interval_hours", 24)
                LocalBibleProvider.getVerseForInterval(applicationContext, verseLang, intervalHours, verseLangSource)
            }
        }

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
            bgBlurRadius = bgBlurRadius,
            bgDarkness = bgDarkness
        )

        if (finalBitmap != null) {
            val wallpaperManager = WallpaperManager.getInstance(applicationContext)
            // 0 = lock only, 1 = home only, 2 = both
            val target = prefs.getInt("wallpaper_target", 0)
            val flag = when (target) {
                1    -> WallpaperManager.FLAG_SYSTEM
                2    -> WallpaperManager.FLAG_LOCK or WallpaperManager.FLAG_SYSTEM
                else -> WallpaperManager.FLAG_LOCK
            }
            try {
                wallpaperManager.setBitmap(
                    finalBitmap,
                    null,
                    true,
                    flag
                )
                return Result.success()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return Result.failure()
    }
}
