package com.daklok.biblelockscreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        AppLogger.i(context, "Boot", "onReceive(BOOT_COMPLETED) — rescheduling wallpaper alarms")

        val prefs = context.getSharedPreferences("bible_app_prefs", Context.MODE_PRIVATE)
        val isDailyActive = prefs.getBoolean("auto_wallpaper_active", false)
        val changeOnScreenOff = prefs.getBoolean("change_on_screen_off", false)

        if (isDailyActive && !changeOnScreenOff) {
            val intervalHours = prefs.getInt("auto_interval_hours", 24)
            val dailyHour = prefs.getInt("daily_hour", 6)
            scheduleAutoWallpaper(context, intervalHours, dailyHour)
        }

        scheduleWallpaperCycling(context)
    }
}