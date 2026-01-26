package com.daklok.biblelockscreen

import android.app.WallpaperManager
import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class DailyVerseWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        // 1. Získame uloženú fotku
        val prefs = applicationContext.getSharedPreferences("bible_app_prefs", Context.MODE_PRIVATE)
        val uriString = prefs.getString("bg_uri", null) ?: return Result.failure()

        // 2. Stiahneme YouVersion verš
        val verseData = YouVersionFetcher.getVerseOfTheDay() ?: return Result.retry()

        // 3. Vytvoríme tapetu
        val finalBitmap = WallpaperUtils.createBitmapWithText(
            applicationContext,
            Uri.parse(uriString),
            verseData.first,
            verseData.second
        )

        // 4. Nastavíme tapetu na Zamknutú obrazovku
        if (finalBitmap != null) {
            val wallpaperManager = WallpaperManager.getInstance(applicationContext)
            try {
                wallpaperManager.setBitmap(
                    finalBitmap,
                    null,
                    true,
                    WallpaperManager.FLAG_LOCK // Iba Lock Screen
                )
                return Result.success()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return Result.failure()
    }
}