package com.daklok.biblelockscreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf

class WallpaperAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val uniqueWorkName = intent.getStringExtra(EXTRA_UNIQUE_WORK_NAME) ?: "DailyBibleWallpaper"
        val source = intent.getStringExtra(EXTRA_SOURCE) ?: "verse"

        AppLogger.i(context, "Alarm", "onReceive: exact alarm fired for '$uniqueWorkName'")

        val data = workDataOf("source" to source)
        val request = OneTimeWorkRequestBuilder<DailyVerseWorker>()
            .setInputData(data)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.REPLACE, request)
    }

    companion object {
        const val EXTRA_UNIQUE_WORK_NAME = "unique_work_name"
        const val EXTRA_SOURCE = "source"
    }
}