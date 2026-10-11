package com.walltodo

import android.app.Application
import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class DailyWallpaperWorker(ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
    override suspend fun doWork(): Result =
        if (WallpaperUpdater.update(applicationContext)) Result.success() else Result.retry()
}

class WallTodoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Periodic work survives reboots and app restarts.
        val req = PeriodicWorkRequestBuilder<DailyWallpaperWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork("daily_wallpaper", ExistingPeriodicWorkPolicy.KEEP, req)
    }
}
