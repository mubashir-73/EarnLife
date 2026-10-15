package com.mbash.earnlife.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build

object ForegroundUsageReconciler {

    fun getForegroundSeconds(
        context: Context,
        packageName: String,
        startTime: Long,
        endTime: Long
    ): Long {
        if (endTime <= startTime) return 0L

        val manager = context.getSystemService(
            Context.USAGE_STATS_SERVICE
        ) as UsageStatsManager

        val events = manager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()

        var foregroundStart: Long? = null
        var totalForegroundMillis = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)

            val eventPackage = event.packageName?.toString()
            val eventTime = event.timeStamp

            val isResumed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
            } else {
                @Suppress("DEPRECATION")
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            }

            val isPaused = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                event.eventType == UsageEvents.Event.ACTIVITY_PAUSED
            } else {
                @Suppress("DEPRECATION")
                event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND
            }

            if (eventPackage != packageName) continue

            when {
                isResumed && foregroundStart == null -> {
                    foregroundStart = eventTime
                }

                isPaused && foregroundStart != null -> {
                    totalForegroundMillis +=
                        (eventTime - foregroundStart).coerceAtLeast(0L)

                    foregroundStart = null
                }
            }
        }

        // Only count a completed foreground interval.
        // An unfinished interval is not credited until we can reconcile it.
        return totalForegroundMillis / 1000L
    }
}
