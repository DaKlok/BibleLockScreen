package com.daklok.biblelockscreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager

class ScreenOffReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_SCREEN_OFF) return

        val prefs = context.getSharedPreferences("bible_app_prefs", Context.MODE_PRIVATE)

        // Only run if screen-off mode AND the main auto-wallpaper toggle is enabled
        val changeOnScreenOff = prefs.getBoolean("change_on_screen_off", false)
        val autoActive = prefs.getBoolean("auto_wallpaper_active", false)
        if (!changeOnScreenOff || !autoActive) return

        // Advance the screen-off verse counter so a fresh verse is picked each lock
        val currentIndex = prefs.getInt("screen_off_verse_index", 0)
        prefs.edit().putInt("screen_off_verse_index", currentIndex + 1).apply()

        // Screen is already off — enqueue wallpaper update so it renders silently
        // with no visible refresh or lag when the user next turns the screen on
        val req = OneTimeWorkRequestBuilder<DailyVerseWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context).enqueue(req)
    }
}