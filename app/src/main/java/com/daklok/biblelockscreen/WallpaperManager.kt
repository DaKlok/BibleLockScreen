package com.daklok.biblelockscreen

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Manages multiple user wallpapers stored in [filesDir]/wallpapers/.
 *
 * Each wallpaper is a JPEG file named wp_<timestamp>.jpg. The "active"
 * wallpaper is the one currently used for the lock-screen preview and
 * background — it's copied to the legacy [user_wallpaper.jpg] location
 * so the existing rendering pipeline doesn't need to change.
 */
object WallpaperManager {

    private const val WALLPAPER_DIR = "wallpapers"
    private const val ACTIVE_WALLPAPER = "user_wallpaper.jpg"

    data class Wallpaper(
        val id: String,
        val file: File,
        val sizeBytes: Long
    )

    // ─────────────────────────────────────────────────────────────────────
    // Cycling modes
    // ─────────────────────────────────────────────────────────────────────

    const val CYCLE_ON_VERSE_CHANGE = "on_verse_change"
    const val CYCLE_CUSTOM_INTERVAL = "custom_interval"
    const val CYCLE_ON_SCREEN_OFF = "on_screen_off"
    const val CYCLE_DAY_NIGHT = "day_night"

    // ─────────────────────────────────────────────────────────────────────
    // Directory helpers
    // ─────────────────────────────────────────────────────────────────────

    private fun wallpaperDir(context: Context): File =
        File(context.filesDir, WALLPAPER_DIR).also { it.mkdirs() }

    fun activeWallpaperFile(context: Context): File =
        File(context.filesDir, ACTIVE_WALLPAPER)

    // ─────────────────────────────────────────────────────────────────────
    // Listing & active selection
    // ─────────────────────────────────────────────────────────────────────

    fun listWallpapers(context: Context): List<Wallpaper> {
        val dir = wallpaperDir(context)
        return dir.listFiles { f -> f.name.startsWith("wp_") && f.name.endsWith(".jpg") }
            ?.sortedByDescending { it.lastModified() }
            ?.map { Wallpaper(
                id = it.nameWithoutExtension,
                file = it,
                sizeBytes = it.length()
            ) }
            ?: emptyList()
    }

    fun getActiveWallpaperId(context: Context, prefs: android.content.SharedPreferences): String? {
        return prefs.getString("active_wallpaper_id", null)
    }

    fun setActiveWallpaper(context: Context, wallpaperId: String): Boolean {
        val source = File(wallpaperDir(context), "$wallpaperId.jpg")
        if (!source.exists()) return false
        val target = activeWallpaperFile(context)
        source.copyTo(target, overwrite = true)
        return true
    }

    // ─────────────────────────────────────────────────────────────────────
    // Add / delete
    // ─────────────────────────────────────────────────────────────────────

