
package com.mbash.earnlife.detection

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.mbash.earnlife.MainActivity
import com.mbash.earnlife.data.AppSelectionStore
import com.mbash.earnlife.data.ForegroundUsageReconciler
import com.mbash.earnlife.data.ProductiveSessionStore

class AppDetectionService : AccessibilityService() {

    private var lastPackageName: String? = null
    private var lastRedirectTime = 0L

    private val handler = Handler(Looper.getMainLooper())
    private var activeRestrictedPackage: String? = null
    private var lastEntertainmentSpendTime = 0L
    private var entertainmentRunnable: Runnable? = null

    override fun onServiceConnected() {
        super.onServiceConnected()

        Log.d("EarnLifeCredits", "Detection service connected")

        // We'll add safe session recovery here next.
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName == lastPackageName) return

        lastPackageName = packageName

        // Stop charging when we leave a restricted app.
        if (activeRestrictedPackage != null &&
            packageName != activeRestrictedPackage
        ) {
            stopEntertainmentTracking()
        }

        // Reconcile the productive session when leaving its app.
        val activeProductivePackage =
            ProductiveSessionStore.getActivePackage(this)

        if (activeProductivePackage != null &&
            packageName != activeProductivePackage
        ) {
            val sessionStart =
                ProductiveSessionStore.getSessionStart(this)

            val productiveSeconds =
                ForegroundUsageReconciler.getForegroundSeconds(
                    context = this,
                    packageName = activeProductivePackage,
                    startTime = sessionStart,
                    endTime = System.currentTimeMillis()
                )

            if (productiveSeconds > 0L) {
                val earnedSeconds =
                    ProductiveSessionStore.addProductiveSeconds(
                        context = this,
                        seconds = productiveSeconds
                    )

                ProductiveSessionStore.clearActiveSession(this)

                Log.d(
                    "EarnLifeCredits",
                    "Productive time: ${productiveSeconds}s, " +
                            "entertainment earned: ${earnedSeconds}s"
                )
            } else {
                Log.d(
                    "EarnLifeCredits",
                    "No completed foreground interval yet for " +
                            activeProductivePackage
                )
            }
        }

        if (packageName == applicationContext.packageName) return

        val restrictedApp = AppSelectionStore.load(
            this,
            productive = false
        ).find { it.packageName == packageName } ?: return

        val balance =
            ProductiveSessionStore.getEntertainmentBalanceSeconds(this)

        if (balance <= 0L) {
            redirectToEarnLife(restrictedApp.label)
            return
        }

        // There is credit available, so allow the app and charge its usage.
        startEntertainmentTracking(packageName)

        Log.d(
            "EarnLifeCredits",
            "Allowing ${restrictedApp.label}; balance: ${balance}s"
        )
    }

    private fun startEntertainmentTracking(packageName: String) {
        if (activeRestrictedPackage == packageName) return

        stopEntertainmentTracking()

        activeRestrictedPackage = packageName
        lastEntertainmentSpendTime = System.currentTimeMillis()

        ProductiveSessionStore.startEntertainmentSession(
            context = this,
            packageName = packageName
        )

        val ticker = object : Runnable {
            override fun run() {
                if (activeRestrictedPackage != packageName) return

                val now = System.currentTimeMillis()
                val elapsedSeconds =
                    (now - lastEntertainmentSpendTime) / 1000L

                if (elapsedSeconds > 0L) {
                    val remaining =
                        ProductiveSessionStore.spendEntertainmentSeconds(
                            context = this@AppDetectionService,
                            seconds = elapsedSeconds
                        )

                    lastEntertainmentSpendTime += elapsedSeconds * 1000L

                    Log.d(
                        "EarnLifeCredits",
                        "Entertainment spent: ${elapsedSeconds}s; " +
                                "balance: ${remaining}s"
                    )

                    if (remaining <= 0L) {
                        activeRestrictedPackage = null
                        entertainmentRunnable = null
                        handler.removeCallbacks(this)

                        val appLabel = AppSelectionStore.load(
                            this@AppDetectionService,
                            productive = false
                        ).find { it.packageName == packageName }?.label
                            ?: packageName

                        redirectToEarnLife(appLabel)
                        return
                    }
                }

                handler.postDelayed(this, 1000L)
            }
        }

        entertainmentRunnable = ticker
        handler.postDelayed(ticker, 1000L)
    }

    private fun stopEntertainmentTracking() {
        val ticker = entertainmentRunnable
        if (ticker != null) {
            handler.removeCallbacks(ticker)
        }

        val packageName = activeRestrictedPackage
        if (packageName != null) {
            val now = System.currentTimeMillis()
            val elapsedSeconds =
                (now - lastEntertainmentSpendTime) / 1000L

            if (elapsedSeconds > 0L) {
                val remaining =
                    ProductiveSessionStore.spendEntertainmentSeconds(
                        context = this,
                        seconds = elapsedSeconds
                    )

                Log.d(
                    "EarnLifeCredits",
                    "Final entertainment charge: ${elapsedSeconds}s; " +
                            "balance: ${remaining}s"
                )
            }
        }

        ProductiveSessionStore.clearEntertainmentSession(this)

        activeRestrictedPackage = null
        entertainmentRunnable = null
        lastEntertainmentSpendTime = 0L
    }

    private fun redirectToEarnLife(appLabel: String) {
        val now = System.currentTimeMillis()

        if (now - lastRedirectTime < 2000L) return
        lastRedirectTime = now

        Log.d(
            "EarnLifeCredits",
            "Entertainment allowance exhausted; redirecting from $appLabel"
        )

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_RESTRICTED_APP, appLabel)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }

        startActivity(intent)
    }

    override fun onInterrupt() {
        stopEntertainmentTracking()
        Log.d("EarnLifeDetection", "Detection service interrupted")
    }
}
