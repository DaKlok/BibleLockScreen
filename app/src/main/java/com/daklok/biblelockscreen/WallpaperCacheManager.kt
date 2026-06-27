package com.daklok.biblelockscreen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File

/**
 * Pre-renders the next wallpaper bitmap (image + verse text + styling) and
 * caches it as a PNG file. When the screen turns off, [ScreenOffReceiver]
 * just loads the cached PNG and calls setBitmap — no rendering needed.
 *
 * This makes the wallpaper swap nearly instant (~200ms instead of ~2-3s).
 */
object WallpaperCacheManager {

    private const val CACHE_FILE = "next_wallpaper_cache.png"

    fun cacheFile(context: Context): File =
        File(context.filesDir, CACHE_FILE)

    fun hasCache(context: Context): Boolean =
        cacheFile(context).exists()

    /**
     * Pre-renders the next wallpaper+verse bitmap and saves it as a PNG.
     * Call this after every successful wallpaper render so the next swap
     * is instant.
     *
     * Steps:
     * 1. Peek at the next wallpaper (without setting it active)
     * 2. Load the current verse (same verse — screen-off doesn't change it
     *    unless verse cycling is also on)
     * 3. Render the bitmap
     * 4. Save as PNG to cache file
     */
    @RequiresApi(Build.VERSION_CODES.P)
    fun prerenderNext(context: Context, prefs: android.content.SharedPreferences) {
        try {
            val wallpapers = WallpaperManager.listWallpapers(context)
            if (wallpapers.size < 2) return

            // Find the next wallpaper (peek, don't set active)
            val currentId = prefs.getString("active_wallpaper_id", null)
            val currentIndex = wallpapers.indexOfFirst { it.id == currentId }
            val nextIndex = (currentIndex + 1).coerceAtLeast(0) % wallpapers.size
            val nextWallpaper = wallpapers[nextIndex]

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

            // Render the bitmap using the NEXT wallpaper's image
            val bitmap = WallpaperUtils.createBitmapWithText(
                context = context,
                imageUri = Uri.fromFile(nextWallpaper.file),
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
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Loads the cached bitmap. Returns null if cache doesn't exist or
     * can't be decoded.
     */
    fun loadCache(context: Context): Bitmap? {
        val file = cacheFile(context)
        if (!file.exists()) return null
        return try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Clears the cache.
     */
    fun clearCache(context: Context) {
        val file = cacheFile(context)
        if (file.exists()) file.delete()
    }
}
