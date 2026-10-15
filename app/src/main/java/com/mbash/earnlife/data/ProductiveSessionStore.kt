package com.mbash.earnlife.data

import android.content.Context

object ProductiveSessionStore {

    private const val PREFS_NAME = "earnlife_productive_sessions"

    private const val ENTERTAINMENT_PACKAGE_KEY = "entertainment_package"
    private const val ENTERTAINMENT_CHECKPOINT_KEY = "entertainment_checkpoint"

    private const val ACTIVE_PACKAGE_KEY = "active_package"
    private const val SESSION_START_KEY = "session_start"
    private const val TOTAL_SECONDS_KEY = "total_seconds"
    private const val BALANCE_SECONDS_KEY = "balance_seconds"
    private const val REMAINDER_SECONDS_KEY = "remainder_seconds"

    // 5 minutes of productive time earns 1 minute of entertainment.
    private const val PRODUCTIVE_SECONDS_PER_CREDIT = 5 * 60L
    private const val CREDIT_SECONDS_PER_REWARD = 60L

    private fun preferences(context: Context) =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    fun startSession(context: Context, packageName: String) {
        preferences(context).edit()
            .putString(ACTIVE_PACKAGE_KEY, packageName)
            .putLong(SESSION_START_KEY, System.currentTimeMillis())
            .apply()
    }

    fun getActivePackage(context: Context): String? =
        preferences(context).getString(ACTIVE_PACKAGE_KEY, null)

    fun getSessionStart(context: Context): Long =
        preferences(context).getLong(SESSION_START_KEY, 0L)

    /**
     * Adds verified productive foreground seconds.
     * Returns the number of entertainment seconds newly earned.
     */
    @Synchronized
    fun addProductiveSeconds(
        context: Context,
        seconds: Long
    ): Long {
        if (seconds <= 0) return 0L

        val prefs = preferences(context)

        val totalProductiveSeconds =
            prefs.getLong(TOTAL_SECONDS_KEY, 0L)

        val oldRemainder =
            prefs.getLong(REMAINDER_SECONDS_KEY, 0L)

        val availableBalance =
            prefs.getLong(BALANCE_SECONDS_KEY, 0L)

        val combinedSeconds = oldRemainder + seconds

        val completedRewards =
            combinedSeconds / PRODUCTIVE_SECONDS_PER_CREDIT

        val newRemainder =
            combinedSeconds % PRODUCTIVE_SECONDS_PER_CREDIT

        val newlyEarnedSeconds =
            completedRewards * CREDIT_SECONDS_PER_REWARD

        prefs.edit()
            .putLong(
                TOTAL_SECONDS_KEY,
                totalProductiveSeconds + seconds
            )
            .putLong(
                REMAINDER_SECONDS_KEY,
                newRemainder
            )
            .putLong(
                BALANCE_SECONDS_KEY,
                availableBalance + newlyEarnedSeconds
            )
            .apply()

        return newlyEarnedSeconds
    }


    @Synchronized
    fun spendEntertainmentSeconds(context: Context, seconds: Long): Long {
        val prefs = preferences(context)
        val currentBalance = prefs.getLong(BALANCE_SECONDS_KEY, 0L)

        if (seconds <= 0L || currentBalance <= 0L) {
            return currentBalance
        }

        val amountToSpend = minOf(seconds, currentBalance)
        val newBalance = currentBalance - amountToSpend

        prefs.edit()
            .putLong(BALANCE_SECONDS_KEY, newBalance)
            .apply()

        return newBalance
    }

    fun getTotalProductiveSeconds(context: Context): Long =
        preferences(context).getLong(TOTAL_SECONDS_KEY, 0L)

    fun getEntertainmentBalanceSeconds(context: Context): Long =
        preferences(context).getLong(BALANCE_SECONDS_KEY, 0L)

    fun getRemainderSeconds(context: Context): Long =
        preferences(context).getLong(REMAINDER_SECONDS_KEY, 0L)

    fun clearActiveSession(context: Context) {
        preferences(context).edit()
            .remove(ACTIVE_PACKAGE_KEY)
            .remove(SESSION_START_KEY)
            .apply()
    }

    fun startEntertainmentSession(
        context: Context,
        packageName: String
    ) {
        preferences(context).edit()
            .putString(ENTERTAINMENT_PACKAGE_KEY, packageName)
            .putLong(
                ENTERTAINMENT_CHECKPOINT_KEY,
                System.currentTimeMillis()
            )
            .apply()
    }

    fun getActiveEntertainmentPackage(context: Context): String? =
        preferences(context).getString(
            ENTERTAINMENT_PACKAGE_KEY,
            null
        )

    fun getEntertainmentCheckpoint(context: Context): Long =
        preferences(context).getLong(
            ENTERTAINMENT_CHECKPOINT_KEY,
            0L
        )

    fun updateEntertainmentCheckpoint(
        context: Context,
        timestamp: Long
    ) {
        preferences(context).edit()
            .putLong(ENTERTAINMENT_CHECKPOINT_KEY, timestamp)
            .apply()
    }

    fun clearEntertainmentSession(context: Context) {
        preferences(context).edit()
            .remove(ENTERTAINMENT_PACKAGE_KEY)
            .remove(ENTERTAINMENT_CHECKPOINT_KEY)
            .apply()
    }
}
