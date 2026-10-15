package com.mbash.earnlife.data

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context

object AppUsageReader {

    fun getRecentUsage(context: Context): List<UsageStats> {
        val usageStatsManager = context.getSystemService(
            Context.USAGE_STATS_SERVICE
        ) as UsageStatsManager

        val endTime = System.currentTimeMillis()
        val startTime = endTime - 24 * 60 * 60 * 1000L

        return usageStatsManager
            .queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )
            .orEmpty()
            .filter { it.totalTimeInForeground > 0 }
            .sortedByDescending { it.totalTimeInForeground }
    }
}