    fun addWallpaper(context: Context, sourceUri: Uri): String? {
        return try {
            val id = "wp_${System.currentTimeMillis()}"
            val target = File(wallpaperDir(context), "$id.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            id
        } catch (e: Exception) {
            null
        }
    }

    fun deleteWallpaper(context: Context, wallpaperId: String) {
        val file = File(wallpaperDir(context), "$wallpaperId.jpg")
        if (file.exists()) file.delete()
    }

    /**
     * Ensures the legacy [user_wallpaper.jpg] exists. If it doesn't and we
     * have at least one managed wallpaper, activates the first one.
     */
    fun ensureActiveWallpaper(context: Context, prefs: android.content.SharedPreferences) {
        val active = activeWallpaperFile(context)
        if (active.exists()) return
        val wallpapers = listWallpapers(context)
        if (wallpapers.isNotEmpty()) {
            val first = wallpapers.first()
            setActiveWallpaper(context, first.id)
            prefs.edit().putString("active_wallpaper_id", first.id).apply()
        }
    }

    fun activeWallpaperUri(context: Context): Uri? {
        val file = activeWallpaperFile(context)
        if (!file.exists()) return null
        return Uri.fromFile(file).buildUpon()
            .appendQueryParameter("v", System.currentTimeMillis().toString())
            .build()
    }

    // ─────────────────────────────────────────────────────────────────────
    // Cycling logic — called by DailyVerseWorker / ScreenOffReceiver
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Cycles to the next wallpaper in the gallery (wraps around).
     * Copies it to user_wallpaper.jpg and updates the active_wallpaper_id pref.
     * Returns true if the wallpaper was changed, false if there are < 2 wallpapers.
     */
    fun cycleToNext(context: Context, prefs: android.content.SharedPreferences): Boolean {
        val wallpapers = listWallpapers(context)
        if (wallpapers.size < 2) return false

        val currentId = prefs.getString("active_wallpaper_id", null)
        val currentIndex = wallpapers.indexOfFirst { it.id == currentId }
        val nextIndex = (currentIndex + 1).coerceAtLeast(0) % wallpapers.size
        val next = wallpapers[nextIndex]

        if (setActiveWallpaper(context, next.id)) {
            prefs.edit().putString("active_wallpaper_id", next.id).apply()
            return true
        }
        return false
    }

    /**
     * Handles day/night wallpaper switching. If it's currently night time
     * (according to WallpaperSettings), switches to the night wallpaper.
     * Otherwise switches to the day (active) wallpaper.
     *
     * Returns true if the wallpaper was changed.
     */
    fun applyDayNightIfNeeded(
        context: Context,
        prefs: android.content.SharedPreferences,
        settings: WallpaperSettings
    ): Boolean {
        if (!settings.cycleEnabled || settings.cycleMode != CYCLE_DAY_NIGHT) return false

        val isNight = settings.isCurrentlyNight()
        val nightId = prefs.getString("night_wallpaper_id", null)
        val currentId = prefs.getString("active_wallpaper_id", null)

        if (isNight && nightId != null && currentId != nightId) {
            // Switch to night wallpaper
            if (setActiveWallpaper(context, nightId)) {
                prefs.edit().putString("active_wallpaper_id", nightId).apply()
                return true
            }
        } else if (!isNight) {
            // Switch back to day wallpaper (the one stored as "day_wallpaper_id")
            val dayId = prefs.getString("day_wallpaper_id", null)
            if (dayId != null && currentId != dayId) {
                if (setActiveWallpaper(context, dayId)) {
                    prefs.edit().putString("active_wallpaper_id", dayId).apply()
                    return true
                }
            }
        }
        return false
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Wallpaper cycling settings — stored in SharedPreferences
// ─────────────────────────────────────────────────────────────────────────────

data class WallpaperSettings(
    val cycleEnabled: Boolean = false,
    /** Cycling mode: ON_VERSE_CHANGE, CUSTOM_INTERVAL, ON_SCREEN_OFF, DAY_NIGHT */
    val cycleMode: String = WallpaperManager.CYCLE_ON_VERSE_CHANGE,
    /** Hours between wallpaper swaps for CUSTOM_INTERVAL: 1, 2, 3, 6, 12, 24. */
    val cycleIntervalHours: Int = 24,
    /** Hour of day for 24h mode (0-23). */
    val cycleDailyHour: Int = 6,
    /** Hour when night wallpaper activates (0-23) — for DAY_NIGHT mode. */
    val nightStartHour: Int = 20,
    /** Hour when night wallpaper deactivates (0-23) — for DAY_NIGHT mode. */
    val nightEndHour: Int = 6
) {
    companion object {
        private const val PREFIX = "wp_cycle_"

        fun load(prefs: android.content.SharedPreferences): WallpaperSettings {
            return WallpaperSettings(
                cycleEnabled = prefs.getBoolean(PREFIX + "enabled", false),
                cycleMode = prefs.getString(PREFIX + "mode", WallpaperManager.CYCLE_ON_VERSE_CHANGE)
                    ?: WallpaperManager.CYCLE_ON_VERSE_CHANGE,
                cycleIntervalHours = prefs.getInt(PREFIX + "interval", 24),
                cycleDailyHour = prefs.getInt(PREFIX + "daily_hour", 6),
                nightStartHour = prefs.getInt(PREFIX + "night_start", 20),
                nightEndHour = prefs.getInt(PREFIX + "night_end", 6)
            )
        }

        fun save(prefs: android.content.SharedPreferences.Editor, settings: WallpaperSettings) {
            prefs.putBoolean(PREFIX + "enabled", settings.cycleEnabled)
            prefs.putString(PREFIX + "mode", settings.cycleMode)
            prefs.putInt(PREFIX + "interval", settings.cycleIntervalHours)
            prefs.putInt(PREFIX + "daily_hour", settings.cycleDailyHour)
            prefs.putInt(PREFIX + "night_start", settings.nightStartHour)
            prefs.putInt(PREFIX + "night_end", settings.nightEndHour)
            prefs.apply()
        }
    }

    fun isCurrentlyNight(): Boolean {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return if (nightStartHour <= nightEndHour) {
            hour in nightStartHour until nightEndHour
        } else {
            hour >= nightStartHour || hour < nightEndHour
        }
    }
}
