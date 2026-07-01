package com.daklok.biblelockscreen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File


object WallpaperCacheManager {

    private const val CACHE_FILE = "next_wallpaper_cache.png"

    private const val CACHE_SOURCE_KEY = "cache_source_uri"

    fun cacheFile(context: Context): File =
        File(context.filesDir, CACHE_FILE)

    fun hasCache(context: Context): Boolean =
        cacheFile(context).exists()

    // ─────────────────────────────────────────────────────────────────────
    // Image source resolution
    // ─────────────────────────────────────────────────────────────────────

    private fun nextImageSourceUri(
        context: Context,
        prefs: android.content.SharedPreferences
    ): String? {
        val wpSettings = WallpaperSettings.load(prefs)
        val cycleWallpaperOnScreenOff = wpSettings.cycleEnabled &&
                wpSettings.cycleMode == WallpaperManager.CYCLE_ON_SCREEN_OFF

        if (cycleWallpaperOnScreenOff) {
            val wallpapers = WallpaperManager.listWallpapers(context)
            if (wallpapers.size >= 2) {
                val currentId = prefs.getString("active_wallpaper_id", null)
                val currentIndex = wallpapers.indexOfFirst { it.id == currentId }
                val nextIndex = (currentIndex + 1).coerceAtLeast(0) % wallpapers.size
                return Uri.fromFile(wallpapers[nextIndex].file).toString()
            }
        }

        // Wallpaper cycling is OFF (or fewer than 2 wallpapers to cycle) —
        // use the active wallpaper image so we DON'T accidentally rotate
        // the background photo when only the verse is supposed to change.
        val active = WallpaperManager.activeWallpaperFile(context)
        if (active.exists()) return Uri.fromFile(active).toString()
        // Legacy fallback: bg_uri from prefs (used before any managed
        // wallpaper was set as active).
        return prefs.getString("bg_uri", null)
    }

    // ─────────────────────────────────────────────────────────────────────
    // Pre-render
    // ─────────────────────────────────────────────────────────────────────

    @RequiresApi(Build.VERSION_CODES.P)
    fun prerenderNext(context: Context, prefs: android.content.SharedPreferences) {
        try {
            val imageUriStr = nextImageSourceUri(context, prefs) ?: return

            // Load styling settings
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

            // Load verse — if verse cycling on screen-off is active, use the
            // NEXT verse index (current + 1) so the pre-render matches what
            // will be shown next time.
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
                // Use the verse that will be shown NEXT (current index + 1)
                // so the cache matches the next screen-off
                val changeOnScreenOff = prefs.getBoolean("change_on_screen_off", false)
                if (changeOnScreenOff) {
                    // Temporarily increment the index for the preview
                    val currentIndex = prefs.getInt("screen_off_verse_index", 0)
                    prefs.edit().putInt("screen_off_verse_index", currentIndex + 1).apply()
                    val verse = LocalBibleProvider.getVerseForScreenOff(context, verseLang, verseLangSource)
                    // Revert the index — we just wanted to peek
                    prefs.edit().putInt("screen_off_verse_index", currentIndex).apply()
                    verse
                } else {
                    val intervalHours = prefs.getInt("auto_interval_hours", 24)
                    LocalBibleProvider.getVerseForInterval(context, verseLang, intervalHours, verseLangSource)
                }
            }

            // Render the bitmap using the image determined above
            val bitmap = WallpaperUtils.createBitmapWithText(
                context = context,
                imageUri = Uri.parse(imageUriStr),
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

            // Save as PNG cache
            if (bitmap != null) {
                cacheFile(context).outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                bitmap.recycle()

                prefs.edit().putString(CACHE_SOURCE_KEY, imageUriStr).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Cache validation & loading
    // ─────────────────────────────────────────────────────────────────────

    fun isCacheValid(context: Context, prefs: android.content.SharedPreferences): Boolean {
        val cache = cacheFile(context)
        if (!cache.exists()) return false

        val expectedUri = nextImageSourceUri(context, prefs) ?: return false
        val storedUri = prefs.getString(CACHE_SOURCE_KEY, null) ?: return false
        if (storedUri != expectedUri) return false

        val activeFile = WallpaperManager.activeWallpaperFile(context)
        val expectedActiveUri = Uri.fromFile(activeFile).toString()
        if (expectedUri == expectedActiveUri && activeFile.exists()) {
            if (activeFile.lastModified() > cache.lastModified()) {
                return false
            }
        }

        return true
    }


    fun loadCache(context: Context): Bitmap? {
        val file = cacheFile(context)
        if (!file.exists()) return null
        return try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            null
        }
    }


    fun clearCache(context: Context) {
        val file = cacheFile(context)
        if (file.exists()) file.delete()
    }
}
