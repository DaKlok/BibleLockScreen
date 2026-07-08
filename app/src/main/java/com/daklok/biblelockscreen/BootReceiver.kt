package com.daklok.biblelockscreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        AppLogger.i(context, "Boot", "onReceive(BOOT_COMPLETED) — rescheduling wallpaper alarms")
        ensureWallpaperAlarmsScheduled(context)
    }
}