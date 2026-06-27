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
 *
 * Night-mode wallpapers (optional) are stored alongside with a `_night`
 * suffix and activated by [WallpaperSettings.isNightMode] when the device
 * time falls inside [WallpaperSettings.nightStartHour, nightEndHour).
 */
object WallpaperManager {

    private const val WALLPAPER_DIR = "wallpapers"
    private const val ACTIVE_WALLPAPER = "user_wallpaper.jpg"

    data class Wallpaper(
        val id: String,        // filename without extension, e.g. "wp_1718901234567"
        val file: File,
        val sizeBytes: Long
    )

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

    /**
     * Copies [sourceUri] into the wallpapers directory and returns the new
     * wallpaper id, or null on failure.
     */
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
     * have at least one managed wallpaper, activates the first one. This
     * bridges the old single-wallpaper system with the new multi-wallpaper
     * gallery.
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

    /**
     * Returns the URI of the currently active wallpaper, suitable for
     * passing to the preview / wallpaper rendering pipeline.
     * Appends a cache-buster query param so Compose recomposes when the
     * active wallpaper changes.
     */
    fun activeWallpaperUri(context: Context): Uri? {
        val file = activeWallpaperFile(context)
        if (!file.exists()) return null
        return Uri.fromFile(file).buildUpon()
            .appendQueryParameter("v", System.currentTimeMillis().toString())
            .build()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Wallpaper cycling settings — stored in SharedPreferences
// ─────────────────────────────────────────────────────────────────────────────

data class WallpaperSettings(
    /** Auto-cycle through all wallpapers (not just verse text). */
    val cycleEnabled: Boolean = false,
    /** Hours between wallpaper swaps: 1, 2, 3, 6, 12, 24. */
    val cycleIntervalHours: Int = 24,
    /** Hour of day for 24h mode (0-23). */
    val cycleDailyHour: Int = 6,
    /** Change wallpaper on every screen-off / lock. */
    val cycleOnScreenOff: Boolean = false,
    /** Night-mode: use a different wallpaper at night. */
    val nightModeEnabled: Boolean = false,
    /** Hour when night wallpaper activates (0-23). */
    val nightStartHour: Int = 20,
    /** Hour when night wallpaper deactivates (0-23). */
    val nightEndHour: Int = 6
) {
    companion object {
        private const val PREFIX = "wp_cycle_"

        fun load(prefs: android.content.SharedPreferences): WallpaperSettings {
            return WallpaperSettings(
                cycleEnabled = prefs.getBoolean(PREFIX + "enabled", false),
                cycleIntervalHours = prefs.getInt(PREFIX + "interval", 24),
                cycleDailyHour = prefs.getInt(PREFIX + "daily_hour", 6),
                cycleOnScreenOff = prefs.getBoolean(PREFIX + "on_screen_off", false),
                nightModeEnabled = prefs.getBoolean(PREFIX + "night_enabled", false),
                nightStartHour = prefs.getInt(PREFIX + "night_start", 20),
                nightEndHour = prefs.getInt(PREFIX + "night_end", 6)
            )
        }

        fun save(prefs: android.content.SharedPreferences.Editor, settings: WallpaperSettings) {
            prefs.apply {
                putBoolean(PREFIX + "enabled", settings.cycleEnabled)
                putInt(PREFIX + "interval", settings.cycleIntervalHours)
                putInt(PREFIX + "daily_hour", settings.cycleDailyHour)
                putBoolean(PREFIX + "on_screen_off", settings.cycleOnScreenOff)
                putBoolean(PREFIX + "night_enabled", settings.nightModeEnabled)
                putInt(PREFIX + "night_start", settings.nightStartHour)
                putInt(PREFIX + "night_end", settings.nightEndHour)
            }
        }
    }

    /** Returns true if the current device time is inside the night window. */
    fun isCurrentlyNight(): Boolean {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return if (nightStartHour <= nightEndHour) {
            hour in nightStartHour until nightEndHour
        } else {
            // Wraps past midnight, e.g. 22 → 6
            hour >= nightStartHour || hour < nightEndHour
        }
    }
}
