package com.daklok.biblelockscreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi

class ScreenOffReceiver : BroadcastReceiver() {

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_SCREEN_OFF) return
        AppLogger.i(context, "Receiver", "onReceive(ACTION_SCREEN_OFF)")

        val prefs = context.getSharedPreferences("bible_app_prefs", Context.MODE_PRIVATE)
        val wpSettings = WallpaperSettings.load(prefs)

        val shouldCycleWallpaper = wpSettings.cycleEnabled &&
                wpSettings.cycleMode == com.daklok.biblelockscreen.WallpaperManager.CYCLE_ON_SCREEN_OFF

        val changeOnScreenOff = prefs.getBoolean("change_on_screen_off", false)
        val autoActive = prefs.getBoolean("auto_wallpaper_active", false)
        val shouldCycleVerse = changeOnScreenOff && autoActive

        if (!shouldCycleWallpaper && !shouldCycleVerse) {
            AppLogger.d(context, "Receiver", "Ignored: No cycling enabled")
            return
        }

        if (shouldCycleWallpaper) {
            val changed = com.daklok.biblelockscreen.WallpaperManager.cycleToNext(context, prefs)
            if (changed) {
                AppLogger.i(context, "Wallpaper", "Cycled to next wallpaper in gallery")
            } else {
                AppLogger.d(context, "Wallpaper", "Cycle skipped: fewer than 2 wallpapers in gallery")
            }
        }

        if (shouldCycleVerse) {
            AppLogger.d(context, "Receiver", "Cycling verse index")
            val currentIndex = prefs.getInt("screen_off_verse_index", 0)
            prefs.edit().putInt("screen_off_verse_index", currentIndex + 1).apply()
        }

        val pendingResult = goAsync()
        Thread {
            try {
                val cachedBitmap = if (WallpaperCacheManager.isCacheValid(context, prefs)) {
                    WallpaperCacheManager.loadCache(context)
                } else {
                    null
                }
                if (cachedBitmap != null) {
                    AppLogger.d(context, "Wallpaper", "Applying cached bitmap")
                    applyBitmap(context, prefs, cachedBitmap)
                    cachedBitmap.recycle()
                } else {
                    AppLogger.d(context, "Wallpaper", "Cache miss, rendering full")
                    val bitmap = renderFull(context, prefs)
                    if (bitmap != null) {
                        applyBitmap(context, prefs, bitmap)
                        bitmap.recycle()
                    } else {
                        AppLogger.e(context, "Wallpaper", "Error: renderFull returned null")
                    }
                }
                // Pre-render next wallpaper for next screen-off
                WallpaperCacheManager.prerenderNext(context, prefs)
                AppLogger.i(context, "Receiver", "ScreenOff processing finished")
            } catch (e: Exception) {
                AppLogger.e(context, "Receiver", "Error: ${e.message}")
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }.start()
    }

    @Suppress("DEPRECATION")
    private fun applyBitmap(
        context: Context,
        prefs: android.content.SharedPreferences,
        bitmap: android.graphics.Bitmap
    ) {
        val wallpaperManager = android.app.WallpaperManager.getInstance(context)
        val target = prefs.getInt("wallpaper_target", 0)
        val flag = when (target) {
            1    -> android.app.WallpaperManager.FLAG_SYSTEM
            2    -> android.app.WallpaperManager.FLAG_LOCK or android.app.WallpaperManager.FLAG_SYSTEM
            else -> android.app.WallpaperManager.FLAG_LOCK
        }
        val targetLabel = when (target) {
            1 -> "Home screen"
            2 -> "Lock + Home screen"
            else -> "Lock screen"
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                wallpaperManager.setBitmap(bitmap, null, true, flag)
            } else {
                wallpaperManager.setBitmap(bitmap)
            }
            AppLogger.i(context, "Wallpaper", "✓ Wallpaper set successfully ($targetLabel)")
        } catch (e: Exception) {
            AppLogger.e(context, "Wallpaper", "✗ Failed to set wallpaper: ${e.message}")
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun renderFull(
        context: Context,
        prefs: android.content.SharedPreferences
    ): android.graphics.Bitmap? {
        val localFile = java.io.File(context.filesDir, "user_wallpaper.jpg")
        val uriString = if (localFile.exists()) {
            android.net.Uri.fromFile(localFile).toString()
        } else {
            prefs.getString("bg_uri", null)
        } ?: return null

        val bgBlurRadius = prefs.getFloat("bg_blur", 0f)
        val textSizeMult = prefs.getFloat("text_size_mult", 1.0f)
        val textWidthMult = prefs.getFloat("text_width_mult", 1.0f)
        val verticalOffset = prefs.getFloat("vertical_offset", 0.0f)
        val textColor = prefs.getInt("text_color", android.graphics.Color.WHITE)
        val textAlpha = prefs.getFloat("text_alpha", 1.0f)
        val isBold = prefs.getBoolean("is_bold", true)
        val useShadow = prefs.getBoolean("use_shadow", true)
        val fontFamilyStr = prefs.getString("font_family", "sans-serif") ?: "sans-serif"
        val bgDarkness = prefs.getFloat("bg_darkness", 0.23f)

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
                LocalBibleProvider.getVerseForScreenOff(context, verseLang, verseLangSource)
            } else {
                val intervalHours = prefs.getInt("auto_interval_hours", 24)
                LocalBibleProvider.getVerseForInterval(context, verseLang, intervalHours, verseLangSource)
            }
        }

        return WallpaperUtils.createBitmapWithText(
            context = context,
            imageUri = android.net.Uri.parse(uriString),
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
    }
}